# Mijo

Carnet de recettes personnel, hors ligne, avec un mode cuisine mains libres : l'appli lit chaque étape à voix haute et avance quand on dit « Suivant ».

## Installer la version de test

À chaque push sur `main`, GitHub Actions compile l'APK et le publie ici :
**https://github.com/franzk/mijo/releases/download/dev/mijo.apk**

Ouvre ce lien depuis le téléphone Android et installe le fichier (autorise l'installation depuis le navigateur si Android le demande). Les builds suivants s'installent par-dessus, sans perdre les données.

Au premier lancement du mode cuisine, l'appli demande l'accès au micro.

## Mode cuisine

| Dire | Effet |
|---|---|
| « Suivant » | étape suivante |
| « Précédent » | étape précédente |
| « Répète » | relit l'étape |

- Tout fonctionne sans réseau : la reconnaissance utilise [Vosk](https://alphacephei.com/vosk/) avec un modèle français embarqué, limité à ces trois commandes ; la lecture utilise la voix française du téléphone.
- L'écoute est coupée pendant la lecture, pour que l'appli ne s'entende pas elle-même.
- Prérequis : une voix française installée dans *Réglages › Synthèse vocale* (présente par défaut sur la plupart des téléphones).

## Architecture

- Kotlin + Compose Multiplatform : interface et logique dans `composeApp/src/commonMain`, prêtes pour iOS.
- Spécifique Android dans `androidMain` : moteur vocal (`VoskVoiceEngine`), activité, stockage.
- `keystore/dev.keystore` est une clé de **développement** publique (mot de passe `android`), uniquement pour que les builds de test se mettent à jour proprement. La clé de publication Play Store sera stockée dans les secrets GitHub, jamais dans le dépôt.

## Développer

```
./gradlew :composeApp:testDebugUnitTest :composeApp:assembleDebug
```

Le modèle vocal n'est pas versionné : la CI le télécharge dans `composeApp/src/androidMain/assets/model-fr` (voir `.github/workflows/android.yml`).
