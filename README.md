# Al Dokkan · الدكان

Ma boutique d'apps, rien qu'à moi : elle liste mes applications Android, les installe et les met à jour
depuis leurs versions publiées (« releases ») sur GitHub.

- Kotlin + Jetpack Compose, minSdk 26.
- Les dépôts étant privés, un jeton GitHub en lecture seule se colle dans **Réglages**. Il reste sur le téléphone.
- Une mise à jour s'installe par-dessus la version actuelle : la signature de l'APK est toujours la même
  (clé fixe `app/al-dokkan.keystore`), donc aucune donnée n'est perdue.
- L'APK est compilé par GitHub Actions (`.github/workflows/build-apk.yml`) et publié dans les Releases.

Ajouter une app à la boutique : une ligne dans `Catalog.kt` (dépôt GitHub + identifiant de l'app).

## Trois APK

- **Clients mobile** et **Clients TV** (`al-dokkan-clients.apk`, `al-dokkan-clients-tv.apk`) : publics, ils affichent seulement les apps choisies dans l'admin.
- **Admin mobile** : un seul écran, ouvert par empreinte ; pour chaque app, deux interrupteurs (mobile / TV), publiés tout de suite sur le serveur (`server/`, Vercel + Neon).
  Aucune clé dans l'APK : au premier lancement, l'appareil enregistre sur le serveur sa propre clé (Keystore, non exportable) ; ensuite chaque publication est signée et horodatée. L'APK admin reste en brouillon privé.
