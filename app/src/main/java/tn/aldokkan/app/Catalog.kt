package tn.aldokkan.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class Platform(val label: String) { Mobile("Mobile"), Tv("TV") }

/** Une app de la boutique : où trouver ses versions et comment la reconnaître sur l'appareil. */
data class AppEntry(
    val id: String,
    val name: String,
    val tagline: String,
    val description: String,
    val category: String,
    val repo: String,
    val pkg: String,
    val bg: Color,
    val fg: Color,
    val glyph: List<String>,
    val apkHint: String? = null,
    val platform: Platform = Platform.Mobile,
    /** Vraie icône de l'app : fond + premier plan (adaptive icon) ou une image toute faite. */
    val iconBg: Int? = null,
    val iconFg: Int? = null,
    val iconImg: Int? = null,
)

object Glyphs {
    val box = listOf("M26 40l22-12 22 12v22L48 74 26 62Z", "M26 40l22 12 22-12", "M48 52v22")
    val book = listOf("M48 34c-8-6-18-6-24-4v34c6-2 16-2 24 4 8-6 18-6 24-4V30c-6-2-16-2-24 4Z", "M48 34v34")
    val scale = listOf("M48 24v46", "M34 72h28", "M22 36h52", "M22 36l-8 20h16Z", "M74 36l-8 20h16Z")
    val calc = listOf("M34 22h28a8 8 0 0 1 8 8v36a8 8 0 0 1-8 8H34a8 8 0 0 1-8-8V30a8 8 0 0 1 8-8Z", "M36 38h24", "M36 52h4", "M48 52h4", "M36 62h4", "M48 62h4")
    val wifi = listOf("M22 44a38 38 0 0 1 52 0", "M32 54a24 24 0 0 1 32 0", "M41 64a11 11 0 0 1 14 0", "M48 74h0")
    val tv = listOf("M24 30h48v30H24Z", "M40 72h16", "M48 60v12", "M32 40h20", "M32 50h14")
    val cup = listOf("M28 36h34v18a14 14 0 0 1-14 14h-6a14 14 0 0 1-14-14Z", "M62 40h6a6 6 0 0 1 0 12h-6", "M32 76h26")
    val store = listOf("M22 34h52", "M26 34l4 12h36l4-12", "M30 46v26h36V46", "M42 72V58h12v14")
}

object Catalog {
    private val esp = Color(0xFF2B1A12)
    private val esp2 = Color(0xFF3D2619)
    private val cream = Color(0xFFFFF8EE)
    private val saffron = Color(0xFFF2A33A)
    private val sage = Color(0xFF58A97D)
    private val sand = Color(0xFFF6E7CF)

    /** Les apps affichées : celles de la plateforme choisie (Mobile ou TV). */
    var platform by mutableStateOf(if (BuildConfig.IS_TV) Platform.Tv else Platform.Mobile)

    /** Identifiants des apps abouties : celles qui ont une version publiée. */
    private var published by mutableStateOf(setOf<String>())

    fun setPublished(id: String, ok: Boolean) {
        published = if (ok) published + id else published - id
    }

    /** Seules les apps abouties de la plateforme choisie sont affichées. */
    val apps: List<AppEntry> get() = all.filter { it.id in published }

