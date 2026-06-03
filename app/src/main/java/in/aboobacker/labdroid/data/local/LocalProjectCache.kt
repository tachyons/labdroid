package `in`.aboobacker.labdroid.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.aboobacker.labdroid.data.model.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "project_cache")

@Singleton
class LocalProjectCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val RECENT_PROJECTS_KEY = stringPreferencesKey("recent_projects")

    val recentProjects: Flow<List<Project>> = context.dataStore.data.map { preferences ->
        val json = preferences[RECENT_PROJECTS_KEY] ?: "[]"
        try {
            Json.decodeFromString<List<Project>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveProject(project: Project) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[RECENT_PROJECTS_KEY] ?: "[]"
            val currentList = try {
                Json.decodeFromString<List<Project>>(currentJson).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }

            // Remove if already exists to move to top
            currentList.removeAll { it.id == project.id }
            currentList.add(0, project)

            // Keep only last 10
            val newList = currentList.take(10)
            preferences[RECENT_PROJECTS_KEY] = Json.encodeToString(newList)
        }
    }
}
