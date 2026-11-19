# CallRecorder Helper

> **English summary** — Companion app of [CallRecorder](https://play.google.com/store/apps/details?id=com.lcsgi.callrecorder).
> It detects phone calls, records them through an accessibility service (the only way to use the microphone during
> a call on Android 10+ without root) and hands each recording to the CallRecorder app on the same phone.
> **No internet permission**: recordings never leave the device. Open source so that anyone can check it.

Application compagnon de **CallRecorder** : elle détecte les appels, les enregistre et remet chaque enregistrement à
l'application CallRecorder du même téléphone, où ils s'écoutent, se recherchent et se classent.

## Pourquoi une application à part ?

Depuis Android 10, une application ordinaire ne peut pas utiliser le micro pendant un appel. Seul un **service
d'accessibilité** actif lève cette restriction, et Google Play n'autorise plus cet usage. L'enregistrement est donc
fait par ce Helper, installé à part, et CallRecorder reste sur le Play Store.

## Vie privée

C'est l'application qui a les permissions sensibles (micro, téléphone, journal d'appels, contacts, accessibilité) :
son code est public pour que chacun puisse le vérifier.

- **Aucune permission Internet** ([`AndroidManifest.xml`](helper/src/main/AndroidManifest.xml)) : le Helper ne peut
  rien envoyer, à personne.
- Le service d'accessibilité **ne lit rien à l'écran** (`canRetrieveWindowContent="false"`,
  [`accessibility_service.xml`](helper/src/main/res/xml/accessibility_service.xml)).
- Les enregistrements ne sont remis qu'à l'application CallRecorder **signée par son auteur** : le certificat de
  signature est vérifié avant chaque échange ([`Signatures.kt`](core/src/main/java/com/callrecorder/core/Signatures.kt)).
  Une application imitant CallRecorder ne reçoit rien.

## Installation

1. Installer l'application **CallRecorder** depuis Google Play.
2. Télécharger l'APK du Helper dans les [Releases](../../releases/latest) et l'installer.
3. Ouvrir le Helper et suivre les étapes : autorisations, service d'accessibilité, batterie.

Sur Android 13+, l'option d'accessibilité est grisée pour une application installée hors Play Store :
fiche de l'application → ⋮ → **Autoriser les paramètres restreints**.

### Le correspondant n'est pas enregistré ?

Android ne donne pas le son de l'appel aux applications (permission réservée au système) : le correspondant est
capté par le micro, depuis l'écouteur. Dans CallRecorder → Réglages → **Voix du correspondant**, activer le
haut-parleur automatique, ou monter le volume et essayer les sources « Micro brut » et « Micro vidéo ».

## Fonctionnement

| Fichier | Rôle |
|---|---|
| `helper/…/CallAccessibilityService.kt` | Service d'accessibilité : garde le Helper actif, écoute l'état des appels |
| `core/…/CallStateTracker.kt` | Sonnerie / décroché / raccroché → début, fin et sens de l'appel |
| `helper/…/CallSession.kt`, `AudioRecorder.kt` | Enregistrement AAC (.m4a), source audio de secours si refusée |
| `helper/…/CallAudio.kt` | Haut-parleur et volume pendant l'appel (options), rétablis au raccroché |
| `helper/…/CallInfoResolver.kt` | Numéro lu dans le journal d'appels après l'appel, nom dans les contacts |
| `helper/…/AppLink.kt` | File d'attente et remise à l'application ; rien n'est perdu si elle est absente |
| `core/…/Contract.kt` | Échanges avec l'application (ContentProvider local) |

## Compilation

```bash
./gradlew assembleDebug    # helper/build/outputs/apk/debug/helper-debug.apk
./gradlew test             # tests JVM
```

Une version compilée soi-même est signée avec une autre clé : elle fonctionne, mais l'application CallRecorder du
Play Store refusera ses enregistrements (vérification du certificat).

## Avertissement

Enregistrer une conversation peut nécessiter l'accord de votre interlocuteur selon votre pays. Vous êtes responsable
de l'usage que vous faites de cette application et de vos enregistrements.

## Licence

[Apache 2.0](LICENSE)
