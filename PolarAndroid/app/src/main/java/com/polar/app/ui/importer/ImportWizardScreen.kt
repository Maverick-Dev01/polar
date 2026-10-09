package com.polar.app.ui.importer

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.polar.app.AppContainer
import com.polar.app.R
import com.polar.app.data.BitmapLoader
import com.polar.app.model.PolarException
import com.polar.app.model.RegionShape
import com.polar.app.model.TemplateRegion
import com.polar.app.template.SavedTemplate
import com.polar.app.ui.components.LabeledSlider
import com.polar.app.ui.components.PolarChip
import com.polar.app.ui.resolve
import com.polar.app.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

private const val NUDGE = 0.02

/** Asistente de 3 pasos: elegir la imagen, revisar los espacios, nombrar y guardar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportWizardScreen(container: AppContainer, onDone: (SavedTemplate) -> Unit, onExit: () -> Unit) {
    val context = LocalContext.current
    // Cada apertura del asistente empieza de cero (el ViewModel vive en la pantalla que lo aloja).
    val sessionKey = rememberSaveable { com.polar.app.model.newId() }
    val vm: ImportWizardViewModel = viewModel(key = sessionKey, factory = viewModelFactory {
        initializer {
            ImportWizardViewModel(
                library = container.templates,
                copyPicked = { uri ->
                    withContext(Dispatchers.IO) {
                        val parsed = android.net.Uri.parse(uri)
                        val ext = context.contentResolver.getType(parsed)?.substringAfter('/')?.substringBefore('+')?.ifBlank { null } ?: "png"
                        val dir = File(context.cacheDir, "import").apply { mkdirs() }
                        val file = File(dir, "molde-${System.nanoTime()}.${if (ext == "jpeg") "jpg" else ext}")
                        (context.contentResolver.openInputStream(parsed) ?: throw PolarException(context.getString(R.string.catalog_bad_image)))
                            .use { input -> file.outputStream().use { input.copyTo(it) } }
                        file
                    }
                }
            )
        }
    })
    val state by vm.state.collectAsStateWithLifecycle()
    var error by remember { mutableStateOf<com.polar.app.ui.UiText?>(null) }
    LaunchedEffect(vm) {
        vm.events.collect { e ->
            when (e) {
                is ImportWizardEvent.Done -> onDone(e.template)
                is ImportWizardEvent.Error -> error = e.text
                ImportWizardEvent.Exit -> onExit()
            }
        }
    }
    BackHandler { vm.back() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.import_title))
                        Text(stringResource(R.string.import_step, state.stepNumber), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = vm::back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) } }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (state.step) {
                WizardStep.PICK -> PickStep(state.busy, vm)
                WizardStep.DUPLICATE -> DuplicateStep(state, vm, container)
                WizardStep.REVIEW -> ReviewStep(state, vm, container)
                WizardStep.NAME -> NameStep(state, vm)
            }
        }
    }
    error?.let {
        AlertDialog(
            onDismissRequest = { error = null },
            confirmButton = { TextButton(onClick = { error = null }) { Text(stringResource(R.string.action_ok)) } },
            text = { Text(it.resolve()) }
        )
    }
}

@Composable
private fun PickStep(busy: Boolean, vm: ImportWizardViewModel) {
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let { uri -> vm.pick(uri.toString()) } }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.m), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.import_pick_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth())
        val description = stringResource(R.string.import_illustration)
        Canvas(Modifier.size(180.dp, 220.dp).semantics { contentDescription = description }) {
            drawRoundRect(Color(0xFF7A293B), cornerRadius = CornerRadius(12.dp.toPx()))
            for ((x, y, w, h) in listOf(listOf(.1f, .08f, .8f, .42f), listOf(.1f, .56f, .8f, .22f))) {
                val tl = Offset(size.width * x, size.height * y); val sz = Size(size.width * w, size.height * h)
                drawRoundRect(Color(0xFFF2DDE1), tl, sz, CornerRadius(6.dp.toPx()))
                drawRoundRect(Color(0xFF7A293B).copy(alpha = .35f), tl, sz, CornerRadius(6.dp.toPx()), style = Stroke(1.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
            }
            drawRoundRect(Color(0xFFE7B9C3), Offset(size.width * .1f, size.height * .86f), Size(size.width * .8f, size.height * .06f), CornerRadius(3.dp.toPx()))
        }
        Text(stringResource(R.string.import_pick_body), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.import_pick_formats), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (busy) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(24.dp)); Text(stringResource(R.string.import_analyzing))
            }
        } else {
            Button(onClick = { pick.launch("image/*") }, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.Outlined.Upload, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.import_pick_button))
            }
        }
    }
}

@Composable
private fun DuplicateStep(state: ImportWizardState, vm: ImportWizardViewModel, container: AppContainer) {
    val match = state.match ?: return
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Text(stringResource(R.string.import_dup_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.import_dup_body, match.template.name), style = MaterialTheme.typography.bodyLarge)
        if (match.sameFile) Text(stringResource(R.string.import_dup_same), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TemplatePreview(container, match.template, Modifier.fillMaxWidth().height(220.dp))
        Button(onClick = vm::useExisting, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.import_use_existing)) }
        OutlinedButton(onClick = vm::keepAsNew, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.import_save_new)) }
    }
}

@Composable
internal fun TemplatePreview(container: AppContainer, template: SavedTemplate, modifier: Modifier = Modifier, maxSide: Int = 480) {
    val bitmap by produceState<Bitmap?>(null, template.id) {
        value = withContext(Dispatchers.IO) { container.bitmaps.load(container.templates.imageFile(template).absolutePath, maxSide) }
    }
    Box(modifier.background(Color(0xFFE4DED5)), contentAlignment = Alignment.Center) {
        bitmap?.let { Image(it.asImageBitmap(), null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()) }
    }
}

@Composable
private fun ReviewStep(state: ImportWizardState, vm: ImportWizardViewModel, container: AppContainer) {
    val file = state.file ?: return
    val bitmap by produceState<Bitmap?>(null, file) {
        value = withContext(Dispatchers.IO) { container.bitmaps.load(file.absolutePath, BitmapLoader.PREVIEW_MAX) }
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = Spacing.m)) {
            Text(stringResource(R.string.import_review_title), style = MaterialTheme.typography.titleLarge)
            Text(
                if (state.detectedCount > 0) stringResource(R.string.import_found, state.detectedCount) else stringResource(R.string.import_none),
                style = MaterialTheme.typography.bodyMedium
            )
            if (state.approximateShape) Text(stringResource(R.string.import_approx), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.import_review_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(Spacing.m), contentAlignment = Alignment.Center) {
            val bmp = bitmap
            if (bmp == null) CircularProgressIndicator() else {
                val aspect = bmp.width.toFloat() / bmp.height
                val width = min(maxWidth.value, maxHeight.value * aspect).dp
                RegionEditor(state, vm, bmp, Modifier.size(width, width / aspect))
            }
        }
        RegionControls(state, vm)
    }
}

private enum class DragKind { MOVE, RESIZE }

@Composable
private fun RegionEditor(state: ImportWizardState, vm: ImportWizardViewModel, bitmap: Bitmap, modifier: Modifier) {
    val density = LocalDensity.current
    val regions by rememberUpdatedState(state.regions)
    val selected by rememberUpdatedState(state.selected)
    var sizePx by remember { mutableStateOf(Size.Zero) }
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val handleReach = with(density) { 28.dp.toPx() }
    Box(
        modifier
            .background(Color(0xFFD8D2C8))
            .pointerInput(Unit) {
                var kind: DragKind? = null
                var index = 0
                var corner: Corner? = null
                detectDragGestures(
                    onDragStart = { o ->
                        val fx = o.x / size.width.toDouble(); val fy = o.y / size.height.toDouble()
                        val rx = handleReach / size.width.toDouble(); val ry = handleReach / size.height.toDouble()
                        val sel = selected?.let { regions.getOrNull(it) }
                        val hitCorner = sel?.let { ImportWizard.cornerAt(it, fx, fy, rx, ry) }
                        if (sel != null && hitCorner != null) { kind = DragKind.RESIZE; index = selected!!; corner = hitCorner }
                        else ImportWizard.regionAt(regions, fx, fy)?.let { kind = DragKind.MOVE; index = it; vm.select(it) } ?: run { kind = null }
                    },
                    onDragEnd = { kind = null }, onDragCancel = { kind = null }
                ) { change, drag ->
                    change.consume()
                    val dx = drag.x / size.width.toDouble(); val dy = drag.y / size.height.toDouble()
                    when (kind) {
                        DragKind.MOVE -> vm.move(index, dx, dy)
                        DragKind.RESIZE -> vm.resize(index, corner!!, dx, dy)
                        null -> Unit
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { o -> vm.select(ImportWizard.regionAt(regions, o.x / size.width.toDouble(), o.y / size.height.toDouble())) }
            }
    ) {
        Image(bitmap.asImageBitmap(), null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize()) {
            sizePx = size
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true
                textSize = 18.dp.toPx()
            }
            for ((i, r) in state.regions.withIndex()) {
                val tl = Offset((r.x * size.width).toFloat(), (r.y * size.height).toFloat())
                val sz = Size((r.width * size.width).toFloat(), (r.height * size.height).toFloat())
                val path = Path().apply {
                    when (r.shape) {
                        RegionShape.ELLIPSE -> addOval(androidx.compose.ui.geometry.Rect(tl, sz))
                        RegionShape.ROUND -> { val rad = (r.radius * min(sz.width, sz.height)).toFloat(); addRoundRect(androidx.compose.ui.geometry.RoundRect(androidx.compose.ui.geometry.Rect(tl, sz), CornerRadius(rad))) }
                        RegionShape.RECT -> addRect(androidx.compose.ui.geometry.Rect(tl, sz))
                    }
                }
                val isSelected = i == state.selected
                drawPath(path, primary.copy(alpha = if (isSelected) .35f else .2f))
                drawPath(path, primary, style = Stroke(if (isSelected) 3.dp.toPx() else 1.5.dp.toPx()))
                // Número con una pastilla para que se lea sobre cualquier imagen.
                val cx = tl.x + sz.width / 2; val cy = tl.y + sz.height / 2
                drawCircle(primary, 14.dp.toPx(), Offset(cx, cy))
                drawContext.canvas.nativeCanvas.drawText("${i + 1}", cx, cy + 6.dp.toPx(), paint.apply { color = android.graphics.Color.WHITE })
                if (isSelected) for (c in Corner.entries) {
                    val hx = if (c == Corner.TOP_LEFT || c == Corner.BOTTOM_LEFT) tl.x else tl.x + sz.width
                    val hy = if (c == Corner.TOP_LEFT || c == Corner.TOP_RIGHT) tl.y else tl.y + sz.height
                    drawCircle(onPrimary, 10.dp.toPx(), Offset(hx, hy))
                    drawCircle(primary, 10.dp.toPx(), Offset(hx, hy), style = Stroke(3.dp.toPx()))
                }
            }
        }
        // Un nodo de accesibilidad por espacio, con acciones para mover y cambiar su tamaño sin arrastrar.
        val total = state.regions.size
        val left = stringResource(R.string.import_act_left); val right = stringResource(R.string.import_act_right)
        val up = stringResource(R.string.import_act_up); val down = stringResource(R.string.import_act_down)
        val wider = stringResource(R.string.import_act_wider); val narrower = stringResource(R.string.import_act_narrower)
        val taller = stringResource(R.string.import_act_taller); val shorter = stringResource(R.string.import_act_shorter)
        val remove = stringResource(R.string.import_remove); val select = stringResource(R.string.import_act_select)
        val selectedText = stringResource(R.string.import_selected)
        for ((i, r) in state.regions.withIndex()) {
            val description = stringResource(R.string.import_region_desc, i + 1, total, r.shape.displayName)
            Box(
                Modifier
                    .offset { IntOffset((r.x * sizePx.width).toInt(), (r.y * sizePx.height).toInt()) }
                    .size(with(density) { (r.width * sizePx.width).toFloat().toDp() }, with(density) { (r.height * sizePx.height).toFloat().toDp() })
                    .semantics {
                        contentDescription = description
                        if (i == state.selected) stateDescription = selectedText
                        onClick(select) { vm.select(i); true }
                        customActions = buildList {
                            add(CustomAccessibilityAction(left) { vm.select(i); vm.move(i, -NUDGE, 0.0); true })
                            add(CustomAccessibilityAction(right) { vm.select(i); vm.move(i, NUDGE, 0.0); true })
                            add(CustomAccessibilityAction(up) { vm.select(i); vm.move(i, 0.0, -NUDGE); true })
                            add(CustomAccessibilityAction(down) { vm.select(i); vm.move(i, 0.0, NUDGE); true })
                            add(CustomAccessibilityAction(wider) { vm.select(i); vm.grow(i, NUDGE, 0.0); true })
                            add(CustomAccessibilityAction(narrower) { vm.select(i); vm.grow(i, -NUDGE, 0.0); true })
                            add(CustomAccessibilityAction(taller) { vm.select(i); vm.grow(i, 0.0, NUDGE); true })
                            add(CustomAccessibilityAction(shorter) { vm.select(i); vm.grow(i, 0.0, -NUDGE); true })
                            if (total > 1) add(CustomAccessibilityAction(remove) { vm.removeRegion(i); true })
                        }
                    }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RegionControls(state: ImportWizardState, vm: ImportWizardViewModel) {
    val index = state.selected
    val region: TemplateRegion? = index?.let { state.regions.getOrNull(it) }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            if (region != null && index != null) {
                Text(stringResource(R.string.import_shape), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RegionShape.entries.forEach { shape ->
                        PolarChip(region.shape == shape, { vm.setShape(index, shape) }, { Text(shape.displayName) }, modifier = Modifier.heightIn(min = 48.dp))
                    }
                }
                if (region.shape == RegionShape.ROUND) LabeledSlider(
                    stringResource(R.string.import_radius), (region.radius * 100).toFloat(), 1f..50f, "${(region.radius * 100).toInt()} %",
                    onChange = { vm.setRadius(index, it / 100.0) }, onStart = {}, onEnd = {}
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = vm::addRegion, enabled = state.regions.size < state.maxRegions, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.import_add)) }
                OutlinedButton(onClick = { index?.let(vm::removeRegion) }, enabled = index != null && state.regions.size > 1, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.import_remove)) }
            }
            Button(onClick = vm::next, enabled = state.regions.isNotEmpty(), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.import_next)) }
        }
    }
}

@Composable
private fun NameStep(state: ImportWizardState, vm: ImportWizardViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Text(stringResource(R.string.import_name_title), style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = state.name, onValueChange = vm::setName, singleLine = true, modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.import_name_label)) },
            supportingText = { Text(stringResource(R.string.import_name_help)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { vm.save() })
        )
        Text(stringResource(R.string.catalog_mine_spaces, state.regions.size), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = vm::save, enabled = state.canSave && !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.import_save)) }
    }
}
