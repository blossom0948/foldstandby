package com.blossom.foldstand.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.blossom.foldstand.BuildConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class UpdateInfo(
    val versionName: String,
    val downloadUrl: String,
    val releaseUrl: String,
    val fileSizeBytes: Long,
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Downloading(val info: UpdateInfo, val progress: Int) : UpdateState
    data class Ready(val info: UpdateInfo, val file: File) : UpdateState
    data class Error(val message: String) : UpdateState
}

class AppUpdateRepository(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()
    private var availableInfo: UpdateInfo? = null

    fun checkForLatest() {
        if (_state.value is UpdateState.Checking) return
        scope.launch {
            _state.value = UpdateState.Checking
            runCatching { fetchLatest() }
                .onSuccess { info ->
                    availableInfo = info
                    _state.value = info?.let(UpdateState::Available) ?: UpdateState.UpToDate
                }
                .onFailure { _state.value = UpdateState.Error("업데이트 확인 실패") }
        }
    }

    fun downloadAvailable() {
        val info = availableInfo ?: return
        scope.launch {
            runCatching {
                _state.value = UpdateState.Downloading(info, 0)
                download(info) { progress -> _state.value = UpdateState.Downloading(info, progress) }
            }.onSuccess { file -> _state.value = UpdateState.Ready(info, file) }
                .onFailure { _state.value = UpdateState.Error("업데이트 다운로드 실패") }
        }
    }

    fun install(activity: android.app.Activity, file: File) {
        if (!activity.packageManager.canRequestPackageInstalls()) {
            activity.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    "package:${activity.packageName}".toUri(),
                ),
            )
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        activity.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    private suspend fun fetchLatest(): UpdateInfo? = withContext(Dispatchers.IO) {
        val connection = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 12_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "FoldStand/${BuildConfig.VERSION_NAME}")
        }
        connection.inputStream.bufferedReader().use { reader ->
            val json = JSONObject(reader.readText())
            val version = json.optString("tag_name").removePrefix("v")
            if (!isNewer(version, BuildConfig.VERSION_NAME)) return@withContext null
            val assets = json.optJSONArray("assets") ?: return@withContext null
            val apk = (0 until assets.length())
                .asSequence()
                .map { assets.getJSONObject(it) }
                .firstOrNull { it.optString("name").endsWith(".apk") }
                ?: return@withContext null
            UpdateInfo(
                versionName = version,
                downloadUrl = apk.optString("browser_download_url"),
                releaseUrl = json.optString("html_url"),
                fileSizeBytes = apk.optLong("size"),
            )
        }.also { connection.disconnect() }
    }

    private suspend fun download(info: UpdateInfo, onProgress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        val destination = File(context.cacheDir, "foldstand-update-${info.versionName}.apk")
        val connection = (URL(info.downloadUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 30_000
            setRequestProperty("User-Agent", "FoldStand/${BuildConfig.VERSION_NAME}")
        }
        val total = connection.contentLengthLong.coerceAtLeast(info.fileSizeBytes)
        connection.inputStream.use { input ->
            destination.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var copied = 0L
                var read: Int
                while (input.read(buffer).also { read = it } >= 0) {
                    if (read == 0) continue
                    output.write(buffer, 0, read)
                    copied += read
                    onProgress(if (total > 0) ((copied * 100) / total).toInt().coerceIn(0, 100) else 0)
                }
            }
        }
        connection.disconnect()
        destination
    }

    private fun isNewer(candidate: String, current: String): Boolean {
        val left = candidate.split(".").map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        val right = current.split(".").map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        return (0 until maxOf(left.size, right.size)).firstNotNullOfOrNull { index ->
            val a = left.getOrElse(index) { 0 }
            val b = right.getOrElse(index) { 0 }
            (a - b).takeIf { it != 0 }
        }?.let { it > 0 } ?: false
    }

    private companion object {
        const val LATEST_RELEASE_URL = "https://api.github.com/repos/blossom0948/foldstandby/releases/latest"
    }
}
