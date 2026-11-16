# Al Dokkan · الدكان

Ma boutique d'apps, rien qu'à moi : elle liste mes applications Android, les installe et les met à jour
depuis leurs versions publiées (« releases ») sur GitHub.

- Kotlin + Jetpack Compose, minSdk 26.
- Les dépôts étant privés, un jeton GitHub en lecture seule se colle dans **Réglages**. Il reste sur le téléphone.
- Une mise à jour s'installe par-dessus la version actuelle : la signature de l'APK est toujours la même
  (clé fixe `app/al-dokkan.keystore`), donc aucune donnée n'est perdue.
- L'APK est compilé par GitHub Actions (`.github/workflows/build-apk.yml`) et publié dans les Releases.

Ajouter une app à la boutique : une ligne dans `Catalog.kt` (dépôt GitHub + identifiant de l'app).
