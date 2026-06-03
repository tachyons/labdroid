package `in`.aboobacker.labdroid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.aboobacker.labdroid.data.model.Commit
import `in`.aboobacker.labdroid.data.model.Discussion
import `in`.aboobacker.labdroid.data.model.Label
import `in`.aboobacker.labdroid.data.model.MarkdownUpload
import `in`.aboobacker.labdroid.data.model.MergeRequest
import `in`.aboobacker.labdroid.data.model.MergeRequestChanges
import `in`.aboobacker.labdroid.data.model.Milestone
import `in`.aboobacker.labdroid.data.model.Note
import `in`.aboobacker.labdroid.data.model.Pipeline
import `in`.aboobacker.labdroid.data.model.Project
import `in`.aboobacker.labdroid.data.model.User
import `in`.aboobacker.labdroid.data.remote.GitLabApi
import `in`.aboobacker.labdroid.data.repository.ProjectRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MergeRequestViewModel @Inject constructor(
    private val api: GitLabApi,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MergeRequestUiState>(MergeRequestUiState.Loading)
    val uiState: StateFlow<MergeRequestUiState> = _uiState.asStateFlow()

    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage.asStateFlow()

    private val _commentText = MutableStateFlow("")
    val commentText: StateFlow<String> = _commentText.asStateFlow()

    fun onCommentTextChange(text: String) {
        _commentText.value = text
    }

    private var currentDiscussionPage = 1
    private var isLastDiscussionPage = false

    fun fetchMergeRequest(projectId: Long, iid: Long) {
        viewModelScope.launch {
            _uiState.value = MergeRequestUiState.Loading
            currentDiscussionPage = 1
            isLastDiscussionPage = false
            try {
                coroutineScope {
                    val mrDeferred = async { api.getMergeRequest(projectId, iid) }
                    val projectDeferred = async { api.getProject(projectId) }
                    val currentUserDeferred = async {
                        try {
                            api.getCurrentUser()
                        } catch (e: Exception) {
                            null
                        }
                    }
                    val labelsDeferred = async { api.getProjectLabels(projectId) }
                    val milestonesDeferred = async { api.getProjectMilestones(projectId) }
                    val membersDeferred = async { api.getProjectMembers(projectId) }

                    val mr = mrDeferred.await()
                    val project = projectDeferred.await()
                    val currentUser = currentUserDeferred.await()
                    val labels = try {
                        labelsDeferred.await()
                    } catch (e: Exception) {
                        emptyList()
                    }
                    val milestones = try {
                        milestonesDeferred.await().sortedByDescending { it.createdAt }
                    } catch (e: Exception) {
                        emptyList()
                    }
                    val members = try {
                        membersDeferred.await()
                    } catch (e: Exception) {
                        emptyList()
                    }

                    val enrichedMr = mr.copy(
                        labels = mr.labels.map { label ->
                            labels.find { it.name == label.name } ?: label
                        }
                    )

                    _uiState.value = MergeRequestUiState.Success(
                        mergeRequest = enrichedMr,
                        project = project,
                        currentUser = currentUser,
                        discussions = emptyList(),
                        changes = null,
                        commits = emptyList(),
                        pipelines = emptyList(),
                        isLoadingDiscussions = true,
                        isLoadingChanges = true,
                        isLoadingCommits = true,
                        isLoadingPipelines = true,
                        availableLabels = labels,
                        availableMilestones = milestones,
                        availableMembers = members
                    )
                }

                // Fetch other data asynchronously
                launch {
                    try {
                        val changes = api.getMergeRequestChanges(projectId, iid)
                        updateSuccessState { it.copy(changes = changes, isLoadingChanges = false) }
                    } catch (e: Exception) {
                        updateSuccessState { it.copy(isLoadingChanges = false) }
                    }
                }

                launch {
                    try {
                        val commits = api.getMergeRequestCommits(projectId, iid)
                        updateSuccessState { it.copy(commits = commits, isLoadingCommits = false) }
                    } catch (e: Exception) {
                        updateSuccessState { it.copy(isLoadingCommits = false) }
                    }
                }

                launch {
                    try {
                        val pipelines = api.getMergeRequestPipelines(projectId, iid)
                        updateSuccessState {
                            it.copy(
                                pipelines = pipelines,
                                isLoadingPipelines = false
                            )
                        }
                    } catch (e: Exception) {
                        updateSuccessState { it.copy(isLoadingPipelines = false) }
                    }
                }

                launch {
                    try {
                        val discussions = api.getMergeRequestDiscussions(
                            projectId,
                            iid,
                            page = currentDiscussionPage
                        )
                        isLastDiscussionPage = discussions.size < 20
                        updateSuccessState {
                            it.copy(
                                discussions = discussions,
                                isLoadingDiscussions = false,
                                hasMoreDiscussions = !isLastDiscussionPage
                            )
                        }
                    } catch (e: Exception) {
                        updateSuccessState { it.copy(isLoadingDiscussions = false) }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = MergeRequestUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun updateSuccessState(update: (MergeRequestUiState.Success) -> MergeRequestUiState.Success) {
        val currentState = _uiState.value
        if (currentState is MergeRequestUiState.Success) {
            _uiState.value = update(currentState)
        }
    }

    fun loadMoreDiscussions(projectId: Long, iid: Long) {
        val currentState = _uiState.value
        if (currentState !is MergeRequestUiState.Success || currentState.isLoadingDiscussions || !currentState.hasMoreDiscussions) return

        viewModelScope.launch {
            try {
                _uiState.value = currentState.copy(isLoadingDiscussions = true)
                currentDiscussionPage++
                val moreDiscussions =
                    api.getMergeRequestDiscussions(projectId, iid, page = currentDiscussionPage)
                isLastDiscussionPage = moreDiscussions.size < 20

                updateSuccessState {
                    it.copy(
                        discussions = it.discussions + moreDiscussions,
                        isLoadingDiscussions = false,
                        hasMoreDiscussions = !isLastDiscussionPage
                    )
                }
            } catch (e: Exception) {
                updateSuccessState { it.copy(isLoadingDiscussions = false) }
            }
        }
    }

    fun postComment(projectId: Long, iid: Long, body: String, discussionId: String? = null) {
        viewModelScope.launch {
            try {
                if (discussionId != null) {
                    api.postMergeRequestDiscussionNote(projectId, iid, discussionId, body)
                } else {
                    api.postMergeRequestNote(projectId, iid, body)
                }
                _commentText.value = ""
                fetchMergeRequest(projectId, iid)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun uploadImage(projectId: Long, file: File) {
        viewModelScope.launch {
            _isUploadingImage.value = true
            try {
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
                val upload = projectRepository.uploadFile(projectId, body)
                
                val currentText = _commentText.value
                val newText = if (currentText.isEmpty()) {
                    upload.markdown
                } else {
                    "$currentText\n${upload.markdown}"
                }
                _commentText.value = newText
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isUploadingImage.value = false
            }
        }
    }

    fun closeMergeRequest(projectId: Long, iid: Long) {
        viewModelScope.launch {
            try {
                api.updateMergeRequest(projectId, iid, stateEvent = "close")
                fetchMergeRequest(projectId, iid)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun mergeMergeRequest(projectId: Long, iid: Long) {
        viewModelScope.launch {
            try {
                api.mergeMergeRequest(projectId, iid)
                fetchMergeRequest(projectId, iid)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun updateMergeRequest(
        projectId: Long,
        iid: Long,
        title: String,
        description: String,
        milestoneId: Long? = null,
        assigneeId: Long? = null,
        labels: String? = null
    ) {
        viewModelScope.launch {
            try {
                api.updateMergeRequest(
                    projectId,
                    iid,
                    title = title,
                    description = description,
                    milestoneId = milestoneId,
                    assigneeId = assigneeId,
                    labels = labels
                )
                fetchMergeRequest(projectId, iid)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun fetchMetadata(projectId: Long) {
        viewModelScope.launch {
            try {
                coroutineScope {
                    val labelsDeferred = async { api.getProjectLabels(projectId) }
                    val milestonesDeferred = async { api.getProjectMilestones(projectId) }
                    val membersDeferred = async { api.getProjectMembers(projectId) }

                    val labels = labelsDeferred.await()
                    val milestones = milestonesDeferred.await().sortedByDescending { it.createdAt }
                    val members = membersDeferred.await()

                    val currentState = _uiState.value
                    if (currentState is MergeRequestUiState.Success) {
                        _uiState.value = currentState.copy(
                            availableLabels = labels,
                            availableMilestones = milestones,
                            availableMembers = members
                        )
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}

sealed interface MergeRequestUiState {
    data object Loading : MergeRequestUiState
    data class Success(
        val mergeRequest: MergeRequest,
        val project: Project? = null,
        val currentUser: User? = null,
        val discussions: List<Discussion> = emptyList(),
        val changes: MergeRequestChanges? = null,
        val commits: List<Commit> = emptyList(),
        val pipelines: List<Pipeline> = emptyList(),
        val isLoadingDiscussions: Boolean = true,
        val isLoadingChanges: Boolean = true,
        val isLoadingCommits: Boolean = true,
        val isLoadingPipelines: Boolean = true,
        val hasMoreDiscussions: Boolean = false,
        val availableLabels: List<Label> = emptyList(),
        val availableMilestones: List<Milestone> = emptyList(),
        val availableMembers: List<User> = emptyList()
    ) : MergeRequestUiState

    data class Error(val message: String) : MergeRequestUiState
}
