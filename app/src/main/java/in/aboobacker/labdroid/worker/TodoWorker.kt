package `in`.aboobacker.labdroid.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import `in`.aboobacker.labdroid.data.local.TodoStore
import `in`.aboobacker.labdroid.data.remote.GitLabApi
import `in`.aboobacker.labdroid.notifications.NotificationHelper
import kotlinx.coroutines.flow.first

@HiltWorker
class TodoWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val api: GitLabApi,
    private val todoStore: TodoStore,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val todos = api.getTodos(state = "pending")
            val lastSeenId = todoStore.lastTodoId.first() ?: 0L

            val newTodos = todos.filter { it.id > lastSeenId }.sortedBy { it.id }

            if (newTodos.isNotEmpty()) {
                newTodos.forEach { todo ->
                    notificationHelper.showTodoNotification(todo)
                }
                todoStore.setLastTodoId(newTodos.last().id)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
