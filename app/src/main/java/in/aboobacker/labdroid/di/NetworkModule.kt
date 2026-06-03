package `in`.aboobacker.labdroid.di

import android.util.Log
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.svg.SvgDecoder
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.okHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import `in`.aboobacker.labdroid.BuildConfig
import `in`.aboobacker.labdroid.GitLabWorkflowClient
import `in`.aboobacker.labdroid.data.local.AuthPreferences
import `in`.aboobacker.labdroid.data.remote.GitLabApi
import `in`.aboobacker.labdroid.data.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val TAG = "NetworkModule"

    @Provides
    @Singleton
    fun provideGitLabWorkflowClient(okHttpClient: OkHttpClient): GitLabWorkflowClient {
        return GitLabWorkflowClient(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authPreferences: AuthPreferences,
        authRepositoryProvider: Provider<AuthRepository>
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                val token = authPreferences.getAccessToken()
                val host = request.url.host
                val isGitLab = host == "gitlab.com" || host.endsWith(".gitlab.com")

                val authenticatedRequest = if (token != null && isGitLab) {
                    request.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    request
                }

                val response = chain.proceed(authenticatedRequest)

                if (response.code == 401 && token != null && isGitLab) {
                    Log.d(TAG, "Encountered 401, attempting token refresh...")
                    synchronized(this) {
                        val currentToken = authPreferences.getAccessToken()

                        if (currentToken != token && currentToken != null) {
                            Log.d(TAG, "Token already refreshed, retrying...")
                            response.close()
                            val newRequest = request.newBuilder()
                                .header("Authorization", "Bearer $currentToken")
                                .build()
                            return@addInterceptor chain.proceed(newRequest)
                        }

                        val refreshResult = runBlocking {
                            authRepositoryProvider.get().refreshToken()
                        }

                        if (refreshResult.isSuccess) {
                            val newToken = refreshResult.getOrNull()
                            if (newToken != null) {
                                Log.d(TAG, "Token refreshed, retrying...")
                                response.close()
                                val newRequest = request.newBuilder()
                                    .header("Authorization", "Bearer $newToken")
                                    .build()
                                return@addInterceptor chain.proceed(newRequest)
                            }
                        } else {
                            Log.e(TAG, "Token refresh failed")
                            authPreferences.clear()
                        }
                    }
                }

                response
            }
            .addNetworkInterceptor { chain ->
                // Ensure authentication is present even after redirects on GitLab
                val request = chain.request()
                val token = authPreferences.getAccessToken()
                val host = request.url.host
                val isGitLab = host == "gitlab.com" || host.endsWith(".gitlab.com")

                if (token != null && isGitLab && request.header("Authorization") == null) {
                    val authenticatedRequest = request.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                    chain.proceed(authenticatedRequest)
                } else {
                    chain.proceed(request)
                }
            }
            .addNetworkInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://gitlab.com/api/v4/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideGitLabApi(retrofit: Retrofit): GitLabApi {
        return retrofit.create(GitLabApi::class.java)
    }

    @Provides
    @Singleton
    fun provideApolloClient(okHttpClient: OkHttpClient): ApolloClient {
        return ApolloClient.Builder()
            .serverUrl("https://gitlab.com/api/graphql")
            .okHttpClient(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    fun provideImageLoader(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context,
        okHttpClient: OkHttpClient
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(okHttpClient))
                add(SvgDecoder.Factory())
            }
            .build()
    }
}
