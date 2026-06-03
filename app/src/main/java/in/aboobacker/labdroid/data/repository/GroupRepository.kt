package `in`.aboobacker.labdroid.data.repository

import com.apollographql.apollo.ApolloClient
import `in`.aboobacker.labdroid.GroupsQuery
import `in`.aboobacker.labdroid.data.model.Group
import `in`.aboobacker.labdroid.data.remote.GitLabApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor(
    private val api: GitLabApi,
    private val apolloClient: ApolloClient,
) {
    suspend fun getGroups(
        search: String? = null,
        ownedOnly: Boolean? = null,
        allAvailable: Boolean? = null
    ): List<Group> {
        val response = apolloClient.query(
            GroupsQuery(
                search = com.apollographql.apollo.api.Optional.presentIfNotNull(search),
            )
        ).execute()
        return response.data?.currentUser?.groups?.nodes?.mapNotNull { node ->
            node?.let {
                Group(
                    id = it.id?.substringAfterLast("/")?.toLongOrNull() ?: 0L,
                    name = it.name ?: "",
                    path = it.path,
                    fullName = it.fullName ?: "",
                    fullPath = it.fullPath,
                    description = it.description,
                    avatarUrl = it.avatarUrl,
                    webUrl = it.webUrl,
                    visibility = it.visibility
                )
            }
        } ?: emptyList()
    }

    suspend fun getGroup(groupId: Long) = api.getGroup(groupId)
    suspend fun getSubgroups(groupId: Long, page: Int = 1) = api.getSubgroups(groupId, page = page)
    suspend fun getGroupProjects(groupId: Long, page: Int = 1) =
        api.getGroupProjects(groupId, page = page)

    suspend fun getGroupMembers(groupId: Long, page: Int = 1) =
        api.getGroupMembers(groupId, page = page)

    suspend fun getGroupIssues(groupId: Long) = api.getGroupIssues(groupId)
    suspend fun getGroupMergeRequests(groupId: Long) = api.getGroupMergeRequests(groupId)
}
