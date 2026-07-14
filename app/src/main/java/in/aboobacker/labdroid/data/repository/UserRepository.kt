package `in`.aboobacker.labdroid.data.repository

import com.apollographql.apollo.ApolloClient
import `in`.aboobacker.labdroid.DashboardQuery
import `in`.aboobacker.labdroid.ProfileQuery
import `in`.aboobacker.labdroid.data.model.User
import `in`.aboobacker.labdroid.data.remote.GitLabApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val api: GitLabApi,
    private val apolloClient: ApolloClient
) {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    suspend fun getDashboardData(): DashboardQuery.Data? {
        val response = apolloClient.query(DashboardQuery()).execute()
        if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "GraphQL Error")
        }
        val data = response.data
        data?.currentUser?.let {
            _currentUser.value = User(
                id = it.id.toString().substringAfterLast("/").toLong(),
                name = it.name,
                username = it.username,
                avatarUrl = it.avatarUrl
            )
        }
        return data
    }

    suspend fun getProfileData(): ProfileQuery.Data? {
        val response = apolloClient.query(ProfileQuery()).execute()
        if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "GraphQL Error")
        }
        val data = response.data
        data?.currentUser?.let {
            _currentUser.value = User(
                id = it.id.toString().substringAfterLast("/").toLong(),
                name = it.name,
                username = it.username,
                avatarUrl = it.avatarUrl
            )
        }
        return data
    }

    fun setCurrentUser(user: User) {
        _currentUser.value = user
    }

    fun clear() {
        _currentUser.value = null
    }
}
