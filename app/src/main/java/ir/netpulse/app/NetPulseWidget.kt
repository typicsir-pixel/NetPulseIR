package ir.netpulse.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.widget.RemoteViews

class NetPulseWidget : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "ir.netpulse.app.UPDATE_WIDGET") updateAll(context)
    }
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, manager, it) }
    }
    private fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, NetPulseWidget::class.java))
        ids.forEach { update(context, manager, it) }
    }
    private fun update(context: Context, manager: AppWidgetManager, id: Int) {
        val p = context.getSharedPreferences("netpulse", Context.MODE_PRIVATE)
        val views = RemoteViews(context.packageName, R.layout.netpulse_widget)
        views.setTextViewText(R.id.network, p.getString("network", "در انتظار شروع پایش"))
        views.setTextViewText(R.id.vpn, "VPN: " + p.getString("vpn", "نامشخص"))
        views.setTextViewText(R.id.latency,
            "Cloudflare: ${p.getString("cloudflare", "--")} | تأخیر: ${p.getString("latency", "--")}")
        views.setTextViewText(R.id.updated, "آخرین بررسی: ${p.getString("updated", "--")}")
        manager.updateAppWidget(id, views)
    }
}
