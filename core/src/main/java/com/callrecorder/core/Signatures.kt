package com.callrecorder.core

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/**
 * Vérifie qu'une autre application est bien la nôtre avant de lui confier des enregistrements,
 * ou d'en accepter : même nom de paquet ne suffit pas, le certificat de signature doit correspondre.
 *
 * Est accepté : le certificat de l'application elle-même (les deux applications signées avec la même clé,
 * cas des versions de développement), ou une empreinte SHA-256 listée dans `trustedCerts`
 * (cas du Play Store, qui signe l'application avec sa propre clé : voir README).
 */
object Signatures {
    fun normalize(digest: String): String = digest.filter { it.isLetterOrDigit() }.uppercase()

    /** Liste « AB:CD:…, 12:34:… » (champ BuildConfig) → empreintes normalisées. */
    fun parseList(list: String): Set<String> =
        list.split(',', ';', ' ', '\n').map { normalize(it) }.filter { it.length == 64 }.toSet()

    fun isTrusted(context: Context, packageName: String, trustedCerts: Collection<String>): Boolean {
        val theirs = certificates(context.packageManager, packageName)
        if (theirs.isEmpty()) return false
        val allowed = certificates(context.packageManager, context.packageName) + trustedCerts.map { normalize(it) }
        return theirs.any { it in allowed }
    }

    /**
     * Vérifie l'appelant d'une transaction Binder en cours (ContentProvider, service AIDL) :
     * il doit appartenir à [packageName] et être signé par un certificat de confiance.
     * L'identité vient de `Binder.getCallingUid()`, jamais d'une donnée fournie par l'appelant.
     * Un appel hors transaction (UID de l'application elle-même) est refusé.
     */
    fun isTrustedCaller(context: Context, callingUid: Int, packageName: String, trustedCerts: Collection<String>): Boolean {
        if (callingUid == android.os.Process.myUid()) return false
        val pm = context.packageManager
        if (packageName !in pm.getPackagesForUid(callingUid).orEmpty()) return false
        val allowed = certificates(pm, context.packageName) + trustedCerts.map { normalize(it) }
        if (allowed.isEmpty()) return false
        return if (Build.VERSION.SDK_INT >= 28) {
            // Certificat de l'UID appelant lui-même, historique de rotation de clé compris.
            allowed.any { runCatching { pm.hasSigningCertificate(callingUid, hexToBytes(it), PackageManager.CERT_INPUT_SHA256) }.getOrDefault(false) }
        } else {
            certificates(pm, packageName).any { it in allowed }
        }
    }

    fun hexToBytes(hex: String): ByteArray = ByteArray(hex.length / 2) { i ->
        ((Character.digit(hex[2 * i], 16) shl 4) + Character.digit(hex[2 * i + 1], 16)).toByte()
    }

    @SuppressLint("PackageManagerGetSignatures")
    @Suppress("DEPRECATION")
    fun certificates(pm: PackageManager, packageName: String): Set<String> = runCatching {
        val sigs = if (Build.VERSION.SDK_INT >= 28) {
            val info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo ?: return emptySet()
            if (info.hasMultipleSigners()) info.apkContentsSigners else info.signingCertificateHistory
        } else {
            pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
        }
        sigs.orEmpty().map { sha256(it.toByteArray()) }.toSet()
    }.getOrDefault(emptySet())

    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02X".format(it) }
}
