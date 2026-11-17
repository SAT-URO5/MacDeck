package com.macdeck.keyboard

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    private val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
    private val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT

    private lateinit var prefs: Prefs
    private lateinit var link: Link
    private lateinit var audio: AudioManager
    private val handler = Handler(Looper.getMainLooper())

    private val modState = hashMapOf(SH to 0, CT to 0, OP to 0, CM to 0, FNF to 0)
    private val modLastTap = hashMapOf<Int, Long>()
    private val keyViews = mutableListOf<Pair<K, TextView>>()

    private var statusDot: View? = null
    private var statusText: TextView? = null
    private var statusMsg = "Connecting…"
    private var connected = false
    private var inSettings = false

    // ---------- colors ----------
    private fun cBg() = if (prefs.dark) 0xFF111113.toInt() else 0xFFD2D4DA.toInt()
    private fun cKey() = if (prefs.dark) 0xFF2B2B2E.toInt() else 0xFFF7F7F9.toInt()
    private fun cKeyDown() = if (prefs.dark) 0xFF58585D.toInt() else 0xFFBEBFC5.toInt()
    private fun cText() = if (prefs.dark) 0xFFF2F2F7.toInt() else 0xFF1C1C1E.toInt()
    private val cAccent = 0xFF0A84FF.toInt()
    private val cLock = 0xFF5E5CE6.toInt()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun lp(w: Int, h: Int, wt: Float = 0f) = LinearLayout.LayoutParams(w, h, wt)
    private fun keyLp(w: Int, h: Int, wt: Float) =
        lp(w, h, wt).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) }

    // ---------- lifecycle ----------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        audio = getSystemService(AUDIO_SERVICE) as AudioManager
        link = Link { msg, ok ->
            runOnUiThread {
                statusMsg = msg
                connected = ok
                updateStatus()
            }
        }
        showKeyboard()
        startLink()
    }

    override fun onDestroy() {
        link.stop()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (inSettings) leaveSettings() else super.onBackPressed()
    }

    private fun startLink() {
        val host = if (prefs.usb) "127.0.0.1" else prefs.host.trim()
        link.start(host, prefs.port, prefs.pin)
    }

    // ---------- keyboard screen ----------
    private fun showKeyboard() {
        inSettings = false
        keyViews.clear()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cBg())
            setPadding(dp(6), dp(4), dp(6), dp(6))
        }
        root.addView(topBar(), lp(MATCH, dp(44)))

        val kb = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(kb, lp(MATCH, 0, 1f))
        Keys.rows.forEachIndexed { ri, row ->
            val rl = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.forEach { k ->
                if (k.label == "ARROWS") rl.addView(arrows(), lp(0, MATCH, k.w))
                else rl.addView(makeKey(k), keyLp(0, MATCH, k.w))
            }
            kb.addView(rl, lp(MATCH, 0, if (ri == 0) 0.7f else 1f))
        }
        setContentView(root)

        if (prefs.keepOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        refreshLabels()
        updateStatus()
    }

    private fun topBar(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val dot = View(this)
        dot.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.RED) }
        statusDot = dot
        bar.addView(dot, lp(dp(12), dp(12)).apply { setMargins(dp(10), 0, dp(10), 0) })

        val st = TextView(this).apply { setTextColor(cText()); textSize = 14f }
        statusText = st
        bar.addView(st, lp(0, WRAP, 1f))

        bar.addView(pill(if (prefs.arabic) "ع  AR" else "EN") { toggleLang() },
            lp(WRAP, MATCH).apply { setMargins(dp(4), dp(3), dp(4), dp(3)) })
        bar.addView(pill("⚙") { showSettings() },
            lp(WRAP, MATCH).apply { setMargins(dp(4), dp(3), dp(4), dp(3)) })
        return bar
    }

    private fun pill(text: String, onClick: () -> Unit): TextView = TextView(this).apply {
        this.text = text
        textSize = 16f
        gravity = Gravity.CENTER
        setTextColor(cText())
        setPadding(dp(18), 0, dp(18), 0)
        background = GradientDrawable().apply { setColor(cKey()); cornerRadius = dp(10).toFloat() }
        setOnClickListener { onClick() }
    }

    private fun updateStatus() {
        statusText?.text = statusMsg
        (statusDot?.background as? GradientDrawable)
            ?.setColor(if (connected) 0xFF30D158.toInt() else 0xFFFF9F0A.toInt())
    }

    private fun arrows(): View {
        val box = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        box.addView(makeKey(Keys.LEFT), keyLp(0, MATCH, 1f))
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(makeKey(Keys.UP), keyLp(MATCH, 0, 1f))
        col.addView(makeKey(Keys.DOWN), keyLp(MATCH, 0, 1f))
        box.addView(col, lp(0, MATCH, 1f))
        box.addView(makeKey(Keys.RIGHT), keyLp(0, MATCH, 1f))
        return box
    }

    private fun makeKey(k: K): TextView {
        val tv = TextView(this)
        tv.gravity = Gravity.CENTER
        tv.maxLines = 1
        tv.setPadding(dp(2), 0, dp(2), 0)
        tv.setAutoSizeTextTypeUniformWithConfiguration(
            8, if (k.small) 15 else 28, 1, TypedValue.COMPLEX_UNIT_SP
        )
        keyViews.add(k to tv)
        tv.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { onDown(k, tv); true }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { onUp(k, tv); true }
                else -> true
            }
        }
        return tv
    }

    // ---------- key logic ----------
    private fun modValue(k: K): Int = when {
        k.mod == CAPS -> if ((modState[SH] ?: 0) == 2) 2 else 0
        k.mod != 0 -> modState[k.mod] ?: 0
        else -> 0
    }

    private fun paint(tv: TextView, k: K, down: Boolean) {
        val st = modValue(k)
        val bg = when {
            down -> cKeyDown()
            st == 2 -> cLock
            st == 1 -> cAccent
            else -> cKey()
        }
        tv.background = GradientDrawable().apply { setColor(bg); cornerRadius = dp(8).toFloat() }
        tv.setTextColor(if (st > 0) Color.WHITE else cText())
    }

    private fun refreshLabels() {
        val shiftOn = (modState[SH] ?: 0) > 0
        for ((k, tv) in keyViews) {
            tv.text = when {
                prefs.arabic && k.ar != null -> k.ar
                shiftOn && k.shift != null -> k.shift
                else -> k.label
            }
            paint(tv, k, false)
        }
    }

    private fun currentFlags(): Int {
        var f = 0
        for ((m, s) in modState) if (s > 0) f = f or m
        return f
    }

    private fun send(code: Int, flags: Int) = link.send("K $code $flags")

    private fun feedback(k: K, tv: View) {
        if (prefs.sound) {
            val fx = when (k.code) {
                49 -> AudioManager.FX_KEYPRESS_SPACEBAR
                36 -> AudioManager.FX_KEYPRESS_RETURN
                51 -> AudioManager.FX_KEYPRESS_DELETE
                else -> AudioManager.FX_KEYPRESS_STANDARD
            }
            audio.playSoundEffect(fx, prefs.volume / 100f)
        }
        if (prefs.haptic) tv.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    private fun onDown(k: K, tv: TextView) {
        feedback(k, tv)
        paint(tv, k, true)
        if (k.mod != 0) { toggleMod(k); return }
        send(k.code, currentFlags())
        var changed = false
        for (m in modState.keys.toList()) if (modState[m] == 1) { modState[m] = 0; changed = true }
        if (changed) refreshLabels()
        if (k.repeat) {
            val r = object : Runnable {
                override fun run() {
                    send(k.code, currentFlags())
                    handler.postDelayed(this, 45)
                }
            }
            tv.tag = r
            handler.postDelayed(r, 380)
        }
    }

    private fun onUp(k: K, tv: TextView) {
        (tv.tag as? Runnable)?.let { handler.removeCallbacks(it); tv.tag = null }
        paint(tv, k, false)
    }

    private fun toggleMod(k: K) {
        if (k.mod == CAPS) {
            modState[SH] = if (modState[SH] == 2) 0 else 2
        } else {
            val m = k.mod
            val s = modState[m] ?: 0
            val now = SystemClock.uptimeMillis()
            modState[m] = when {
                s == 0 -> 1
                s == 1 && now - (modLastTap[m] ?: 0L) < 500 -> 2
                else -> 0
            }
            modLastTap[m] = now
        }
        refreshLabels()
    }

    private fun toggleLang() {
        prefs.arabic = !prefs.arabic
        if (prefs.syncLang) send(49, if (prefs.langOption) OP else CT)
        showKeyboard()
    }

    // ---------- settings screen ----------
    private fun showSettings() {
        inSettings = true
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(32), dp(12), dp(32), dp(32))
        }

        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(TextView(this).apply { text = "Settings"; textSize = 24f; setTextColor(cText()) }, lp(0, WRAP, 1f))
        head.addView(pill("Done") { leaveSettings() }, lp(WRAP, dp(44)))
        col.addView(head)

        col.addView(section("Connection"))
        col.addView(radios(listOf("USB cable (adb on the Mac)", "Wi-Fi"), if (prefs.usb) 0 else 1) { prefs.usb = (it == 0) })
        col.addView(edit(prefs.host, "Mac IP (Wi-Fi only)", false) { prefs.host = it.trim() })
        col.addView(edit(prefs.port.toString(), "Port", true) { prefs.port = it.toIntOrNull() ?: 8765 })
        col.addView(edit(prefs.pin, "PIN (same as pin.txt on the Mac)", false) { prefs.pin = it.filter { c -> !c.isWhitespace() } })

        col.addView(section("Look"))
        col.addView(sw("Black keyboard (off = white)", prefs.dark) { prefs.dark = it })

        col.addView(section("Sound and touch"))
        col.addView(sw("Key click sound", prefs.sound) { prefs.sound = it })
        col.addView(label("Sound volume"))
        col.addView(SeekBar(this).apply {
            max = 100
            progress = prefs.volume
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) { prefs.volume = p }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {
                    audio.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, prefs.volume / 100f)
                }
            })
        })
        col.addView(sw("Vibration on key press", prefs.haptic) { prefs.haptic = it })

        col.addView(section("Language"))
        col.addView(label("Shortcut that switches language on the Mac"))
        col.addView(radios(listOf("Option + Space", "Control + Space"), if (prefs.langOption) 0 else 1) { prefs.langOption = (it == 0) })
        col.addView(sw("Send the shortcut to the Mac when I tap EN / ع", prefs.syncLang) { prefs.syncLang = it })

        col.addView(section("Screen"))
        col.addView(sw("Keep tablet screen on", prefs.keepOn) { prefs.keepOn = it })

        col.addView(TextView(this).apply {
            text = "Tip: Arabic labels only change what is printed on the keys. The Mac types in whatever input source it is set to."
            setTextColor(0xFF8E8E93.toInt()); textSize = 13f; setPadding(0, dp(18), 0, 0)
        })

        setContentView(ScrollView(this).apply { setBackgroundColor(cBg()); addView(col) })
    }

    private fun leaveSettings() {
        startLink()
        showKeyboard()
    }

    private fun section(t: String) = TextView(this).apply {
        text = t.uppercase(); textSize = 13f; setTextColor(cAccent); setPadding(0, dp(22), 0, dp(6))
    }

    private fun label(t: String) = TextView(this).apply {
        text = t; textSize = 16f; setTextColor(cText()); setPadding(0, dp(6), 0, dp(2))
    }

    private fun sw(text: String, init: Boolean, on: (Boolean) -> Unit) = Switch(this).apply {
        this.text = text; textSize = 16f; setTextColor(cText()); isChecked = init
        setPadding(0, dp(10), 0, dp(10))
        setOnCheckedChangeListener { _, b -> on(b) }
    }

    private fun radios(items: List<String>, selected: Int, on: (Int) -> Unit): RadioGroup {
        val rg = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        items.forEachIndexed { i, t ->
            rg.addView(RadioButton(this).apply {
                id = 100 + i; text = t; textSize = 16f; setTextColor(cText())
            })
        }
        rg.check(100 + selected)
        rg.setOnCheckedChangeListener { _, id -> on(id - 100) }
        return rg
    }

    private fun edit(init: String, hint: String, numeric: Boolean, on: (String) -> Unit) = EditText(this).apply {
        setText(init)
        this.hint = hint
        setTextColor(cText())
        setHintTextColor(0xFF8E8E93.toInt())
        inputType = if (numeric) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT
        setSingleLine()
        addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { on(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
    }
}
