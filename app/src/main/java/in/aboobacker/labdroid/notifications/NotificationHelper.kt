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
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.LocusIdCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
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
        const val DUO_CHANNEL_ID = "duo_chat_notifications"
        const val DUO_SHORTCUT_ID = "duo_chat_shortcut"
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val DUO_NOTIFICATION_ID = 200
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        // Todo channel
        val todoName = "Todos"
        val todoDescription = "Notifications for new GitLab todos"
        val todoImportance = NotificationManager.IMPORTANCE_DEFAULT
        val todoChannel = NotificationChannel(TODO_CHANNEL_ID, todoName, todoImportance).apply {
            description = todoDescription
        }
        notificationManager.createNotificationChannel(todoChannel)

        // Duo channel
        val duoName = context.getString(R.string.duo_chat_channel_name)
        val duoDescription = context.getString(R.string.duo_chat_channel_description)
        val duoImportance = NotificationManager.IMPORTANCE_HIGH
        val duoChannel = NotificationChannel(DUO_CHANNEL_ID, duoName, duoImportance).apply {
            description = duoDescription
        }
        notificationManager.createNotificationChannel(duoChannel)
    }

    fun showDuoChatNotification(message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val duoPerson = Person.Builder()
            .setName(context.getString(R.string.duo_name))
            .setImportant(true)
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_launcher_foreground))
            .build()

        val userPerson = Person.Builder()
            .setName("You")
            .build()

        // Create shortcut for Bubbles and Conversation section
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("screen", "duo_chat")
        }

        val shortcut = ShortcutInfoCompat.Builder(context, DUO_SHORTCUT_ID)
            .setLocusId(LocusIdCompat(DUO_SHORTCUT_ID))
            .setShortLabel(context.getString(R.string.duo_name))
            .setIntent(intent)
            .setLongLived(true)
            .setPerson(duoPerson)
            .build()

        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // RemoteInput for direct reply
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel(context.getString(R.string.reply_hint))
            .build()

        val replyIntent = Intent(context, DuoReplyReceiver::class.java)
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            replyIntent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_launcher_foreground,
            context.getString(R.string.reply),
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        // Bubble metadata
        val bubbleMetadata = NotificationCompat.BubbleMetadata.Builder(
            pendingIntent,
            IconCompat.createWithResource(context, R.drawable.ic_launcher_foreground)
        )
            .setDesiredHeight(600)
            .build()

        val messagingStyle = NotificationCompat.MessagingStyle(userPerson)
            .setConversationTitle(context.getString(R.string.duo_chat))
            .addMessage(message, System.currentTimeMillis(), duoPerson)

        val builder = NotificationCompat.Builder(context, DUO_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setStyle(messagingStyle)
            .setShortcutId(DUO_SHORTCUT_ID)
            .addPaddingCharacter(LocusIdCompat(DUO_SHORTCUT_ID))
            .setContentIntent(pendingIntent)
            .setBubbleMetadata(bubbleMetadata)
            .addAction(replyAction)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)

        notificationManager.notify(DUO_NOTIFICATION_ID, builder.build())
    }

    private fun NotificationCompat.Builder.addPaddingCharacter(locusId: LocusIdCompat): NotificationCompat.Builder {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            this.setLocusId(locusId)
        }
        return this
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
