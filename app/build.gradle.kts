plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Le numéro de version vient de la compilation GitHub (numéro d'exécution).
// Il augmente à chaque version : l'APK s'installe par-dessus la précédente.
val dokkanVersionCode = (project.findProperty("dokkanVersionCode") as String?)?.toIntOrNull() ?: 1

android {
    namespace = "tn.aldokkan.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "tn.aldokkan.app"
        minSdk = 26
        targetSdk = 35
        versionCode = dokkanVersionCode
        versionName = "1.0.$dokkanVersionCode"
    }

    // Deux dimensions :
    //  - « role »   : admin (gère la boutique) ou client (boutique simple pour les clients) ;
    //  - « device » : mobile (téléphone) ou tv (téléviseur, télécommande).
    // L'admin n'existe qu'en mobile : 3 APK (admin mobile, clients mobile, clients TV).
    // Identifiants : admin mobile = tn.aldokkan.app, admin TV = tn.aldokkan.tv (inchangés, donc les
    // installations actuelles se mettent à jour), clients = les mêmes avec le suffixe « .client ».
    flavorDimensions += listOf("role", "device")
    productFlavors {
        create("admin") {
            dimension = "role"
            buildConfigField("boolean", "IS_ADMIN", "true")
        }
        create("client") {
            dimension = "role"
            applicationIdSuffix = ".client"
            buildConfigField("boolean", "IS_ADMIN", "false")
        }
        create("mobile") {
            dimension = "device"
            buildConfigField("boolean", "IS_TV", "false")
        }
        create("tv") {
            dimension = "device"
            applicationId = "tn.aldokkan.tv"
            buildConfigField("boolean", "IS_TV", "true")
        }
    }

    // Clé fixe gardée dans le dépôt : toutes les versions ont la même signature,
    // donc une nouvelle version s'installe par-dessus l'ancienne, sans perdre les données.
    signingConfigs {
        getByName("debug") {
            storeFile = file("al-dokkan.keystore")
            storePassword = "aldokkan"
            keyAlias = "aldokkan"
            keyPassword = "aldokkan"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

androidComponents {
    // Trois APK seulement : pas d'admin TV.
    beforeVariants { v ->
        val f = v.productFlavors.map { it.second }
        if ("admin" in f && "tv" in f) v.enable = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
}
