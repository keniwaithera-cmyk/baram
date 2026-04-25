package com.barakaplaza.rentmanager.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.barakaplaza.rentmanager.database.DatabaseHelper
import java.text.SimpleDateFormat
import java.util.*

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ReminderReceiver"
        const val ACTION_SEND_REMINDER = "com.barakaplaza.SEND_REMINDER"

        fun scheduleMonthlyReminders(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_SEND_REMINDER
            }

            fun pendingIntent(id: Int) = PendingIntent.getBroadcast(
                context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 28th – advance notice
            val cal28 = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 28)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                if (before(Calendar.getInstance())) add(Calendar.MONTH, 1)
            }
            // 1st – new month reminder
            val cal1 = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 8)
                if (before(Calendar.getInstance())) add(Calendar.MONTH, 1)
            }
            // 5th – due date
            val cal5 = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 5)
                set(Calendar.HOUR_OF_DAY, 8)
                if (before(Calendar.getInstance())) add(Calendar.MONTH, 1)
            }

            val monthMs = AlarmManager.INTERVAL_DAY * 30
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, cal28.timeInMillis, monthMs, pendingIntent(1001))
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, cal1.timeInMillis, monthMs, pendingIntent(1002))
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, cal5.timeInMillis, monthMs, pendingIntent(1003))
            Log.d(TAG, "Reminders scheduled for 28th, 1st and 5th of each month")
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == ACTION_SEND_REMINDER || action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Sending monthly reminders...")
            sendMonthlyReminders(context)
            if (action == Intent.ACTION_BOOT_COMPLETED) scheduleMonthlyReminders(context)
        }
    }

    private fun sendMonthlyReminders(context: Context) {
        val db = DatabaseHelper.getInstance(context)
        val tenants = db.getAllActiveTenants()
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val deadline = "5th ${SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)}"

        tenants.forEach { tenant ->
            val house = db.getHouseByNumber(tenant.houseNumber) ?: return@forEach
            val rent = house.monthlyRent
            when {
                day >= 28 || day <= 2 -> SmsUtils.sendMonthlyReminder(context, tenant, rent, deadline)
                day in 6..10          -> SmsUtils.sendOverdueReminder(context, tenant, rent, day - 5)
                day in 3..5           -> SmsUtils.sendMonthlyReminder(context, tenant, rent, "TODAY (5th)!")
            }
        }
    }
}
