package com.example.hifienhancer

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var eq: Equalizer? = null
    private var bass: BassBoost? = null
    private var virt: Virtualizer? = null
    private var loud: LoudnessEnhancer? = null

    private val bandBars = ArrayList<SeekBar>()
    private lateinit var bassBar: SeekBar
    private lateinit var virtBar: SeekBar
    private lateinit var loudBar: SeekBar
    private var hiFiMode = false
    private var current: Preset = Presets.all[0]

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        setContentView(ScrollView(this).apply { addView(root) })

        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val rate = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE) ?: "?"
        val buf = am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER) ?: "?"
        root.addView(TextView(this).apply {
            text = "Device output: $rate Hz, buffer $buf frames\n${Presets.all.size} presets"
            textSize = 14f
        })

        try {
            eq = Equalizer(0, 0)
            bass = BassBoost(0, 0)
            virt = Virtualizer(0, 0)
            loud = LoudnessEnhancer(0)
        } catch (e: Exception) {
            Toast.makeText(this, "Effects not supported: ${e.message}", Toast.LENGTH_LONG).show()
        }
        setAll(true)

        // Module 1: master switch
        val master = Switch(this).apply { text = "Enhancer ON"; isChecked = true }
        master.setOnCheckedChangeListener { _, on -> setAll(on) }
        root.addView(master)

        // Module 2: Hi-Fi mode (pure sound: no 3D, no loudness, no bass boost)
        val hifi = Switch(this).apply { text = "Hi-Fi mode (EQ only, no coloring)" }
        hifi.setOnCheckedChangeListener { _, on -> hiFiMode = on; applyPreset(current) }
        root.addView(hifi)

        // Module 3: 67 presets
        root.addView(label("Preset"))
        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Presets.all.map { it.name })
        root.addView(spinner)

        // Module 4: manual EQ
        val e = eq
        val min = e?.bandLevelRange?.get(0)?.toInt() ?: -1500
        val max = e?.bandLevelRange?.get(1)?.toInt() ?: 1500
        for (b in 0 until (e?.numberOfBands?.toInt() ?: 0)) {
            val hz = e!!.getCenterFreq(b.toShort()) / 1000
            root.addView(label(if (hz >= 1000) "${hz / 1000} kHz" else "$hz Hz"))
            val sb = SeekBar(this).apply {
                this.max = max - min
                progress = e.getBandLevel(b.toShort()) - min
                setOnSeekBarChangeListener(listener { p -> e.setBandLevel(b.toShort(), (p + min).toShort()) })
            }
            bandBars.add(sb); root.addView(sb)
        }

        // Module 5: bass, 3D, loudness
        root.addView(label("Bass Boost"))
        bassBar = SeekBar(this).apply {
            max = 1000
            setOnSeekBarChangeListener(listener { p -> bass?.setStrength(p.toShort()) })
        }
        root.addView(bassBar)

        root.addView(label("3D Surround"))
        virtBar = SeekBar(this).apply {
            max = 1000
            setOnSeekBarChangeListener(listener { p -> virt?.setStrength(p.toShort()) })
        }
        root.addView(virtBar)

        root.addView(label("Loudness Boost"))
        loudBar = SeekBar(this).apply {
            max = 1500
            setOnSeekBarChangeListener(listener { p -> loud?.setTargetGain(p) })
        }
        root.addView(loudBar)

        // Module 6: reset
        root.addView(Button(this).apply {
            text = "Reset to flat"
            setOnClickListener { spinner.setSelection(0); applyPreset(Presets.all[0]) }
        })

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                current = Presets.all[pos]; applyPreset(current)
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun setAll(on: Boolean) {
        try { eq?.enabled = on; bass?.enabled = on; virt?.enabled = on; loud?.enabled = on } catch (_: Exception) {}
    }

    private fun applyPreset(p: Preset) {
        val e = eq
        if (e != null) {
            val min = e.bandLevelRange[0].toInt()
            val max = e.bandLevelRange[1].toInt()
            val n = e.numberOfBands.toInt()
            for (b in 0 until n) {
                val mb = (Presets.bandGain(p.eq, b, n) * 100).toInt().coerceIn(min, max)
                try { e.setBandLevel(b.toShort(), mb.toShort()) } catch (_: Exception) {}
                bandBars[b].progress = mb - min
            }
        }
        val bs = if (hiFiMode) 0 else p.bass
        val vs = if (hiFiMode) 0 else p.virt
        val ls = if (hiFiMode) 0 else p.loud
        bassBar.progress = bs; virtBar.progress = vs; loudBar.progress = ls
        try { bass?.setStrength(bs.toShort()) } catch (_: Exception) {}
        try { virt?.setStrength(vs.toShort()) } catch (_: Exception) {}
        try { loud?.setTargetGain(ls) } catch (_: Exception) {}
    }

    private fun label(t: String) = TextView(this).apply { text = t; setPadding(0, dp(16), 0, 0) }

    private fun listener(onChange: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
            if (fromUser) try { onChange(p) } catch (_: Exception) {}
        }
        override fun onStartTrackingTouch(s: SeekBar?) {}
        override fun onStopTrackingTouch(s: SeekBar?) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        eq?.release(); bass?.release(); virt?.release(); loud?.release()
    }
}
