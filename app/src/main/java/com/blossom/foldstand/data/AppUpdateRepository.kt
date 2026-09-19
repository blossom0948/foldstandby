package com.blossom.foldstand.data

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import com.blossom.foldstand.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
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
    val sha256: String? = null,
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Downloading(val info: UpdateInfo, val progress: Int) : UpdateState
    data class Ready(val info: UpdateInfo, val file: File) : UpdateState
    data class WaitingForInstallPermission(val info: UpdateInfo, val file: File) : UpdateState
    data class Installing(val info: UpdateInfo, val file: File) : UpdateState
    data class InstallerOpened(val info: UpdateInfo, val file: File) : UpdateState
    data class Error(val message: String) : UpdateState
}

class AppUpdateRepository(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()
    private var availableInfo: UpdateInfo? = null

    /** Checks for a newer release only when the user explicitly asks. */
    fun checkForLatest() {
        if (_state.value is UpdateState.Checking ||
            _state.value is UpdateState.Downloading ||
            _state.value is UpdateState.Installing
        ) return
        scope.launch {
            _state.value = UpdateState.Checking
            runCatching { fetchLatest() }
                .onSuccess { info ->
                    availableInfo = info
                    _state.value = info?.let(UpdateState::Available) ?: UpdateState.UpToDate
                }
                .onFailure { error ->
                    _state.value = UpdateState.Error(error.message ?: "업데이트 확인에 실패했어요.")
                }
        }
    }

    fun downloadAvailable() {
        val info = availableInfo ?: return
        if (_state.value is UpdateState.Downloading || _state.value is UpdateState.Installing) return
        scope.launch {
            runCatching {
                _state.value = UpdateState.Downloading(info, 0)
                download(info) { progress -> _state.value = UpdateState.Downloading(info, progress) }
            }.onSuccess { file ->
                _state.value = UpdateState.Ready(info, file)
            }.onFailure { error ->
                _state.value = UpdateState.Error(error.message ?: "업데이트 APK를 다운로드하지 못했어요.")
            }
        }
    }

    fun install(activity: Activity, file: File) {
        val info = availableInfo ?: (state.value as? UpdateState.Ready)?.info
        if (info == null || !file.isFile || file.length() <= 0L) {
            _state.value = UpdateState.Error("다운로드된 업데이트 파일이 없어요. 업데이트를 다시 확인해 주세요.")
            return
        }

        if (!activity.packageManager.canRequestPackageInstalls()) {
            _state.value = UpdateState.WaitingForInstallPermission(info, file)
            activity.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    "package:${activity.packageName}".toUri(),
                ),
            )
            return
        }

        _state.value = UpdateState.Installing(info, file)
        scope.launch {
            runCatching { installWithPackageInstaller(activity, file) }
                .onSuccess { _state.value = UpdateState.InstallerOpened(info, file) }
                .onFailure { error ->
                    // Some vendor package installers do not expose a PackageInstaller session UI.
                    // Keep a reliable local-URI fallback for those devices; it never opens a web URL.
                    Log.w(TAG, "PackageInstaller session failed; using local APK installer", error)
                    withContext(Dispatchers.Main) {
                        runCatching { openLocalInstaller(activity, file) }
                            .onSuccess { _state.value = UpdateState.InstallerOpened(info, file) }
                            .onFailure { fallbackError ->
                                _state.value = UpdateState.Error(
                                    fallbackError.message ?: "Android 설치 화면을 열지 못했어요.",
                                )
                            }
                    }
                }
        }
    }

    /** Resumes a pending install after the user returns from Android's unknown-source setting. */
    fun resumePendingInstall(activity: Activity) {
        val pending = state.value as? UpdateState.WaitingForInstallPermission ?: return
        if (activity.packageManager.canRequestPackageInstalls()) {
            install(activity, pending.file)
        }
    }

    /**
     * Streams the verified APK into Android's package installer. FoldStand remains in control
     * until the final, user-confirmed system install screen appears.
     */
    private fun installWithPackageInstaller(activity: Activity, file: File) {
        val installer = activity.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(file.length())
        }
        val sessionId = installer.createSession(params)
        try {
            installer.openSession(sessionId).use { session ->
                file.inputStream().use { input ->
                    session.openWrite("base.apk", 0L, file.length()).use { output ->
                        input.copyTo(output, DEFAULT_BUFFER_SIZE)
                        session.fsync(output)
                    }
                }
                val callbackIntent = Intent(context, UpdateInstallReceiver::class.java).apply {
                    action = UpdateInstallReceiver.ACTION_INSTALL_COMMIT
                    putExtra(UpdateInstallReceiver.EXTRA_SESSION_ID, sessionId)
                    setPackage(context.packageName)
                }
                val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        // Android 14+ rejects immutable status receivers for targetSdk 35+.
                        PendingIntent.FLAG_MUTABLE
                    } else {
                        0
                    }
                val statusReceiver = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    callbackIntent,
                    pendingFlags,
                )
                session.commit(statusReceiver.intentSender)
            }
        } catch (error: Throwable) {
            runCatching { installer.abandonSession(sessionId) }
            throw error
        }
    }

    private fun openLocalInstaller(activity: Activity, file: File) {
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
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "FoldStand/${BuildConfig.VERSION_NAME}")
        }
        try {
            check(connection.responseCode in 200..299) { "업데이트 서버 응답 오류: ${connection.responseCode}" }
            connection.inputStream.bufferedReader().use { reader ->
                val json = JSONObject(reader.readText())
                val version = parseVersionName(json.optString("tag_name"))
                    ?: error("업데이트 버전 형식을 확인하지 못했어요.")
                if (!isVersionNewer(version, BuildConfig.VERSION_NAME)) return@withContext null
                val assets = json.optJSONArray("assets") ?: error("업데이트 APK를 찾지 못했어요.")
                val apk = (0 until assets.length())
                    .asSequence()
                    .map { assets.getJSONObject(it) }
                    .firstOrNull { asset ->
                        asset.optString("name") == BuildConfig.UPDATE_ASSET_NAME &&
                            asset.optString("browser_download_url").startsWith(RELEASE_DOWNLOAD_PREFIX)
                    }
                    ?: error("현재 설치본에 맞는 업데이트 APK를 찾지 못했어요.")
                val size = apk.optLong("size", 0L).coerceAtLeast(0L)
                require(size in 1..MAX_APK_BYTES) { "업데이트 APK 크기를 확인하지 못했어요." }
                UpdateInfo(
                    versionName = version,
                    downloadUrl = apk.optString("browser_download_url"),
                    releaseUrl = json.optString("html_url"),
                    fileSizeBytes = size,
                    sha256 = apk.optString("digest")
                        .removePrefix("sha256:")
                        .takeIf { it.matches(SHA256_PATTERN) },
                )
            }
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun download(info: UpdateInfo, onProgress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        require(info.downloadUrl.startsWith(RELEASE_DOWNLOAD_PREFIX)) { "업데이트 APK의 출처가 올바르지 않아요." }
        require(info.versionName.matches(VERSION_PATTERN)) { "업데이트 버전이 올바르지 않아요." }
        require(info.fileSizeBytes in 1..MAX_APK_BYTES) { "업데이트 APK 크기를 확인하지 못했어요." }

        val updateDirectory = File(context.cacheDir, "updates")
        check(updateDirectory.exists() || updateDirectory.mkdirs()) { "업데이트 저장 공간을 만들지 못했어요." }
        val destination = File(updateDirectory, "foldstand-${info.versionName}.apk")
        val temporary = File(updateDirectory, ".${destination.name}.part")
        var lastError: Throwable? = null

        repeat(MAX_DOWNLOAD_ATTEMPTS) { attempt ->
            try {
                temporary.delete()
                val connection = (URL(info.downloadUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = DOWNLOAD_READ_TIMEOUT_MS
                    instanceFollowRedirects = true
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.android.package-archive")
                    setRequestProperty("Accept-Encoding", "identity")
                    setRequestProperty("User-Agent", "FoldStand/${BuildConfig.VERSION_NAME}")
                }
                try {
                    check(connection.responseCode in 200..299) { "업데이트 서버 응답 오류: ${connection.responseCode}" }
                    val contentLength = connection.contentLengthLong
                    check(contentLength <= 0L || contentLength == info.fileSizeBytes) {
                        "업데이트 APK 크기가 릴리스 정보와 다릅니다."
                    }
                    connection.inputStream.use { input ->
                        FileOutputStream(temporary).use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var downloaded = 0L
                            var lastProgress = -1
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                if (read == 0) continue
                                downloaded += read
                                check(downloaded <= MAX_APK_BYTES && downloaded <= info.fileSizeBytes) {
                                    "다운로드 APK 크기가 예상보다 큽니다."
                                }
                                output.write(buffer, 0, read)
                                val progress = ((downloaded * 100L) / info.fileSizeBytes)
                                    .toInt().coerceIn(0, 99)
                                if (progress != lastProgress) {
                                    lastProgress = progress
                                    onProgress(progress)
                                }
                            }
                            output.fd.sync()
                            check(downloaded == info.fileSizeBytes) {
                                "다운로드 APK가 끝까지 완료되지 않았어요."
                            }
                        }
                    }
                } finally {
                    connection.disconnect()
                }

                info.sha256?.let { expected ->
                    check(sha256(temporary).equals(expected, ignoreCase = true)) {
                        "다운로드한 APK의 SHA-256 검증에 실패했어요."
                    }
                }
                verifyApk(temporary, info.versionName)
                if (destination.exists()) check(destination.delete()) { "이전 업데이트 파일을 정리하지 못했어요." }
                check(temporary.renameTo(destination)) { "다운로드 APK를 준비하지 못했어요." }
                onProgress(100)
                return@withContext destination
            } catch (error: Throwable) {
                lastError = error
                if (attempt + 1 < MAX_DOWNLOAD_ATTEMPTS) {
                    Thread.sleep(RETRY_DELAY_MILLIS * (attempt + 1))
                }
            }
        }
        throw IOException("APK 다운로드를 완료하지 못했어요.", lastError)
    }

    private fun verifyApk(apkFile: File, expectedVersionName: String) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        val archive = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, flags)
            ?: throw IOException("다운로드한 파일이 정상적인 APK가 아니에요.")
        check(archive.packageName == context.packageName) { "다른 앱의 APK라 설치를 중단했어요." }
        check(archive.versionName == expectedVersionName) { "APK 버전이 릴리스 정보와 일치하지 않아요." }
        val current = context.packageManager.getPackageInfo(context.packageName, flags)
        check(PackageInfoCompat.getLongVersionCode(archive) > PackageInfoCompat.getLongVersionCode(current)) {
            "설치된 버전보다 새 버전이 아니에요."
        }
        check(signerDigests(archive) == signerDigests(current)) {
            "기존 앱과 서명 키가 달라 업데이트를 중단했어요."
        }
    }

    private fun signerDigests(info: PackageInfo): Set<String> {
        @Suppress("DEPRECATION")
        val signers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners?.toList().orEmpty()
        } else {
            info.signatures?.toList().orEmpty()
        }
        if (signers.isEmpty()) throw IOException("APK 서명을 확인할 수 없어요.")
        return signers.map { signature -> sha256(signature.toByteArray()) }.toSet()
    }

    private fun sha256(file: File): String = file.inputStream().buffered().use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
        digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private fun sha256(value: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(value)
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private fun parseVersionName(tagName: String): String? =
        VERSION_PATTERN.find(tagName.removePrefix("v"))?.value

    private fun isVersionNewer(remote: String, current: String): Boolean {
        val remoteParts = versionParts(remote) ?: return remote != current
        val currentParts = versionParts(current) ?: return remote != current
        return remoteParts.zip(currentParts).firstNotNullOfOrNull { (remotePart, currentPart) ->
            (remotePart - currentPart).takeIf { it != 0 }
        }?.let { it > 0 } ?: (remoteParts.size > currentParts.size)
    }

    private fun versionParts(version: String): List<Int>? =
        VERSION_PATTERN.matchEntire(version)?.groupValues?.drop(1)?.map(String::toInt)

    private companion object {
        const val TAG = "FoldStandUpdater"
        const val LATEST_RELEASE_URL = "https://api.github.com/repos/blossom0948/foldstandby/releases/latest"
        const val RELEASE_DOWNLOAD_PREFIX = "https://github.com/blossom0948/foldstandby/releases/download/"
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 30_000
        const val DOWNLOAD_READ_TIMEOUT_MS = 60_000
        const val MAX_APK_BYTES = 160L * 1024L * 1024L
        const val MAX_DOWNLOAD_ATTEMPTS = 3
        const val RETRY_DELAY_MILLIS = 750L
        val VERSION_PATTERN = Regex("(\\d+)\\.(\\d+)\\.(\\d+)")
        val SHA256_PATTERN = Regex("[a-fA-F0-9]{64}")
    }
}
