package com.maskan.mobileapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maskan.mobileapp.ui.theme.MaskanTheme

/*
 * Tablet layout helpers — the Compose counterpart of iOS AdaptiveLayout.swift.
 * Width classes follow Material's window size classes and read the app's own
 * window (so split-screen and freeform windows count too). On a phone every
 * helper falls back to the single-column layout, so phone screens are unchanged.
 */

/** Medium width and up (≥ 600dp): tablets in portrait, unfolded foldables. */
@Composable
fun isMediumWidth(): Boolean = LocalConfiguration.current.screenWidthDp >= 600

/** Expanded width (≥ 840dp): tablets in landscape — room for side-by-side layouts. */
@Composable
fun isExpandedWidth(): Boolean = LocalConfiguration.current.screenWidthDp >= 840

/**
 * Tablets: caps content at a comfortable reading width and centres it, so forms,
 * details and settings don't stretch across the whole screen. No-op on phones.
 */
@Composable
fun Modifier.readableWidth(maxWidth: Dp = 820.dp): Modifier =
    if (isMediumWidth()) {
        this.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = maxWidth)
    } else {
        this
    }

/**
 * Wraps a pushed screen (form or detail) so on tablets it sits in a centred
 * column of readable width over the screen background, like the iPad's
 * centred 760–860pt layouts. Phones get the screen unchanged.
 */
@Composable
fun ReadableRoute(maxWidth: Dp = 820.dp, content: @Composable () -> Unit) {
    if (isMediumWidth()) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaskanTheme.colors.background),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(modifier = Modifier.widthIn(max = maxWidth).fillMaxHeight()) { content() }
        }
    } else {
        content()
    }
}

/**
 * Two equal columns side by side on expanded widths, stacked (leading first)
 * otherwise. Each column keeps its own height, like iOS AdaptiveColumns.
 */
@Composable
fun AdaptiveColumns(
    modifier: Modifier = Modifier,
    spacing: Dp = 20.dp,
    leading: @Composable ColumnScope.() -> Unit,
    trailing: @Composable ColumnScope.() -> Unit,
) {
    if (isExpandedWidth()) {
        Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing), content = leading)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing), content = trailing)
        }
    } else {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing)) {
            leading()
            trailing()
        }
    }
}

/**
 * Grid cells for card lists: one column on phones, as many columns of at
 * least [minWidth] as fit on tablets.
 */
@Composable
fun adaptiveGridCells(minWidth: Dp = 440.dp): GridCells =
    if (isMediumWidth()) GridCells.Adaptive(minWidth) else GridCells.Fixed(1)

/**
 * Expanded widths: a fixed-width list beside a detail pane (like the iPad
 * Properties/Tenants/Bills split). Callers show [list] alone on narrower widths.
 */
@Composable
fun ListDetailLayout(
    modifier: Modifier = Modifier,
    listWidth: Dp = 480.dp,
    list: @Composable () -> Unit,
    detail: @Composable RowScope.() -> Unit,
) {
    Row(modifier = modifier) {
        Box(modifier = Modifier.width(listWidth).fillMaxHeight()) { list() }
        VerticalDivider(color = MaskanTheme.colors.border)
        detail()
    }
}
