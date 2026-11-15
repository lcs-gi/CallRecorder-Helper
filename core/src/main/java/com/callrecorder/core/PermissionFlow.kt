package com.callrecorder.core

import androidx.core.content.edit
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Demande des permissions en trois temps (recommandation Android) :
 * déjà accordée → rien ; refusée une fois → explication à l'écran puis demande sur action de l'utilisateur ;
 * refusée définitivement → la fenêtre système ne s'affiche plus, on ouvre la fiche de l'application.
 *
 * `shouldShowRequestPermissionRationale` vaut false avant la toute première demande comme après un refus
 * définitif : on mémorise donc les permissions déjà demandées pour distinguer les deux cas.
 */
object PermissionFlow {
    private const val PREFS = "permission_flow"

    fun isGranted(context: Context, permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun markAsked(context: Context, permissions: Array<String>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit { permissions.forEach { putBoolean(it, true) } }
    }

    private fun wasAsked(context: Context, permission: String) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(permission, false)

    /** Jamais demandée : la demande automatique au premier lancement est possible. */
    fun neverAsked(activity: Activity, permissions: Array<String>): Boolean = permissions.any {
        !isGranted(activity, it) && !wasAsked(activity, it) && !ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
    }

    /** Refusée définitivement : la fenêtre système ne s'affichera plus. */
    fun isPermanentlyDenied(activity: Activity, permission: String): Boolean =
        !isGranted(activity, permission) && wasAsked(activity, permission) &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

    fun anyPermanentlyDenied(activity: Activity, permissions: Array<String>) =
        permissions.any { isPermanentlyDenied(activity, it) }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
