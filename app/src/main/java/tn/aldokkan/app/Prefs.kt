package tn.aldokkan.app

import android.content.Context

/** Petits réglages gardés sur le téléphone. Rien ne part ailleurs que vers GitHub. */
class Prefs(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("dokkan", Context.MODE_PRIVATE)

    var onboarded: Boolean
        get() = sp.getBoolean("onboarded", false)
        set(v) = sp.edit().putBoolean("onboarded", v).apply()

    var token: String
        get() = sp.getString("token", "") ?: ""
        set(v) = sp.edit().putString("token", v.trim()).apply()

    var fullscreen: Boolean
        get() = sp.getBoolean("fullscreen", true)
        set(v) = sp.edit().putBoolean("fullscreen", v).apply()

    var wifiOnly: Boolean
        get() = sp.getBoolean("wifiOnly", true)
        set(v) = sp.edit().putBoolean("wifiOnly", v).apply()

    /** Empreinte de la version installée depuis la boutique (identifiant du fichier + date). */
    fun stamp(pkg: String): String? = sp.getString("stamp_$pkg", null)

    fun setStamp(pkg: String, stamp: String) = sp.edit().putString("stamp_$pkg", stamp).apply()
}