    val all = listOf(
        AppEntry(
            id = "aldokkan", name = "Al Dokkan", tagline = "Ma boutique d'apps",
            description = "La boutique : elle installe tes apps et les met à jour par-dessus la version actuelle.",
            category = "Perso", repo = "daly64/Al-Dokkan", pkg = "tn.aldokkan.app",
            bg = Color(0xFFB8441F), fg = cream, glyph = Glyphs.store, apkHint = "al-dokkan.apk",
        ),
        AppEntry(
            id = "alwaraqa", name = "Al Waraqa", tagline = "Lecteur PDF",
            description = "Lis tes PDF avec des pages qui se tournent comme dans un livre.",
            category = "Lecture", repo = "daly64/Al-Waraqa", pkg = "tn.alwaraqa.app",
            bg = saffron, fg = esp, glyph = Glyphs.book, apkHint = "al-waraqa",
            iconBg = R.drawable.ic_app_alwaraqa_bg, iconFg = R.drawable.ic_app_alwaraqa_fg,
        ),
        AppEntry(
            id = "mizan", name = "Mizan", tagline = "Suivi de dépenses",
            description = "Suis tes dépenses personnelles, avec bilan par jour et par mois.",
            category = "Perso", repo = "daly64/Mizan", pkg = "tn.mizan.app",
            bg = esp2, fg = cream, glyph = Glyphs.scale, apkHint = "al-mizan",
            iconBg = R.drawable.ic_app_mizan_bg, iconFg = R.drawable.ic_app_mizan_fg,
        ),
        AppEntry(
            id = "costo", name = "Costo", tagline = "Coût de revient",
            description = "Calcule le coût de revient des boissons : ingrédients, recettes par portion, pertes, marge visée et prix conseillé.",
            category = "Gestion", repo = "daly64/Costo", pkg = "tn.costo",
            bg = Color(0xFF9CC8A4), fg = esp, glyph = Glyphs.calc,
            iconBg = R.drawable.ic_app_costo_bg, iconFg = R.drawable.ic_app_costo_fg,
        ),
        AppEntry(
            id = "fichiers", name = "Gestion de fichiers", tagline = "Fichiers du téléphone",
            description = "Parcours, range et gère les fichiers de ton téléphone.",
            category = "Perso", repo = "daly64/gestion-fichiers", pkg = "io.github.daly64.gestionfichiers",
            bg = Color(0xFF2D5BFF), fg = cream, glyph = Glyphs.box,
            iconBg = R.drawable.ic_app_fichiers_bg, iconFg = R.drawable.ic_app_fichiers_fg,
        ),
        AppEntry(
            id = "commandes", name = "Commandes", tagline = "Prise de commande",
            description = "Prends les commandes du salon depuis ton téléphone.",
            category = "Salon de thé", repo = "daly64/commandes-android", pkg = "fr.salon.commandes",
            bg = Color(0xFF2E6B5E), fg = cream, glyph = Glyphs.cup,
            iconBg = R.drawable.ic_app_commandes_bg, iconFg = R.drawable.ic_app_commandes_fg,
        ),
        AppEntry(
            id = "metrix", name = "Metrix", tagline = "Périmètre et aire",
            description = "Calcule le périmètre et l\'aire de tes figures, en centimètres ou en mètres.",
            category = "Perso", repo = "daly64/Metrix", pkg = "com.example.metrix",
            bg = sand, fg = esp, glyph = Glyphs.scale,
            iconImg = R.drawable.ic_app_metrix,
        ),
        AppEntry(
            id = "taskslist", name = "Tasks List", tagline = "Liste de tâches",
            description = "Note tes tâches et coche-les au fur et à mesure.",
            category = "Perso", repo = "daly64/TasksList", pkg = "com.alphadaly.taskslist",
            bg = sage, fg = esp, glyph = Glyphs.calc,
            iconImg = R.drawable.ic_app_taskslist,
        ),
        AppEntry(
            id = "receplio", name = "Receplio", tagline = "Recettes",
            description = "Garde tes recettes sous forme de cartes.",
            category = "Perso", repo = "daly64/Receplio", pkg = "com.example.receplio",
            bg = saffron, fg = esp, glyph = Glyphs.cup,
        ),
        AppEntry(
            id = "calctraitement", name = "Calculateur de traitement", tagline = "Sirop, boîtes, ampoules",
            description = "Calcule les quantités d\'un traitement : sirop, boîtes et ampoules.",
            category = "Perso", repo = "daly64/Calculateur_de_traitement", pkg = "com.example.calculateurdetraitement",
            bg = sage, fg = esp, glyph = Glyphs.calc,
        ),
    )

    val categories = listOf("Gestion", "Lecture", "Salon de thé", "Perso")

    fun byId(id: String?): AppEntry? = all.firstOrNull { it.id == id }
}
