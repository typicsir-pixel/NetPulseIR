package ir.netpulse.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 48, 28, 28)
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(0xFF0B1020.toInt())
        }
        val title = TextView(this).apply {
            text = "NetPulse IR"
            textSize = 26f
            setTextColor(0xFFFFFFFF.toInt())
        }
        status = TextView(this).apply {
            text = "پایش مستقل اینترنت و Cloudflare"
            textSize = 16f
            setTextColor(0xFFB8C7E0.toInt())
            setPadding(0, 24, 0, 24)
        }
        val start = Button(this).apply {
            text = "شروع پایش پس‌زمینه"
            setOnClickListener {
                val i = Intent(this@MainActivity, MonitorService::class.java)
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
                status.text = "درخواست شروع پایش ارسال شد. اعلان دائمی را بررسی کنید."
            }
        }
        val stop = Button(this).apply {
            text = "توقف پایش"
            setOnClickListener {
                stopService(Intent(this@MainActivity, MonitorService::class.java))
                status.text = "پایش متوقف شد."
            }
        }
        layout.addView(title); layout.addView(status); layout.addView(start); layout.addView(stop)
        setContentView(layout)
    }
}
