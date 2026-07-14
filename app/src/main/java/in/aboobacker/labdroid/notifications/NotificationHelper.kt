package `in`.aboobacker.labdroid.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.aboobacker.labdroid.MainActivity
import `in`.aboobacker.labdroid.R
import `in`.aboobacker.labdroid.data.model.Todo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val TODO_CHANNEL_ID = "todo_notifications"
        const val TODO_GROUP_KEY = "in.aboobacker.labdroid.TODO_GROUP"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val name = "Todos"
        val descriptionText = "Notifications for new GitLab todos"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(TODO_CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun showTodoNotification(todo: Todo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, todo.id.toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = todo.target?.title ?: "New Todo"
        val body = todo.body ?: todo.actionName ?: "You have a new action required"

        val builder = NotificationCompat.Builder(context, TODO_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(TODO_GROUP_KEY)

        val summaryNotification = NotificationCompat.Builder(context, TODO_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setGroup(TODO_GROUP_KEY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).apply {
            notify(todo.id.toInt(), builder.build())
            notify(0, summaryNotification)
        }
    }
}
