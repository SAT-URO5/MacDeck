package com.macdeck.keyboard

import android.content.Context
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("macdeck", Context.MODE_PRIVATE)

    private fun bool(k: String, d: Boolean) = object : ReadWriteProperty<Any?, Boolean> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) = sp.getBoolean(k, d)
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
            sp.edit().putBoolean(k, value).apply()
        }
    }

    private fun int(k: String, d: Int) = object : ReadWriteProperty<Any?, Int> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) = sp.getInt(k, d)
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
            sp.edit().putInt(k, value).apply()
        }
    }

    private fun str(k: String, d: String) = object : ReadWriteProperty<Any?, String> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): String = sp.getString(k, d) ?: d
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
            sp.edit().putString(k, value).apply()
        }
    }

    var usb by bool("usb", true)            // true = USB (adb reverse), false = Wi-Fi
    var host by str("host", "192.168.1.10")  // Mac IP for Wi-Fi
    var port by int("port", 8765)
    var pin by str("pin", "1234")
    var dark by bool("dark", true)           // black keyboard
    var sound by bool("sound", true)
    var volume by int("volume", 60)
    var haptic by bool("haptic", true)
    var arabic by bool("arabic", false)      // Arabic key labels
    var syncLang by bool("syncLang", true)   // send language shortcut to Mac when toggling
    var langOption by bool("langOption", true) // true = Option+Space, false = Ctrl+Space
    var keepOn by bool("keepOn", true)
}
