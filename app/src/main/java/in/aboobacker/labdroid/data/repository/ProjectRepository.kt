package `in`.aboobacker.labdroid.data.repository

import com.apollographql.apollo.ApolloClient
import `in`.aboobacker.labdroid.DashboardQuery
import `in`.aboobacker.labdroid.ProjectsQuery
import `in`.aboobacker.labdroid.data.local.LocalProjectCache
import `in`.aboobacker.labdroid.data.model.MarkdownUpload
import `in`.aboobacker.labdroid.data.model.Project
import `in`.aboobacker.labdroid.data.remote.GitLabApi
import kotlinx.coroutines.flow.Flow
import okhttp3.MultipartBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(
    private val api: GitLabApi,
    private val apolloClient: ApolloClient,
    private val localCache: LocalProjectCache
) {
    val recentProjects: Flow<List<Project>> = localCache.recentProjects

    suspend fun getDashboardData(): DashboardQuery.Data? {
        val response = apolloClient.query(DashboardQuery()).execute()
        if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "GraphQL Error")
        }
        return response.data
    }

    suspend fun getProjects(): List<Project> {
        val response = apolloClient.query(
            ProjectsQuery(
                membership = com.apollographql.apollo.api.Optional.present(null),
                search = com.apollographql.apollo.api.Optional.present(null)
            )
        ).execute()
        return response.data?.projects?.nodes?.mapNotNull { node ->
            node?.let {
                Project(
                    id = it.id.substringAfterLast("/").toLong(),
                    name = it.name,
                    nameWithNamespace = it.nameWithNamespace,
                    pathWithNamespace = it.fullPath,
                    description = it.description,
                    avatarUrl = it.avatarUrl,
                    starCount = it.starCount,
                    forksCount = it.forksCount,
                    lastActivityAt = it.lastActivityAt?.toString(),
                    visibility = it.visibility,
                    webUrl = it.webUrl
                )
            }
        } ?: emptyList()
    }

    suspend fun getProject(projectId: Long): Project {
        return api.getProject(projectId)
    }

    suspend fun uploadFile(projectId: Long, file: MultipartBody.Part): MarkdownUpload {
        return api.uploadFile(projectId, file)
    }

    suspend fun saveRecentProject(project: Project) {
        localCache.saveProject(project)
    }

    suspend fun getRepositoryTree(projectId: Long, path: String? = null, ref: String? = null) =
        api.getRepositoryTree(projectId, path, ref)

    suspend fun getLastCommit(projectId: Long, ref: String? = null) =
        api.getCommits(projectId, ref, perPage = 1).firstOrNull()

    suspend fun getProjectPipelines(projectId: Long) =
        api.getProjectPipelines(projectId)

    suspend fun getLatestPipeline(projectId: Long, ref: String? = null) =
        try {
            api.getLatestPipeline(projectId, ref)
        } catch (e: Exception) {
            null
        }

    suspend fun getPipelineJobs(projectId: Long, pipelineId: Long) =
        try {
            api.getPipelineJobs(projectId, pipelineId)
        } catch (e: Exception) {
            emptyList()
        }

    suspend fun getProjectIssues(projectId: Long) =
        api.getProjectIssues(projectId)

    suspend fun getProjectMergeRequests(projectId: Long) =
        api.getProjectMergeRequests(projectId)

    suspend fun getRepositoryFile(projectId: Long, filePath: String, ref: String = "HEAD") =
        api.getRepositoryFile(projectId, filePath, ref)
}
