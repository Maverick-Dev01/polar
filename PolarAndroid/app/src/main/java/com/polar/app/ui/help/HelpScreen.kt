package com.polar.app.ui.help

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.polar.app.R
import com.polar.app.help.HelpArticle
import com.polar.app.help.HelpContent
import com.polar.app.help.HelpSearch
import com.polar.app.ui.components.PolarChip
import com.polar.app.ui.theme.Spacing

/** Centro de ayuda: búsqueda (sin acentos), categorías, artículo con animación y «Llévame ahí». */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HelpScreen(content: HelpContent?, onBack: () -> Unit, onGo: (destino: String) -> Unit, onTour: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var articleId by rememberSaveable { mutableStateOf<String?>(null) }
    val article = articleId?.let { id -> content?.article(id) }
    val focus = LocalFocusManager.current
    BackHandler(enabled = article != null) { articleId = null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(article?.titulo ?: stringResource(R.string.help_title), maxLines = 2) },
                navigationIcon = {
                    IconButton(onClick = { if (article != null) articleId = null else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(if (article != null) R.string.help_back_to_list else R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            if (content == null) {
                Text(stringResource(R.string.help_unavailable), Modifier.padding(Spacing.m), style = MaterialTheme.typography.bodyLarge)
            } else if (article != null) {
                ArticleView(content, article, onGo)
            } else {
                val results = remember(content, query, category) { HelpSearch.filter(content.articulos, query, category) }
                LazyColumn(Modifier.widthIn(max = 640.dp).fillMaxWidth(), contentPadding = PaddingValues(horizontal = Spacing.m, vertical = Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    item {
                        OutlinedTextField(
                            value = query, onValueChange = { query = it }, singleLine = true,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                            placeholder = { Text(stringResource(R.string.help_search)) },
                            leadingIcon = { Icon(Icons.Filled.Search, null) },
                            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, stringResource(R.string.help_search_clear)) } },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                            shape = MaterialTheme.shapes.extraLarge
                        )
                    }
                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PolarChip(selected = category == null, onClick = { category = null }, label = { Text(stringResource(R.string.help_all)) })
                            content.categorias.forEach { c ->
                                PolarChip(selected = category == c.id, onClick = { category = c.id }, label = { Text(c.titulo) })
                            }
                        }
                    }
                    item {
                        OutlinedCard(Modifier.fillMaxWidth().clickable(onClick = onTour)) {
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.help_tour_again)) },
                                supportingContent = { Text(stringResource(R.string.help_tour_again_hint)) },
                                leadingContent = { Icon(Icons.AutoMirrored.Filled.Help, null, tint = MaterialTheme.colorScheme.primary) },
                                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                                modifier = Modifier.heightIn(min = 56.dp)
                            )
                        }
                    }
                    if (results.isEmpty()) item {
                        Column(Modifier.fillMaxWidth().padding(vertical = Spacing.l), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                            Text(stringResource(R.string.help_no_results, query.trim()), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.help_no_results_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = { query = ""; category = null }, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.help_show_all)) }
                        }
                    } else {
                        item {
                            Text(pluralStringResource(R.plurals.help_count, results.size, results.size), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        items(results, key = { it.id }) { a ->
                            val category = content.categorias.firstOrNull { it.id == a.categoria }?.titulo.orEmpty()
                            Card(Modifier.fillMaxWidth().clickable { focus.clearFocus(); articleId = a.id }) {
                                ListItem(
                                    overlineContent = { Text(category) },
                                    headlineContent = { Text(a.titulo) },
                                    supportingContent = { Text(a.resumen) },
                                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                                    modifier = Modifier.heightIn(min = 72.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleView(content: HelpContent, article: HelpArticle, onGo: (String) -> Unit) {
    androidx.compose.foundation.lazy.LazyColumn(Modifier.widthIn(max = 640.dp).fillMaxWidth(), contentPadding = PaddingValues(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                HelpAnimation(article.animacion, Modifier.padding(Spacing.m))
            }
        }
        item { Text(article.resumen, style = MaterialTheme.typography.titleMedium) }
        item { Text(stringResource(R.string.help_article_steps), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() }) }
        val pasos = article.pasosAqui
        items(pasos.size) { i ->
            Row(Modifier.semantics(mergeDescendants = true) { }, horizontalArrangement = Arrangement.spacedBy(Spacing.grid), verticalAlignment = Alignment.Top) {
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(28.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer) }
                }
                Text(pasos[i], Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            }
        }
        article.destino?.let { destino ->
            item { Button(onClick = { onGo(destino) }, Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(stringResource(R.string.help_go)) } }
        }
    }
}
