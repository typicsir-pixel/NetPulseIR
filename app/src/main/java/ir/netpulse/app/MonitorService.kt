package ir.netpulse.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.*
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MonitorService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var cm: ConnectivityManager
    private val prefs by lazy { getSharedPreferences("netpulse", Context.MODE_PRIVATE) }
    private val tick = object : Runnable {
        override fun run() {
            worker.execute { performChecks() }
            handler.postDelayed(this, 30_000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(41, notification("پایش شبکه فعال شد"))
        handler.removeCallbacks(tick)
        tick.run()
        return START_STICKY
    }

    private fun performChecks() {
        val active = cm.activeNetwork
        val activeCaps = active?.let { cm.getNetworkCapabilities(it) }
        val vpn = activeCaps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        val networks = cm.allNetworks
        val lines = mutableListOf<String>()
        var bestCloudflare = "--"
        var bestMs = -1L

        for (network in networks) {
            val caps = cm.getNetworkCapabilities(network) ?: continue
            val kind = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "سیم‌کارت"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                else -> continue
            }
            val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            val result = probe(network)
            val label = if (result.first) "Cloudflare OK" else "Cloudflare ناموفق"
            lines.add("$kind: ${if (validated) "اینترنت دارد" else "اعتبارسنجی نشده"}، $label")
            if (result.first && (bestMs < 0 || result.second < bestMs)) {
                bestMs = result.second
                bestCloudflare = "OK"
            }
        }

        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val networkText = if (lines.isEmpty()) "هیچ شبکه قابل بررسی پیدا نشد" else lines.distinct().joinToString(" | ")
        val latencyText = if (bestMs >= 0) "$bestMs ms" else "--"
        prefs.edit()
            .putString("network", networkText)
            .putString("cloudflare", bestCloudflare)
            .putString("latency", latencyText)
            .putString("vpn", if (vpn) "روشن" else "خاموش")
            .putString("updated", time)
            .apply()

        val summary = if (vpn) "VPN روشن است؛ مسیر تست ممکن است از تونل عبور کند" else networkText
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(41, notification(summary))
        sendBroadcast(Intent("ir.netpulse.app.UPDATE_WIDGET").setPackage(packageName))
    }

    private fun probe(network: Network): Pair<Boolean, Long> {
        var conn: HttpURLConnection? = null
        return try {
            val start = System.nanoTime()
            conn = network.openConnection(URL("https://www.cloudflare.com/cdn-cgi/trace")) as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.instanceFollowRedirects = true
            conn.requestMethod = "GET"
            conn.setRequestProperty("Cache-Control", "no-cache")
            val code = conn.responseCode
            val elapsed = (System.nanoTime() - start) / 1_000_000
            Pair(code in 200..299, elapsed)
        } catch (_: Exception) {
            Pair(false, -1L)
        } finally {
            conn?.disconnect()
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel("netpulse_monitor", "NetPulse monitoring",
                NotificationManager.IMPORTANCE_LOW)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun notification(text: String): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0))
        val builder = if (Build.VERSION.SDK_INT >= 26)
            Notification.Builder(this, "netpulse_monitor") else Notification.Builder(this)
        return builder.setContentTitle("NetPulse IR")
            .setContentText(text.take(110))
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        worker.shutdownNow()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
