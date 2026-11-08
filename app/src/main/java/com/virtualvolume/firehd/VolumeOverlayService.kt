package com.virtualvolume.firehd

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class VolumeOverlayService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var audio: AudioManager
    private var bubble: TextView? = null
    private var panel: LinearLayout? = null
    private var panelParams: WindowManager.LayoutParams? = null
    private var expanded = false
    private var bubbleParams: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        audio = getSystemService(AUDIO_SERVICE) as AudioManager
        showBubble()
    }

    private fun params(width: Int, height: Int) = WindowManager.LayoutParams(
        width, height,
        if (android.os.Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.END; x = dp(14); y = dp(180) }

    private fun showBubble() {
        if (bubble != null) return
        val b = TextView(this).apply {
            text = "VOL"; textSize = 13f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            background = shape(Color.rgb(35, 184, 145), 100)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setOnClickListener { togglePanel() }
            setOnTouchListener(object : View.OnTouchListener {
                var downX = 0f; var downY = 0f; var startX = 0; var startY = 0; var moved = false
                override fun onTouch(v: View, e: MotionEvent): Boolean {
                    val p = bubbleParams ?: return false
                    when (e.action) {
                        MotionEvent.ACTION_DOWN -> { downX=e.rawX; downY=e.rawY; startX=p.x; startY=p.y; moved=false; return true }
                        MotionEvent.ACTION_MOVE -> {
                            val dx=(e.rawX-downX).toInt(); val dy=(e.rawY-downY).toInt()
                            if (kotlin.math.abs(dx)>dp(5) || kotlin.math.abs(dy)>dp(5)) moved=true
                            p.x=(startX-dx).coerceAtLeast(0); p.y=(startY+dy).coerceAtLeast(0)
                            try { wm.updateViewLayout(v, p) } catch (_: Exception) {}
                            return true
                        }
                        MotionEvent.ACTION_UP -> { if (!moved) togglePanel(); return true }
                    }
                    return false
                }
            })
        }
        bubble = b
        bubbleParams = params(dp(58), dp(52))
        try { wm.addView(b, bubbleParams) } catch (_: Exception) { bubble = null; stopSelf() }
    }

    private fun togglePanel() { if (expanded) hidePanel() else showPanel() }

    private fun showPanel() {
        if (panel != null) { expanded=true; panel?.visibility=View.VISIBLE; return }
        val root = LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL; setPadding(dp(16), dp(14), dp(16), dp(12))
            background=shape(Color.rgb(20,29,38), 22)
        }
        val title = TextView(this).apply { text="MEDIA VOLUME"; textSize=13f; setTextColor(Color.rgb(88,214,176)); typeface=Typeface.DEFAULT_BOLD }
        root.addView(title)
        val value = TextView(this).apply { textSize=22f; setTextColor(Color.WHITE); setPadding(0, dp(6), 0, dp(4)) }
        root.addView(value)
        val max = maxOf(1, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
        val seek = SeekBar(this).apply {
            this.max=max; progress=audio.getStreamVolume(AudioManager.STREAM_MUSIC)
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) audio.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                    val current=audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                    value.text="${current*100/max}%"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        value.text="${seek.progress*100/max}%"
        root.addView(seek, LinearLayout.LayoutParams(dp(220), dp(48)))
        val buttons=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
        fun smallButton(text: String, action: () -> Unit) = TextView(this).apply {
            this.text=text; textSize=13f; setTextColor(Color.WHITE); gravity=Gravity.CENTER
            background=shape(Color.rgb(48,63,75), 12); setPadding(dp(12), dp(10), dp(12), dp(10))
            setOnClickListener { action(); val v=audio.getStreamVolume(AudioManager.STREAM_MUSIC); seek.progress=v; value.text="${v*100/max}%" }
        }
        buttons.addView(smallButton("Mute", { audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0) }))
        val gap=View(this); buttons.addView(gap, LinearLayout.LayoutParams(dp(8), 1))
        buttons.addView(smallButton("Close", { hidePanel() }))
        root.addView(buttons)
        panel=root
        panelParams=params(dp(260), WindowManager.LayoutParams.WRAP_CONTENT).apply { x=dp(14); y=dp(240) }
        try { wm.addView(root, panelParams); expanded=true } catch (_: Exception) { panel=null; stopSelf() }
    }

    private fun hidePanel() { panel?.let { try { wm.removeView(it) } catch (_: Exception) {} }; panel=null; panelParams=null; expanded=false }
    private fun shape(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius=dp(radius).toFloat() }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        hidePanel()
        bubble?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        bubble=null
        super.onDestroy()
    }
}
