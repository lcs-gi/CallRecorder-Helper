package com.callrecorder.helper

import androidx.core.net.toUri
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.callrecorder.core.Contract
import com.callrecorder.core.PermissionFlow
import com.callrecorder.core.findActivity
import com.callrecorder.core.ui.withContent
import androidx.compose.foundation.layout.consumeWindowInsets
import com.callrecorder.core.ui.Blue
import com.callrecorder.core.ui.CallRecorderTheme
import com.callrecorder.core.ui.Green
import com.callrecorder.core.ui.Orange
import com.callrecorder.core.ui.RestrictedSettingsHint
import com.callrecorder.core.ui.SetupStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Écran unique du Helper : les étapes de configuration et le lien vers l'application. */
class HelperActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { CallRecorderTheme { HelperScreen() } }
    }
}

private data class HelperStatus(
    val missingPermissions: List<String>,
    val accessibilityEnabled: Boolean,
    val batteryUnrestricted: Boolean,
    val appInstalled: Boolean,
    val pending: Int,
) {
    val essentialMissing get() = missingPermissions.any { it in Contract.HELPER_ESSENTIAL_PERMISSIONS }
    val ready get() = !essentialMissing && accessibilityEnabled

    companion object {
        fun permissions(): Array<String> = buildList {
            addAll(Contract.HELPER_ESSENTIAL_PERMISSIONS)
            addAll(Contract.HELPER_OPTIONAL_PERMISSIONS)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()

        fun check(context: Context) = HelperStatus(
            missingPermissions = permissions().filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            },
            accessibilityEnabled = CallAccessibilityService.isEnabled(context),
            batteryUnrestricted = context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName),
            appInstalled = AppLink.isInstalled(context),
            pending = AppLink.pendingCount(context),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HelperScreen() {
    val context = LocalContext.current
    var status by remember { mutableStateOf(HelperStatus.check(context)) }
    var resumes by remember { mutableIntStateOf(0) }
    fun refresh() {
        status = HelperStatus.check(context)
        // Permission Téléphone accordée après l'activation du service : l'écoute des appels démarre maintenant.
        CallAccessibilityService.instance?.listen()
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refresh() }
    val launch = {
        val perms = HelperStatus.permissions()
        PermissionFlow.markAsked(context, perms)
        permLauncher.launch(perms)
    }
    // Bouton de l'étape (l'explication est affichée juste au-dessus) : refus définitif → fiche de l'application.
    val requestPermissions = {
        val activity = context.findActivity()
        val missing = status.missingPermissions.toTypedArray()
        if (activity != null && PermissionFlow.anyPermanentlyDenied(activity, missing)) PermissionFlow.openAppSettings(context)
        else launch()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh(); resumes++ }
    // Premier lancement seulement : jamais redemandé automatiquement après un refus.
    LaunchedEffect(Unit) {
        val activity = context.findActivity() ?: return@LaunchedEffect
        if (PermissionFlow.neverAsked(activity, HelperStatus.permissions())) launch()
    }
    // À chaque retour sur l'écran : remise de ce qui attend (application installée entre-temps…).
    LaunchedEffect(resumes) {
        if (status.pending > 0 && withContext(Dispatchers.IO) { AppLink.deliver(context) } > 0) {
            Notifications.cancelPending(context)
            refresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = padding.withContent(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().consumeWindowInsets(padding),
        ) {
            item { StatusCard(status) }
            item {
                SetupStep(
                    1, Icons.Filled.Mic, stringResource(R.string.setup_perm_title), stringResource(R.string.setup_perm_text),
                    done = status.missingPermissions.isEmpty(),
                    action = stringResource(R.string.action_allow), onAction = requestPermissions,
                )
            }
            item {
                SetupStep(
                    2, Icons.Filled.AccessibilityNew, stringResource(R.string.setup_a11y_title), stringResource(R.string.setup_a11y_text),
                    done = status.accessibilityEnabled,
                    action = stringResource(R.string.action_open), onAction = { open(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                ) {
                    if (Build.VERSION.SDK_INT >= 33 && !status.accessibilityEnabled) RestrictedSettingsHint(
                        stringResource(R.string.setup_restricted_title), stringResource(R.string.setup_restricted_text),
                        stringResource(R.string.setup_restricted_action),
                    ) {
                        open(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
                    }
                }
            }
            item {
                SetupStep(
                    3, Icons.Filled.BatteryChargingFull, stringResource(R.string.setup_battery_title), stringResource(R.string.setup_battery_text),
                    done = status.batteryUnrestricted,
                    action = stringResource(R.string.action_allow), onAction = { requestBatteryExemption(context) },
                    optionalLabel = stringResource(R.string.setup_optional),
                )
            }
            item {
                SetupStep(
                    4, Icons.Filled.GraphicEq, stringResource(R.string.setup_app_title), stringResource(R.string.setup_app_text),
                    done = status.appInstalled,
                    action = stringResource(R.string.action_install), onAction = { openStore(context) },
                )
            }
            if (status.appInstalled) item {
                OutlinedButton(onClick = { openApp(context) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_open_app))
                }
            }
            item {
                Text(
                    stringResource(R.string.privacy_text), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun StatusCard(status: HelperStatus) {
    val (title, text, color) = when {
        !status.ready -> Triple(stringResource(R.string.status_setup_title), stringResource(R.string.status_setup_text), Orange)
        status.pending > 0 -> Triple(
            stringResource(R.string.status_ready_title),
            pluralStringResource(R.plurals.status_pending, status.pending, status.pending), Orange,
        )
        else -> Triple(stringResource(R.string.status_ready_title), stringResource(R.string.status_ready_text), Green)
    }
    Box(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(Blue, Color(0xFF0D47A1))))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(text, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}

private fun open(context: Context, intent: Intent) {
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private fun openApp(context: Context) {
    context.packageManager.getLaunchIntentForPackage(Contract.APP_PACKAGE)?.let { open(context, it) }
}

private fun openStore(context: Context) {
    val market = Intent(Intent.ACTION_VIEW, "market://details?id=${Contract.APP_PACKAGE}".toUri())
    runCatching { context.startActivity(market.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.onFailure {
        open(context, Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=${Contract.APP_PACKAGE}".toUri()))
    }
}

@SuppressLint("BatteryLife")
private fun requestBatteryExemption(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:${context.packageName}".toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }.onFailure { open(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
}
