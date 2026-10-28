package dev.yaee.expert.settings

import com.google.gson.JsonParser
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.application.PathManager
import com.intellij.platform.lsp.api.LspClientManager
import dev.yaee.expert.lsp.ExpertLspServerSupportProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

object ExpertDownloader {
    private const val RELEASE_API = "https://api.github.com/repos/expert-lsp/expert/releases/latest"
    private val downloading = AtomicBoolean(false)

    fun download(project: Project, onFailure: ((Throwable) -> Unit)? = null) {
        if (!downloading.compareAndSet(false, true)) return
        val assetName = ExpertReleaseAssets.assetName(System.getProperty("os.name"), System.getProperty("os.arch"))
        if (assetName == null) {
            downloading.set(false)
            onFailure?.invoke(UnsupportedOperationException("Expert does not publish a binary for this operating system and architecture."))
            return
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Downloading Expert", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = "Finding the latest Expert release"
                    indicator.isIndeterminate = true
                    val release = fetchRelease()
                    val assetUrl = release.second[assetName]
                        ?: error("The latest Expert release does not include $assetName")
                    val checksumUrl = release.second["expert_checksums.txt"]
                        ?: error("The latest Expert release has no checksum manifest")
                    val checksum = ExpertReleaseAssets.checksumFor(assetName, downloadText(checksumUrl))
                        ?: error("No checksum was published for $assetName")

                    val destination = destinationFile(assetName)
                    Files.createDirectories(destination.parent)
                    val temporary = Files.createTempFile(destination.parent, "expert-", ".download")
                    try {
                        indicator.text = "Downloading Expert ${release.first}"
                        downloadToFile(assetUrl, temporary.toFile(), indicator)
                        val actualChecksum = sha256(temporary.toFile())
                        check(actualChecksum.equals(checksum, ignoreCase = true)) {
                            "The downloaded Expert binary failed checksum verification"
                        }
                        Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                    } finally {
                        Files.deleteIfExists(temporary)
                    }
                    if (!SystemInfo.isWindows && !destination.toFile().setExecutable(true)) {
                        error("Could not mark the downloaded Expert binary executable")
                    }
                    ApplicationManager.getApplication().invokeLater {
                        ExpertSettings.getInstance().expertPath = destination.toString()
                        (ProjectManager.getInstance().openProjects.asList() + project)
                            .distinct()
                            .filterNot { it.isDisposed || it.isDefault }
                            .forEach { openProject ->
                                LspClientManager.getInstance(openProject)
                                    .stopAndRestartClientsIfNeeded(ExpertLspServerSupportProvider::class.java)
                            }
                    }
                } catch (error: Throwable) {
                    ApplicationManager.getApplication().invokeLater { onFailure?.invoke(error) }
                } finally {
                    downloading.set(false)
                }
            }
        })
    }

    private fun fetchRelease(): Pair<String, Map<String, String>> {
        val json = JsonParser.parseString(downloadText(RELEASE_API)).asJsonObject
        val version = json.get("tag_name").asString
        val assets = json.getAsJsonArray("assets").associate { value ->
            val asset = value.asJsonObject
            asset.get("name").asString to asset.get("browser_download_url").asString
        }
        return version to assets
    }

    private fun destinationFile(assetName: String): java.nio.file.Path =
        File(PathManager.getSystemPath(), "expert/$assetName").toPath()

    private fun downloadText(url: String): String {
        val connection = open(url)
        return try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadToFile(url: String, destination: File, indicator: ProgressIndicator) {
        val connection = open(url)
        try {
            connection.inputStream.use { input ->
                destination.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        indicator.checkCanceled()
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 60_000
        setRequestProperty("User-Agent", "Yaee-Elixir-Expert-Plugin")
        instanceFollowRedirects = true
        connect()
        check(responseCode in 200..299) { "Download failed with HTTP $responseCode for $url" }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
