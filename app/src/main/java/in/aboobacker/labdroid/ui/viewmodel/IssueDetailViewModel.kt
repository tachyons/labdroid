package `in`.aboobacker.labdroid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.aboobacker.labdroid.data.model.Discussion
import `in`.aboobacker.labdroid.data.model.Issue
import `in`.aboobacker.labdroid.data.model.IssueRequest
import `in`.aboobacker.labdroid.data.model.Label
import `in`.aboobacker.labdroid.data.model.MarkdownUpload
import `in`.aboobacker.labdroid.data.model.Milestone
import `in`.aboobacker.labdroid.data.model.Note
import `in`.aboobacker.labdroid.data.model.Project
import `in`.aboobacker.labdroid.data.model.User
import `in`.aboobacker.labdroid.data.remote.GitLabApi
import `in`.aboobacker.labdroid.data.repository.AuthRepository
import `in`.aboobacker.labdroid.data.repository.IssueRepository
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
class IssueDetailViewModel @Inject constructor(
    private val repository: IssueRepository,
    private val projectRepository: ProjectRepository,
    private val authRepository: AuthRepository,
    private val api: GitLabApi
) : ViewModel() {

    private val _uiState = MutableStateFlow<IssueDetailUiState>(IssueDetailUiState.Loading)
    val uiState: StateFlow<IssueDetailUiState> = _uiState.asStateFlow()

    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage.asStateFlow()

    private val _commentText = MutableStateFlow("")
    val commentText: StateFlow<String> = _commentText.asStateFlow()

    fun onCommentTextChange(text: String) {
        _commentText.value = text
    }

    private var currentDiscussionPage = 1
    private var isLastDiscussionPage = false

    fun loadIssueDetail(projectId: Long, issueIid: Long) {
        viewModelScope.launch {
            _uiState.value = IssueDetailUiState.Loading
            currentDiscussionPage = 1
            isLastDiscussionPage = false
            try {
                coroutineScope {
                    val issueDeferred = async { repository.getIssue(projectId, issueIid) }
                    val projectDeferred = async { projectRepository.getProject(projectId) }
                    val currentUserDeferred = async { authRepository.getUsername() }
                    val labelsDeferred = async { api.getProjectLabels(projectId) }
                    val milestonesDeferred = async { api.getProjectMilestones(projectId) }
                    val membersDeferred = async { api.getProjectMembers(projectId) }

                    val issue = issueDeferred.await()
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

                    val enrichedIssue = issue.copy(
                        labels = issue.labels.map { label ->
                            labels.find { it.name == label.name } ?: label
                        }
                    )

                    val canEdit =
                        (issue.author?.username != null && issue.author.username == currentUser) ||
                                (project.permissions?.projectAccess?.accessLevel ?: 0) >= 30 ||
                                (project.permissions?.groupAccess?.accessLevel ?: 0) >= 30

                    _uiState.value = IssueDetailUiState.Success(
                        issue = enrichedIssue,
                        project = project,
                        discussions = emptyList(),
                        canEdit = canEdit,
                        isLoadingDiscussions = true,
                        availableLabels = labels,
                        availableMilestones = milestones,
                        availableMembers = members
                    )

                    // Fetch discussions asynchronously
                    launch {
                        try {
                            val discussions = repository.getIssueDiscussions(
                                projectId,
                                issueIid,
                                page = currentDiscussionPage
                            )
                            isLastDiscussionPage = discussions.size < 20
                            val currentState = _uiState.value
                            if (currentState is IssueDetailUiState.Success) {
                                _uiState.value = currentState.copy(
                                    discussions = discussions,
                                    isLoadingDiscussions = false,
                                    hasMoreDiscussions = !isLastDiscussionPage
                                )
                            }
                        } catch (e: Exception) {
                            val currentState = _uiState.value
                            if (currentState is IssueDetailUiState.Success) {
                                _uiState.value = currentState.copy(isLoadingDiscussions = false)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = IssueDetailUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadMoreDiscussions(projectId: Long, issueIid: Long) {
        val currentState = _uiState.value
        if (currentState !is IssueDetailUiState.Success || currentState.isLoadingDiscussions || isLastDiscussionPage) return

        viewModelScope.launch {
            try {
                _uiState.value = currentState.copy(isLoadingDiscussions = true)
                currentDiscussionPage++
                val moreDiscussions = repository.getIssueDiscussions(
                    projectId,
                    issueIid,
                    page = currentDiscussionPage
                )
                isLastDiscussionPage = moreDiscussions.size < 20

                val updatedState = _uiState.value
                if (updatedState is IssueDetailUiState.Success) {
                    _uiState.value = updatedState.copy(
                        discussions = updatedState.discussions + moreDiscussions,
                        isLoadingDiscussions = false,
                        hasMoreDiscussions = !isLastDiscussionPage
                    )
                }
            } catch (e: Exception) {
                val updatedState = _uiState.value
                if (updatedState is IssueDetailUiState.Success) {
                    _uiState.value = updatedState.copy(isLoadingDiscussions = false)
                }
            }
        }
    }

    fun updateIssue(projectId: Long, issueIid: Long, request: IssueRequest) {
        viewModelScope.launch {
            try {
                repository.updateIssue(projectId, issueIid, request)
                loadIssueDetail(projectId, issueIid)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun closeIssue(projectId: Long, issueIid: Long) {
        updateIssue(projectId, issueIid, IssueRequest(stateEvent = "close"))
    }

    fun reopenIssue(projectId: Long, issueIid: Long) {
        updateIssue(projectId, issueIid, IssueRequest(stateEvent = "reopen"))
    }

    fun deleteIssue(projectId: Long, issueIid: Long, onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteIssue(projectId, issueIid)
                onDeleted()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun createIssue(projectId: Long, request: IssueRequest) {
        viewModelScope.launch {
            try {
                repository.createIssue(projectId, request)
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
                    if (currentState is IssueDetailUiState.Success) {
                        _uiState.value = currentState.copy(
                            availableLabels = labels,
                            availableMilestones = milestones,
                            availableMembers = members
                        )
                    } else if (currentState is IssueDetailUiState.Loading) {
                        // If still loading, we can't easily update but Success will handle it
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun postComment(projectId: Long, issueIid: Long, body: String, discussionId: String? = null) {
        viewModelScope.launch {
            try {
                repository.postIssueNote(projectId, issueIid, body, discussionId)
                _commentText.value = ""
                loadIssueDetail(projectId, issueIid)
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
}

sealed interface IssueDetailUiState {
    data object Loading : IssueDetailUiState
    data class Success(
        val issue: Issue,
        val project: Project? = null,
        val discussions: List<Discussion> = emptyList(),
        val canEdit: Boolean = false,
        val isLoadingDiscussions: Boolean = true,
        val hasMoreDiscussions: Boolean = false,
        val availableLabels: List<Label> = emptyList(),
        val availableMilestones: List<Milestone> = emptyList(),
        val availableMembers: List<User> = emptyList()
    ) : IssueDetailUiState

    data class Error(val message: String) : IssueDetailUiState
}
