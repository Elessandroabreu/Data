package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.ProductItem
import com.example.util.DateUtils
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

object ExpiryNotificationHelper {
    const val CHANNEL_ID = "data_validade_alertas"
    private const val NOTIFICATION_ID = 1001
    private const val PREFS_NAME = "data_expiry_cache"
    private const val KEY_ENABLED = "notifications_enabled"
    private const val KEY_PRODUCTS_JSON = "cached_active_products"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alertas de Validade",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Lembretes diários de produtos vencendo hoje ou em até 3 dias."
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun cacheProductsAndSettings(
        context: Context,
        enabled: Boolean,
        activeProducts: List<ProductItem>
    ) {
        val array = JSONArray()
        activeProducts.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("name", item.name)
                    put("marketName", item.marketName)
                    put("expiryDate", item.expiryDate)
                }
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putString(KEY_PRODUCTS_JSON, array.toString())
            .apply()
    }

    fun notifyUrgentFromCache(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, true)) return false
        if (!hasNotificationPermission(context)) return false
        val raw = prefs.getString(KEY_PRODUCTS_JSON, null) ?: return false

        val today = DateUtils.todayIso()
        var dueTodayCount = 0
        var due3DaysCount = 0
        val highlights = mutableListOf<String>()

        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val expiry = obj.optString("expiryDate", "")
                val diff = DateUtils.daysUntilExpiry(expiry, today)
                if (diff in 0L..3L) {
                    if (diff == 0L) dueTodayCount++ else due3DaysCount++
                    if (highlights.size < 3) {
                        val name = obj.optString("name", "").ifBlank { "Produto com foto" }
                        val market = obj.optString("marketName", "")
                        val label = if (diff == 0L) "HOJE" else "em ${diff}d"
                        highlights.add("$name ($market) - $label")
                    }
                }
            }
        } catch (_: Exception) {
            return false
        }

        val total = dueTodayCount + due3DaysCount
        if (total == 0) return false

        ensureChannel(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when {
            dueTodayCount > 0 && due3DaysCount > 0 ->
                "$dueTodayCount vence(m) hoje e $due3DaysCount em até 3 dias"
            dueTodayCount > 0 ->
                "Atenção: $dueTodayCount produto(s) vence(m) hoje!"
            else ->
                "$due3DaysCount produto(s) vencendo em até 3 dias"
        }

        val body = highlights.joinToString(" • ")
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun scheduleDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ExpiryNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerCal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun cancelDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ExpiryNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}

class ExpiryNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("data_expiry_cache", Context.MODE_PRIVATE)
            if (prefs.getBoolean("notifications_enabled", true)) {
                ExpiryNotificationHelper.scheduleDailyReminder(context)
            }
            return
        }
        ExpiryNotificationHelper.notifyUrgentFromCache(context)
    }
}
