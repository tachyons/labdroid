package `in`.aboobacker.labdroid.data.repository

import `in`.aboobacker.labdroid.data.model.Discussion
import `in`.aboobacker.labdroid.data.model.Issue
import `in`.aboobacker.labdroid.data.model.IssueRequest
import `in`.aboobacker.labdroid.data.model.Note
import `in`.aboobacker.labdroid.data.remote.GitLabApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IssueRepository @Inject constructor(
    private val api: GitLabApi
) {
    suspend fun getIssues(): List<Issue> = api.getIssues()

    suspend fun getIssue(projectId: Long, issueIid: Long): Issue =
        api.getIssue(projectId, issueIid)

    suspend fun createIssue(projectId: Long, issueRequest: IssueRequest): Issue =
        api.createIssue(projectId, issueRequest)

    suspend fun updateIssue(projectId: Long, issueIid: Long, issueRequest: IssueRequest): Issue =
        api.updateIssue(projectId, issueIid, issueRequest)

    suspend fun deleteIssue(projectId: Long, issueIid: Long) =
        api.deleteIssue(projectId, issueIid)

    suspend fun getIssueNotes(projectId: Long, issueIid: Long): List<Note> =
        api.getIssueNotes(projectId, issueIid)

    suspend fun getIssueDiscussions(
        projectId: Long,
        issueIid: Long,
        page: Int = 1
    ): List<Discussion> =
        api.getIssueDiscussions(projectId, issueIid, page = page)

    suspend fun postIssueNote(
        projectId: Long,
        issueIid: Long,
        body: String,
        discussionId: String? = null
    ): Note =
        if (discussionId != null) {
            api.postIssueDiscussionNote(projectId, issueIid, discussionId, body)
        } else {
            api.postIssueNote(projectId, issueIid, body)
        }
}
