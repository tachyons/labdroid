package `in`.aboobacker.labdroid.di

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo.cache.normalized.normalizedCache
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.apollo.network.okHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import `in`.aboobacker.labdroid.BuildConfig
import `in`.aboobacker.labdroid.GitLabWorkflowClient
import `in`.aboobacker.labdroid.data.local.AuthPreferences
import `in`.aboobacker.labdroid.data.remote.GitLabApi
import `in`.aboobacker.labdroid.data.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okio.Path.Companion.toPath
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
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
        @ApplicationContext context: Context,
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

        val cacheSize = 50L * 1024L * 1024L // 50MB
        val cache = Cache(File(context.cacheDir, "http_cache"), cacheSize)

        return OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                val token = authPreferences.getAccessToken()
                val host = request.url.host
                val gitLabHost = BuildConfig.GITLAB_BASE_URL.toUri().host
                val isGitLab =
                    host == gitLabHost || (gitLabHost != null && host.endsWith(".$gitLabHost"))

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
                val gitLabHost = BuildConfig.GITLAB_BASE_URL.toUri().host
                val isGitLab =
                    host == gitLabHost || (gitLabHost != null && host.endsWith(".$gitLabHost"))

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
            .baseUrl("${BuildConfig.GITLAB_BASE_URL}/api/v4/")
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
    fun provideNormalizedCacheFactory(
        @ApplicationContext context: Context
    ): NormalizedCacheFactory {
        val memoryCacheFactory = MemoryCacheFactory(maxSizeBytes = 10 * 1024 * 1024)
        val sqlCacheFactory = SqlNormalizedCacheFactory(context, "apollo.db")
        return memoryCacheFactory.chain(sqlCacheFactory)
    }

    @Provides
    @Singleton
    fun provideApolloClient(
        okHttpClient: OkHttpClient,
        cacheFactory: NormalizedCacheFactory
    ): ApolloClient {
        return ApolloClient.Builder()
            .serverUrl("${BuildConfig.GITLAB_BASE_URL}/api/graphql")
            .okHttpClient(okHttpClient)
            .normalizedCache(cacheFactory)
            .build()
    }

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").absolutePath.toPath())
                    .maxSizeBytes(100L * 1024L * 1024L) // 100MB
                    .build()
            }
            .components {
                add(OkHttpNetworkFetcherFactory(okHttpClient))
                add(SvgDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}
