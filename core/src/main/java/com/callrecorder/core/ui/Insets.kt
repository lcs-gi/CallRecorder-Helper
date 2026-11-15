package com.callrecorder.core.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Marges d'une liste plein écran : celles du Scaffold (barres système et barres de l'application) plus les marges
 * du contenu. Passées en `contentPadding`, la liste défile derrière les barres système sans que le premier ni le
 * dernier élément ne soient masqués (affichage bord à bord).
 */
@Composable
fun PaddingValues.withContent(horizontal: Dp = 16.dp, top: Dp = 16.dp, bottom: Dp = 16.dp): PaddingValues {
    val dir = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(dir) + horizontal,
        top = calculateTopPadding() + top,
        end = calculateEndPadding(dir) + horizontal,
        bottom = calculateBottomPadding() + bottom,
    )
}
