package `in`.aboobacker.labdroid.data.remote

import `in`.aboobacker.labdroid.data.model.Branch
import `in`.aboobacker.labdroid.data.model.Change
import `in`.aboobacker.labdroid.data.model.Commit
import `in`.aboobacker.labdroid.data.model.Discussion
import `in`.aboobacker.labdroid.data.model.Epic
import `in`.aboobacker.labdroid.data.model.Event
import `in`.aboobacker.labdroid.data.model.Group
import `in`.aboobacker.labdroid.data.model.GroupDetail
import `in`.aboobacker.labdroid.data.model.GroupMember
import `in`.aboobacker.labdroid.data.model.Issue
import `in`.aboobacker.labdroid.data.model.IssueRequest
import `in`.aboobacker.labdroid.data.model.Label
import `in`.aboobacker.labdroid.data.model.MarkdownUpload
import `in`.aboobacker.labdroid.data.model.MergeRequest
import `in`.aboobacker.labdroid.data.model.MergeRequestChanges
import `in`.aboobacker.labdroid.data.model.Milestone
import `in`.aboobacker.labdroid.data.model.Note
import `in`.aboobacker.labdroid.data.model.Pipeline
import `in`.aboobacker.labdroid.data.model.PipelineJob
import `in`.aboobacker.labdroid.data.model.Project
import `in`.aboobacker.labdroid.data.model.RepositoryFile
import `in`.aboobacker.labdroid.data.model.RepositoryItem
import `in`.aboobacker.labdroid.data.model.SearchResponse
import `in`.aboobacker.labdroid.data.model.Todo
import `in`.aboobacker.labdroid.data.model.User
import `in`.aboobacker.labdroid.data.model.UserUpdate
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface GitLabApi {
    @GET("user")
    suspend fun getCurrentUser(): User

    @GET("users")
    suspend fun searchUsers(@Query("username") username: String): List<User>

    @GET("users/{id}")
    suspend fun getUser(@Path("id") userId: Long): User

    @POST("users/{id}/follow")
    suspend fun followUser(@Path("id") userId: Long)

    @POST("users/{id}/unfollow")
    suspend fun unfollowUser(@Path("id") userId: Long)

    @PUT("user")
    suspend fun updateCurrentUser(@Body userUpdate: UserUpdate): User

    @GET("projects")
    suspend fun getProjects(
        @Query("membership") membership: Boolean = false,
        @Query("simple") simple: Boolean = true,
        @Query("order_by") orderBy: String = "last_activity_at",
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1
    ): List<Project>

    @GET("users/{id}/projects")
    suspend fun getUserProjects(
        @Path("id") userId: Long,
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1,
        @Query("simple") simple: Boolean = true,
        @Query("order_by") orderBy: String = "last_activity_at"
    ): List<Project>

    @GET("projects/{id}")
    suspend fun getProject(
        @Path("id") projectId: Long
    ): Project

    @Multipart
    @POST("projects/{id}/uploads")
    suspend fun uploadFile(
        @Path("id") projectId: Long,
        @Part file: MultipartBody.Part
    ): MarkdownUpload

    @GET("projects/{id}/repository/tree")
    suspend fun getRepositoryTree(
        @Path("id") projectId: Long,
        @Query("path") path: String? = null,
        @Query("ref") ref: String? = null,
        @Query("recursive") recursive: Boolean = false,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 100
    ): List<RepositoryItem>

    @GET("projects/{id}/repository/commits")
    suspend fun getCommits(
        @Path("id") projectId: Long,
        @Query("ref_name") ref: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): List<Commit>

    @GET("projects/{id}/repository/commits/{sha}")
    suspend fun getCommit(
        @Path("id") projectId: Long,
        @Path("sha") sha: String
    ): Commit

    @GET("projects/{id}/repository/commits/{sha}/discussions")
    suspend fun getCommitDiscussions(
        @Path("id") projectId: Long,
        @Path("sha") sha: String,
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1
    ): List<Discussion>

    @GET("projects/{id}/repository/commits/{sha}/diff")
    suspend fun getCommitDiff(
        @Path("id") projectId: Long,
        @Path("sha") sha: String
    ): List<Change>

    @GET("projects/{id}/repository/branches")
    suspend fun getBranches(
        @Path("id") projectId: Long
    ): List<Branch>

    @GET("events")
    suspend fun getEvents(
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1
    ): List<Event>

    @GET("users/{id}/events")
    suspend fun getUserEvents(
        @Path("id") userId: Long,
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1
    ): List<Event>

    @GET("projects")
    suspend fun getStarredProjects(
        @Query("starred") starred: Boolean = true,
        @Query("per_page") perPage: Int = 5
    ): List<Project>

    @GET("issues")
    suspend fun getIssues(
        @Query("scope") scope: String = "all",
        @Query("state") state: String = "opened",
        @Query("search") search: String? = null,
        @Query("assignee_username[]") assigneeUsername: String? = null,
        @Query("with_labels_details") withLabelsDetails: Boolean = true,
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1
    ): List<Issue>

    @GET("projects/{id}/issues")
    suspend fun getProjectIssues(
        @Path("id") projectId: Long,
        @Query("state") state: String = "opened",
        @Query("search") search: String? = null,
        @Query("scope") scope: String? = null,
        @Query("assignee_id") assigneeId: Long? = null,
        @Query("author_id") authorId: Long? = null,
        @Query("with_labels_details") withLabelsDetails: Boolean = true,
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1
    ): List<Issue>

    @GET("projects/{id}/merge_requests")
    suspend fun getProjectMergeRequests(
        @Path("id") projectId: Long,
        @Query("state") state: String = "opened",
        @Query("search") search: String? = null,
        @Query("scope") scope: String? = null,
        @Query("with_labels_details") withLabelsDetails: Boolean = true,
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1
    ): List<MergeRequest>

    @GET("projects/{id}/issues/{issue_iid}")
    suspend fun getIssue(
        @Path("id") projectId: Long,
        @Path("issue_iid") issueIid: Long,
        @Query("with_labels_details") withLabelsDetails: Boolean = true
    ): Issue

    @GET("projects/{id}/labels")
    suspend fun getProjectLabels(
        @Path("id") projectId: Long
    ): List<Label>

    @GET("projects/{id}/milestones")
    suspend fun getProjectMilestones(
        @Path("id") projectId: Long
    ): List<Milestone>

    @GET("projects/{id}/users")
    suspend fun getProjectMembers(
        @Path("id") projectId: Long
    ): List<User>

    @POST("projects/{id}/issues")
    suspend fun createIssue(
        @Path("id") projectId: Long,
        @Body issueRequest: IssueRequest
    ): Issue

    @PUT("projects/{id}/issues/{issue_iid}")
    suspend fun updateIssue(
        @Path("id") projectId: Long,
        @Path("issue_iid") issueIid: Long,
        @Body issueRequest: IssueRequest
    ): Issue

    @retrofit2.http.DELETE("projects/{id}/issues/{issue_iid}")
    suspend fun deleteIssue(
        @Path("id") projectId: Long,
        @Path("issue_iid") issueIid: Long
    )

    @GET("projects/{id}/issues/{issue_iid}/notes")
    suspend fun getIssueNotes(
        @Path("id") projectId: Long,
        @Path("issue_iid") issueIid: Long,
        @Query("sort") sort: String = "desc",
        @Query("order_by") orderBy: String = "created_at"
    ): List<Note>

    @GET("projects/{id}/issues/{issue_iid}/discussions")
    suspend fun getIssueDiscussions(
        @Path("id") projectId: Long,
        @Path("issue_iid") issueIid: Long,
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1
    ): List<Discussion>

    @POST("projects/{id}/issues/{issue_iid}/notes")
    suspend fun postIssueNote(
        @Path("id") projectId: Long,
        @Path("issue_iid") issueIid: Long,
        @Query("body") body: String
    ): Note

    @POST("projects/{id}/issues/{issue_iid}/discussions/{discussion_id}/notes")
    suspend fun postIssueDiscussionNote(
        @Path("id") projectId: Long,
        @Path("issue_iid") issueIid: Long,
        @Path("discussion_id") discussionId: String,
        @Query("body") body: String
    ): Note

    @POST("projects/{id}/repository/commits/{sha}/discussions/{discussion_id}/notes")
    suspend fun postCommitDiscussionNote(
        @Path("id") projectId: Long,
        @Path("sha") sha: String,
        @Path("discussion_id") discussionId: String,
        @Query("body") body: String
    ): Note

    @POST("projects/{id}/repository/commits/{sha}/notes")
    suspend fun postCommitNote(
        @Path("id") projectId: Long,
        @Path("sha") sha: String,
        @Query("body") body: String
    ): Note

    @POST("projects/{id}/merge_requests/{merge_request_iid}/notes")
    suspend fun postMergeRequestNote(
        @Path("id") projectId: Long,
        @Path("merge_request_iid") mergeRequestIid: Long,
        @Query("body") body: String
    ): Note

    @POST("projects/{id}/merge_requests/{merge_request_iid}/discussions/{discussion_id}/notes")
    suspend fun postMergeRequestDiscussionNote(
        @Path("id") projectId: Long,
        @Path("merge_request_iid") mergeRequestIid: Long,
        @Path("discussion_id") discussionId: String,
        @Query("body") body: String
    ): Note

    @GET("groups/{id}/epics")
    suspend fun getGroupEpics(
        @Path("id") groupId: Long,
        @Query("state") state: String = "opened"
    ): List<Epic>

    @GET("groups/{id}/epics/{epic_iid}")
    suspend fun getEpic(
        @Path("id") groupId: Long,
        @Path("epic_iid") epicIid: Long
    ): Epic

    @GET("groups/{id}/epics/{epic_iid}/discussions")
    suspend fun getEpicDiscussions(
        @Path("id") groupId: Long,
        @Path("epic_iid") epicIid: Long,
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1
    ): List<Discussion>

    @GET("groups/{id}/epics/{epic_iid}/issues")
    suspend fun getEpicIssues(
        @Path("id") groupId: Long,
        @Path("epic_iid") epicIid: Long
    ): List<Issue>

    @GET("groups")
    suspend fun getGroups(
        @Query("min_access_level") minAccessLevel: Int = 10,
        @Query("statistics") statistics: Boolean = true,
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1
    ): List<Group>

    @GET("groups/{id}")
    suspend fun getGroup(
        @Path("id") groupId: Long,
        @Query("with_projects") withProjects: Boolean = false
    ): GroupDetail

    @GET("groups/{id}/subgroups")
    suspend fun getSubgroups(
        @Path("id") groupId: Long,
        @Query("per_page") perPage: Int = 10,
        @Query("page") page: Int = 1
    ): List<Group>

    @GET("groups/{id}/projects")
    suspend fun getGroupProjects(
        @Path("id") groupId: Long,
        @Query("per_page") perPage: Int = 10,
        @Query("page") page: Int = 1
    ): List<Project>

    @GET("groups/{id}/members")
    suspend fun getGroupMembers(
        @Path("id") groupId: Long,
        @Query("per_page") perPage: Int = 10,
        @Query("page") page: Int = 1
    ): List<GroupMember>

    @GET("groups/{id}/issues")
    suspend fun getGroupIssues(
        @Path("id") groupId: Long,
        @Query("state") state: String = "opened",
        @Query("per_page") perPage: Int = 20
    ): List<Issue>

    @GET("groups/{id}/merge_requests")
    suspend fun getGroupMergeRequests(
        @Path("id") groupId: Long,
        @Query("state") state: String = "opened",
        @Query("per_page") perPage: Int = 20
    ): List<MergeRequest>

    @GET("todos")
    suspend fun getTodos(
        @Query("state") state: String = "pending",
        @Query("action") action: String? = null,
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1
    ): List<Todo>

    @POST("todos/{id}/mark_as_done")
    suspend fun markTodoAsDone(@Path("id") todoId: Long)

    @GET("projects/{id}/pipelines")
    suspend fun getProjectPipelines(
        @Path("id") projectId: Long,
        @Query("ref") ref: String? = null,
        @Query("status") status: String? = null,
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1
    ): List<Pipeline>

    @GET("projects/{id}/pipelines/latest")
    suspend fun getLatestPipeline(
        @Path("id") projectId: Long,
        @Query("ref") ref: String? = null
    ): Pipeline

    @GET("projects/{id}/pipelines/{pipeline_id}")
    suspend fun getPipeline(
        @Path("id") projectId: Long,
        @Path("pipeline_id") pipelineId: Long
    ): Pipeline

    @GET("projects/{id}/pipelines/{pipeline_id}/jobs")
    suspend fun getPipelineJobs(
        @Path("id") projectId: Long,
        @Path("pipeline_id") pipelineId: Long
    ): List<PipelineJob>

    @GET("merge_requests")
    suspend fun getMergeRequests(
        @Query("scope") scope: String = "all",
        @Query("state") state: String = "opened",
        @Query("search") search: String? = null,
        @Query("author_id") authorId: Long? = null,
        @Query("assignee_id") assigneeId: Long? = null,
        @Query("reviewer_username") reviewerUsername: String? = null,
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1
    ): List<MergeRequest>

    @GET("projects/{id}/merge_requests/{merge_request_iid}")
    suspend fun getMergeRequest(
        @Path("id") projectId: Long,
        @Path("merge_request_iid") mergeRequestIid: Long
    ): MergeRequest

    @GET("projects/{id}/merge_requests/{merge_request_iid}/notes")
    suspend fun getMergeRequestNotes(
        @Path("id") projectId: Long,
        @Path("merge_request_iid") mergeRequestIid: Long
    ): List<Note>

    @GET("projects/{id}/merge_requests/{merge_request_iid}/discussions")
    suspend fun getMergeRequestDiscussions(
        @Path("id") projectId: Long,
        @Path("merge_request_iid") mergeRequestIid: Long,
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1
    ): List<Discussion>

    @GET("projects/{id}/merge_requests/{merge_request_iid}/changes")
    suspend fun getMergeRequestChanges(
        @Path("id") projectId: Long,
        @Path("merge_request_iid") mergeRequestIid: Long
    ): MergeRequestChanges

    @GET("projects/{id}/merge_requests/{merge_request_iid}/commits")
    suspend fun getMergeRequestCommits(
        @Path("id") projectId: Long,
        @Path("merge_request_iid") mergeRequestIid: Long
    ): List<Commit>

    @GET("projects/{id}/merge_requests/{merge_request_iid}/pipelines")
    suspend fun getMergeRequestPipelines(
        @Path("id") projectId: Long,
        @Path("merge_request_iid") mergeRequestIid: Long
    ): List<Pipeline>

    @PUT("projects/{id}/merge_requests/{merge_request_iid}")
    suspend fun updateMergeRequest(
        @Path("id") projectId: Long,
        @Path("merge_request_iid") mergeRequestIid: Long,
        @Query("title") title: String? = null,
        @Query("description") description: String? = null,
        @Query("state_event") stateEvent: String? = null,
        @Query("assignee_id") assigneeId: Long? = null,
        @Query("milestone_id") milestoneId: Long? = null,
        @Query("labels") labels: String? = null
    ): MergeRequest

    @PUT("projects/{id}/merge_requests/{merge_request_iid}/merge")
    suspend fun mergeMergeRequest(
        @Path("id") projectId: Long,
        @Path("merge_request_iid") mergeRequestIid: Long,
        @Query("merge_commit_message") mergeCommitMessage: String? = null,
        @Query("should_remove_source_branch") shouldRemoveSourceBranch: Boolean? = null
    ): MergeRequest

    @GET("projects/{id}/repository/files/{file_path}")
    suspend fun getRepositoryFile(
        @Path("id") projectId: Long,
        @Path("file_path") filePath: String,
        @Query("ref") ref: String = "HEAD"
    ): RepositoryFile

    @GET("search")
    suspend fun search(
        @Query("scope") scope: String,
        @Query("search") search: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): List<SearchResponse>

    @GET("groups/{id}/search")
    suspend fun searchGroup(
        @Path("id") groupId: Long,
        @Query("scope") scope: String,
        @Query("search") search: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): List<SearchResponse>

    @GET("projects/{id}/search")
    suspend fun searchProject(
        @Path("id") projectId: Long,
        @Query("scope") scope: String,
        @Query("search") search: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): List<SearchResponse>

    @GET
    suspend fun getContributionCalendar(@retrofit2.http.Url url: String): Map<String, Int>
}
