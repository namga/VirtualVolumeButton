package com.virtualvolume.firehd

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.content.Context
import android.media.AudioManager
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var audio: AudioManager
    private lateinit var volumeLabel: TextView
    private lateinit var slider: SeekBar
    private val prefs by lazy { getSharedPreferences("virtual_volume", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audio = getSystemService(AUDIO_SERVICE) as AudioManager
        render()
    }

    override fun onResume() {
        super.onResume()
        if (::slider.isInitialized) syncVolume()
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(24))
            setBackgroundColor(Color.rgb(16, 24, 32))
        }
        fun label(text: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false) = TextView(this).apply {
            this.text = text; textSize = size; setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }
        root.addView(label("VIRTUAL VOLUME", 13f, Color.rgb(88,214,176), true))
        root.addView(label("Floating controls for Fire HD 10", 23f, Color.WHITE, true).apply {
            setPadding(0, dp(8), 0, dp(8))
        })
        root.addView(label("Adjust media volume without physical buttons. The floating panel can be used over other apps.", 15f, Color.LTGRAY))
        volumeLabel = label("Media volume", 17f, Color.WHITE, true).apply { setPadding(0, dp(28), 0, dp(4)) }
        root.addView(volumeLabel)
        slider = SeekBar(this).apply {
            max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            progress = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) audio.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                    updateLabel()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        root.addView(slider, LinearLayout.LayoutParams(-1, dp(54)))
        root.addView(label("Fine control uses the volume steps exposed by Fire OS.", 12f, Color.LTGRAY))
        val mute = Button(this).apply { text = "Mute media"; setOnClickListener {
            if (audio.getStreamVolume(AudioManager.STREAM_MUSIC) > 0) {
                prefs.edit().putInt("last_volume", audio.getStreamVolume(AudioManager.STREAM_MUSIC)).apply()
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
            } else {
                val restore = prefs.getInt("last_volume", maxOf(1, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2))
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, restore.coerceIn(0, slider.max), 0)
            }
            syncVolume()
        } }
        root.addView(mute, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(12) })
        val overlay = Button(this).apply { text = "Grant overlay permission"; setOnClickListener {
            if (!Settings.canDrawOverlays(this@MainActivity)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            } else startService(Intent(this@MainActivity, VolumeOverlayService::class.java))
        } }
        root.addView(overlay, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(8) })
        root.addView(label("After granting permission, return here and tap ‘Start floating control’.", 12f, Color.LTGRAY).apply { setPadding(0, dp(6), 0, 0) })
        val start = Button(this).apply { text = "Start floating control"; setOnClickListener {
            if (Settings.canDrawOverlays(this@MainActivity)) startService(Intent(this@MainActivity, VolumeOverlayService::class.java))
            else startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } }
        root.addView(start, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(8) })
        val stop = Button(this).apply { text = "Stop floating control"; setOnClickListener { stopService(Intent(this@MainActivity, VolumeOverlayService::class.java)) } }
        root.addView(stop, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(8) })
        root.addView(label("Limitations", 17f, Color.rgb(88,214,176), true).apply { setPadding(0, dp(26), 0, dp(6)) })
        root.addView(label("This app changes normal media volume. It cannot guarantee amplification beyond the tablet’s system or hardware limit. Fire OS may stop floating controls when the app is closed or the device restarts.", 13f, Color.LTGRAY))
        setContentView(root)
        updateLabel()
    }

    private fun syncVolume() {
        val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (slider.progress != current) slider.progress = current
        updateLabel()
    }
    private fun updateLabel() {
        if (!::slider.isInitialized || !::volumeLabel.isInitialized) return
        val max = maxOf(1, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
        val value = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        volumeLabel.text = "Media volume  •  ${value * 100 / max}%  ($value/$max)"
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
