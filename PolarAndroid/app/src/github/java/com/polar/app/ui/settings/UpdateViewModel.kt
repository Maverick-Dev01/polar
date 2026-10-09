package com.polar.app.ui.settings

import android.app.DownloadManager
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polar.app.R
import com.polar.app.data.PendingUpdate
import com.polar.app.data.UpdateInfo
import com.polar.app.data.UpdateRepository
import com.polar.app.ui.UiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class UpdatePhase { IDLE, CHECKING, CURRENT, AVAILABLE, DOWNLOADING, VERIFYING, READY }
data class UpdateState(val phase: UpdatePhase = UpdatePhase.IDLE, val info: UpdateInfo? = null,
                       val progress: Float? = null, val message: UiText? = null)

class UpdateViewModel(private val repository: UpdateRepository) : ViewModel() {
    private val mutable = MutableStateFlow(UpdateState())
    val state = mutable.asStateFlow()
    val canUpdate get() = repository.canUpdate
    private var operation: Job? = null
    private var pending: PendingUpdate? = null

    init { run { repository.resume()?.let { monitor(it) } } }

    private fun run(block: suspend () -> Unit) {
        if (operation?.isActive == true) return
        operation = viewModelScope.launch {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutable.value = mutable.value.copy(phase = UpdatePhase.IDLE, message = UiText(R.string.updates_error)) }
        }
    }

    fun check() = run {
        mutable.value = UpdateState(UpdatePhase.CHECKING)
        val info = repository.latest()
        mutable.value = when {
            info == null -> UpdateState(message = UiText(R.string.updates_not_published))
            info.versionCode <= repository.version -> UpdateState(UpdatePhase.CURRENT, message = UiText(R.string.updates_current))
            info.minSdk > Build.VERSION.SDK_INT -> UpdateState(message = UiText(R.string.updates_incompatible))
            else -> UpdateState(UpdatePhase.AVAILABLE, info)
        }
    }

    fun download() = run { state.value.info?.let { monitor(repository.start(it)) } }

    private suspend fun monitor(update: PendingUpdate) {
        pending = update
        try {
            while (true) {
                val progress = repository.progress(update)
                check(progress.bytes <= update.info.sizeBytes && progress.total <= update.info.sizeBytes)
                when (progress.status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        mutable.value = UpdateState(UpdatePhase.VERIFYING, update.info)
                        repository.verifiedIntent(update)
                        mutable.value = UpdateState(UpdatePhase.READY, update.info)
                        return
                    }
                    DownloadManager.STATUS_FAILED -> error("Descarga fallida")
                    else -> mutable.value = UpdateState(UpdatePhase.DOWNLOADING, update.info,
                        (progress.bytes.toFloat() / update.info.sizeBytes).coerceIn(0f, 1f),
                        if (progress.status == DownloadManager.STATUS_PAUSED) UiText(R.string.updates_waiting) else null)
                }
                delay(500)
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            repository.cancel(update.downloadId)
            pending = null
            mutable.value = UpdateState(UpdatePhase.AVAILABLE, update.info, message = UiText(R.string.updates_verification_error))
        }
    }

    fun cancel() {
        operation?.cancel()
        operation = null
        run {
            pending?.let { repository.cancel(it.downloadId) }
            pending = null
            mutable.value = UpdateState(message = UiText(R.string.updates_cancelled))
        }
    }

    suspend fun installerIntent(): Intent? {
        if (state.value.phase != UpdatePhase.READY) return null
        val downloaded = pending ?: return null
        mutable.value = state.value.copy(phase = UpdatePhase.VERIFYING, message = null)
        return try {
            repository.verifiedIntent(downloaded).also { mutable.value = state.value.copy(phase = UpdatePhase.READY) }
        } catch (cancelled: CancellationException) {
            mutable.value = state.value.copy(phase = UpdatePhase.READY)
            throw cancelled
        }
        catch (_: Exception) {
            repository.cancel(downloaded.downloadId)
            pending = null
            mutable.value = UpdateState(UpdatePhase.AVAILABLE, downloaded.info, message = UiText(R.string.updates_verification_error))
            null
        }
    }

    fun installationUnavailable() { mutable.value = state.value.copy(message = UiText(R.string.updates_install_unavailable)) }
    fun permissionDenied() { mutable.value = state.value.copy(message = UiText(R.string.updates_permission_denied)) }
}
