package tn.aldokkan.app

import android.content.Context

/** Petits réglages gardés sur le téléphone. Rien ne quitte l'appareil, à part les demandes de téléchargement. */
class Prefs(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("dokkan", Context.MODE_PRIVATE)

    var onboarded: Boolean
        get() = sp.getBoolean("onboarded", false)
        set(v) = sp.edit().putBoolean("onboarded", v).apply()

    var fullscreen: Boolean
        get() = sp.getBoolean("fullscreen", true)
        set(v) = sp.edit().putBoolean("fullscreen", v).apply()

    var wifiOnly: Boolean
        get() = sp.getBoolean("wifiOnly", false)
        set(v) = sp.edit().putBoolean("wifiOnly", v).apply()

    /** Empreinte de la version installée depuis la boutique (identifiant du fichier + date). */
    fun stamp(pkg: String): String? = sp.getString("stamp_$pkg", null)

    fun setStamp(pkg: String, stamp: String) = sp.edit().putString("stamp_$pkg", stamp).apply()

    /** Dernière liste d'apps choisies pour les clients, gardée pour fonctionner sans connexion. */
    var clientIds: Set<String>?
        get() = sp.getString("clientIds", null)?.split(",")?.filter { it.isNotBlank() }?.toSet()
        set(v) = sp.edit().putString("clientIds", v?.joinToString(",")).apply()
}
