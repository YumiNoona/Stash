package com.lockr.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.*
import android.os.Bundle
import android.widget.RemoteViews

class WalletWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = updateWidgets(context)
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) = updateWidgets(context)
}
class DocumentsWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = updateWidgets(context)
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) = updateWidgets(context)
}

fun updateWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val prefs = context.getSharedPreferences("lockr_preferences", 0)
    val dark = when (prefs.getString("theme_mode", "system")) {
        "dark" -> true; "light" -> false
        else -> context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }
    listOf(WalletWidget::class.java to "cards", DocumentsWidget::class.java to "documents").forEach { (provider, target) ->
        manager.getAppWidgetIds(ComponentName(context, provider)).forEach { id ->
            val view = RemoteViews(context.packageName, R.layout.stash_widget)
            val locked = prefs.getBoolean("app_lock_enabled", false)
            val count = prefs.getInt("widget_${target}_count", 0)
            view.setImageViewBitmap(R.id.widget_art, widgetArtwork(context, target == "documents", dark, if (locked) "Tap to unlock" else "$count saved · Tap to open"))
            val intent = Intent(context, MainActivity::class.java).setAction("com.lockr.app.WIDGET.$target")
                .putExtra("widget_target", target).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            view.setOnClickPendingIntent(R.id.widget_art, PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            view.setContentDescription(R.id.widget_art, if (target == "cards") "Stash wallet. Tap to open cards." else "Stash documents. Tap to open documents.")
            manager.updateAppWidget(id, view)
        }
    }
}

/** Only generic artwork/counts reach the launcher, never vault thumbnails or private values. */
fun widgetArtwork(context: Context, documents: Boolean, dark: Boolean, subtitle: String): Bitmap {
    val bitmap = Bitmap.createBitmap(720, 480, Bitmap.Config.ARGB_8888)
    val c = Canvas(bitmap)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    val ink = if (dark) Color.rgb(238, 236, 248) else Color.rgb(247, 250, 255)
    p.setShadowLayer(18f, 0f, 10f, 0x55203050)
    if (documents) {
        p.shader = LinearGradient(40f, 80f, 640f, 420f, intArrayOf(0xFF8DDDF7.toInt(),0xFF3887C9.toInt()), null, Shader.TileMode.CLAMP)
        c.drawRoundRect(70f, 112f, 650f, 430f, 40f, 40f, p)
        c.drawRoundRect(70f, 72f, 310f, 170f, 38f, 38f, p)
        p.clearShadowLayer(); p.shader = null
        repeat(3) { i ->
            c.save(); c.rotate((i - 1) * 7f, 360f, 330f)
            p.color = Color.rgb(235+i*7, 239+i*5, 246+i*3)
            c.drawRoundRect(170f+i*75, 100f-i*12, 380f+i*70, 365f, 15f, 15f, p)
            p.color = 0xFFBDCBDF.toInt(); repeat(4) { line -> c.drawRoundRect(190f+i*75, 150f+line*25-i*12, 340f+i*70, 156f+line*25-i*12, 3f,3f,p) }
            c.restore()
        }
        val path=Path().apply { moveTo(70f,245f); quadTo(70f,215f,110f,215f); lineTo(390f,215f); quadTo(410f,215f,430f,191f); quadTo(445f,175f,475f,175f); lineTo(610f,175f); quadTo(650f,175f,650f,215f); lineTo(650f,390f); quadTo(650f,430f,610f,430f); lineTo(110f,430f); quadTo(70f,430f,70f,390f); close() }
        p.shader = LinearGradient(80f,190f,610f,430f, intArrayOf(0xFF9DE5FF.toInt(),0xFF42A8E2.toInt(),0xFF428ACA.toInt()),null,Shader.TileMode.CLAMP);c.drawPath(path,p)
    } else {
        p.shader=LinearGradient(60f,50f,620f,430f,intArrayOf(if(dark) 0xFF34343F.toInt() else 0xFF455266.toInt(),0xFF191F2B.toInt()),null,Shader.TileMode.CLAMP)
        c.drawRoundRect(40f,35f,680f,440f,42f,42f,p);p.clearShadowLayer()
        repeat(3) { i ->
            val top=65f+i*43f
            p.shader=LinearGradient(90f,top,620f,top+180f,intArrayOf(intArrayOf(0xFF7E97B7.toInt(),0xFFD9BC75.toInt(),0xFFB3A0CE.toInt())[i],0xFF526278.toInt()),null,Shader.TileMode.CLAMP)
            c.drawRoundRect(90f,top,630f,top+220f,20f,20f,p)
            p.shader=null;p.color=0xFFEEF0F7.toInt();p.textSize=24f;p.typeface=Typeface.create("sans-serif",Typeface.BOLD)
            c.drawText(listOf("STASH","YOUR WALLET","••••  ••••  ••••  ••••")[i],115f,top+35f,p)
        }
        p.shader=LinearGradient(40f,200f,640f,440f,intArrayOf(if(dark) 0xFF30313B.toInt() else 0xFF414957.toInt(),0xFF202431.toInt()),null,Shader.TileMode.CLAMP)
        val pocket=Path().apply { moveTo(40f,205f); cubicTo(190f,178f,260f,260f,365f,245f); cubicTo(475f,230f,545f,185f,680f,205f); lineTo(680f,398f); quadTo(680f,440f,638f,440f);lineTo(82f,440f);quadTo(40f,440f,40f,398f);close() }
        c.drawPath(pocket,p)
        p.shader=null;p.color=0x668F93A6;p.style=Paint.Style.STROKE;p.strokeWidth=2f;p.pathEffect=DashPathEffect(floatArrayOf(6f,5f),0f)
        c.drawRoundRect(56f,52f,664f,424f,30f,30f,p);p.pathEffect=null;p.style=Paint.Style.FILL
    }
    p.shader=null;p.color=ink;p.typeface=Typeface.create("sans-serif",Typeface.BOLD);p.textSize=34f
    c.drawText(if(documents) "Documents" else "Stash wallet", if(documents)105f else 85f,350f,p)
    p.textSize=23f;p.typeface=Typeface.create("sans-serif",Typeface.NORMAL);p.color=0xDDEDF3FF.toInt()
    c.drawText(subtitle,if(documents)105f else 85f,387f,p)
    return bitmap
}

fun pinStashWidget(context: Context, documents: Boolean) {
    val manager = AppWidgetManager.getInstance(context)
    if (!manager.isRequestPinAppWidgetSupported) {
        android.widget.Toast.makeText(context, "Touch and hold your home screen, choose Widgets, then Stash.", android.widget.Toast.LENGTH_LONG).show()
        return
    }
    val prefs = context.getSharedPreferences("lockr_preferences", 0)
    val dark = when (prefs.getString("theme_mode", "system")) {
        "dark" -> true; "light" -> false
        else -> context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }
    val preview = RemoteViews(context.packageName, R.layout.stash_widget).apply {
        setImageViewBitmap(R.id.widget_art, widgetArtwork(context, documents, dark, "Tap to open Stash"))
    }
    val extras = Bundle().apply { putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, preview) }
    manager.requestPinAppWidget(ComponentName(context, if (documents) DocumentsWidget::class.java else WalletWidget::class.java), extras, null)
}
