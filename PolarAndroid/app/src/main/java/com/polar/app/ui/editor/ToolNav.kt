package com.polar.app.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.polar.app.R
import com.polar.app.help.HelpIds
import com.polar.app.ui.help.helpTarget
import com.polar.app.ui.theme.Spacing

internal data class ToolItem(val tool: Tool, val icon: ImageVector, val label: Int, val helpId: String)
internal val tools = listOf(
    ToolItem(Tool.PHOTOS, Icons.Outlined.PhotoLibrary, R.string.tool_photos, HelpIds.tool("fotos")),
    ToolItem(Tool.FILTERS, Icons.Outlined.Tune, R.string.tool_filters, HelpIds.tool("filtros")),
    ToolItem(Tool.DESIGN, Icons.Outlined.Dashboard, R.string.tool_design, HelpIds.tool("diseno")),
    ToolItem(Tool.TEXT, Icons.Outlined.TextFields, R.string.tool_text, HelpIds.tool("texto")),
    ToolItem(Tool.PAPER, Icons.Outlined.Description, R.string.tool_paper, HelpIds.tool("papel"))
)
@Composable
fun ToolNavBar(tool: Tool?, onSelect: (Tool)->Unit, systemInset: Boolean = true) {
    Surface(color=MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.fillMaxWidth().then(if(systemInset) Modifier.navigationBarsPadding() else Modifier).padding(horizontal=Spacing.s), horizontalArrangement=Arrangement.spacedBy(Spacing.xs)) {
            tools.forEach { item ->
                val label=stringResource(item.label); val active=tool==item.tool
                Surface(onClick={onSelect(item.tool)}, modifier=Modifier.weight(1f).heightIn(min=56.dp).helpTarget(item.helpId).semantics { role=Role.Tab; selected=active; contentDescription=label },
                    shape=RoundedCornerShape(topStart=12.dp,topEnd=12.dp), color=if(active) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer,
                    border=if(active) BorderStroke(1.dp,MaterialTheme.colorScheme.primary) else null) {
                    Column(Modifier.padding(vertical=Spacing.s),horizontalAlignment=Alignment.CenterHorizontally) {
                        Icon(item.icon,null,Modifier.size(20.dp),tint=if(active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(label,style=MaterialTheme.typography.labelSmall,maxLines=1,overflow=TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
@Composable
fun ToolRail(tool: Tool,onSelect:(Tool)->Unit) {
    Column(Modifier.width(104.dp),verticalArrangement=Arrangement.spacedBy(Spacing.s)) {
        tools.forEach { item ->
            val label=stringResource(item.label)
            OutlinedButton(onClick={onSelect(item.tool)},modifier=Modifier.fillMaxWidth().heightIn(min=64.dp).helpTarget(item.helpId).semantics { selected=tool==item.tool; role=Role.Tab;contentDescription=label },contentPadding=PaddingValues(Spacing.s),
                border=BorderStroke(if(tool==item.tool) 2.dp else 1.dp,if(tool==item.tool) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                Column(horizontalAlignment=Alignment.CenterHorizontally) { Icon(item.icon,null,Modifier.size(20.dp)); Text(label,style=MaterialTheme.typography.labelSmall,maxLines=1) }
            }
        }
    }
}
