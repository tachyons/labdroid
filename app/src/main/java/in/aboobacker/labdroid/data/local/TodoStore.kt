package `in`.aboobacker.labdroid.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "todo_prefs")

@Singleton
class TodoStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val LAST_TODO_ID = longPreferencesKey("last_todo_id")

    val lastTodoId: Flow<Long?> = context.dataStore.data
        .map { preferences ->
            preferences[LAST_TODO_ID]
        }

    suspend fun setLastTodoId(id: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_TODO_ID] = id
        }
    }
}
