package `in`.aboobacker.labdroid.data.repository

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import `in`.aboobacker.labdroid.GetAiChatAvailableModelsQuery
import `in`.aboobacker.labdroid.GetAiChatContextPresetsQuery
import `in`.aboobacker.labdroid.GetGitlabCreditsAvailableQuery
import javax.inject.Inject
import javax.inject.Singleton

data class DuoModel(
    val name: String,
    val ref: String,
    val provider: String? = null,
    val description: String? = null,
    val costIndicator: String? = null
)

data class AvailableDuoModels(
    val defaultModel: DuoModel?,
    val selectableModels: List<DuoModel>,
    val pinnedModel: DuoModel?
)

@Singleton
class DuoRepository @Inject constructor(
    private val apolloClient: ApolloClient
) {
    suspend fun getAiChatContextPresets(
        projectId: Long? = null,
        url: String? = null,
        questionCount: Int? = 4
    ): List<String> {
        val response = apolloClient.query(
            GetAiChatContextPresetsQuery(
                resourceId = Optional.absent(),
                projectId = Optional.presentIfNotNull(projectId?.let { "gid://gitlab/Project/$it" }),
                url = Optional.presentIfNotNull(url),
                questionCount = Optional.presentIfNotNull(questionCount)
            )
        ).execute()

        return response.data?.aiChatContextPresets?.questions?.filterNotNull() ?: emptyList()
    }

    suspend fun getAvailableModels(
        rootNamespaceId: Long? = null,
        namespaceId: Long? = null,
        projectId: Long? = null
    ): AvailableDuoModels? {
        val response = apolloClient.query(
            GetAiChatAvailableModelsQuery(
                rootNamespaceId = Optional.presentIfNotNull(rootNamespaceId?.let { "gid://gitlab/Group/$it" }),
                namespaceId = Optional.presentIfNotNull(namespaceId?.let { "gid://gitlab/Group/$it" }),
                projectId = Optional.presentIfNotNull(projectId?.let { "gid://gitlab/Project/$it" })
            )
        ).execute()

        val data = response.data?.aiChatAvailableModels ?: return null

        return AvailableDuoModels(
            defaultModel = data.defaultModel?.let {
                DuoModel(it.name, it.ref, it.modelProvider)
            },
            selectableModels = data.selectableModels?.map {
                DuoModel(it.name, it.ref, it.modelProvider, it.modelDescription, it.costIndicator)
            } ?: emptyList(),
            pinnedModel = data.pinnedModel?.let {
                DuoModel(it.name, it.ref)
            }
        )
    }

    suspend fun getGitlabCreditsAvailable(namespaceId: Long): Boolean {
        val response = apolloClient.query(
            GetGitlabCreditsAvailableQuery(
                namespaceId = Optional.presentIfNotNull("gid://gitlab/Group/$namespaceId")
            )
        ).execute()

        return response.data?.gitlabCreditsAvailable ?: false
    }
}
