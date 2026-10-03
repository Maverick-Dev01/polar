package com.polar.app.ui.editor.panels

import com.polar.app.ui.theme.Spacing
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PanelColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.m, vertical = 8.dp).padding(bottom = Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
        content = content
    )
}
