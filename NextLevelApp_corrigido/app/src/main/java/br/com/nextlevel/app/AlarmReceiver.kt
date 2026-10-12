package br.com.nextlevel.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("habit") ?: "Hora do seu hábito!"
        val id = intent.getIntExtra("id", 0)
        val time = intent.getStringExtra("time")

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel("alarmes", "Alarmes de hábito", NotificationManager.IMPORTANCE_HIGH)
        )
        val n = NotificationCompat.Builder(context, "alarmes")
            .setSmallIcon(R.drawable.ic_clock)
            .setContentTitle("Next Level")
            .setContentText(name)
            .setAutoCancel(true)
            .build()
        nm.notify(id, n)

        // recorrência diária: agenda o mesmo horário para amanhã
        if (time != null) scheduleAt(context, id.toLong(), name, time)
    }

    companion object {
        fun schedule(ctx: Context, h: Habit) {
            cancel(ctx, h.id)
            val alarm = h.alarm ?: return
            scheduleAt(ctx, h.id, h.name, alarm)
        }

        private fun scheduleAt(ctx: Context, id: Long, name: String, time: String) {
            val parts = time.split(":")
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                set(Calendar.MINUTE, parts[1].toInt())
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis() + 1000) add(Calendar.DAY_OF_YEAR, 1)
            }
            val pi = PendingIntent.getBroadcast(
                ctx, id.toInt(),
                Intent(ctx, AlarmReceiver::class.java)
                    .putExtra("habit", name).putExtra("id", id.toInt()).putExtra("time", time),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            try {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            } catch (e: SecurityException) {
                am.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            }
        }

        fun cancel(ctx: Context, hid: Long) {
            val pi = PendingIntent.getBroadcast(
                ctx, hid.toInt(), Intent(ctx, AlarmReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pi)
        }
    }
}
