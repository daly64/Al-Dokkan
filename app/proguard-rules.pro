# Minification R8 activée pour réduire la taille des APK.
# Le travail de vérification des mises à jour est créé par WorkManager par son nom de classe.
-keep class tn.aldokkan.app.UpdateWorker { <init>(...); }
-keep class tn.aldokkan.app.InstallReceiver { <init>(); }
