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
