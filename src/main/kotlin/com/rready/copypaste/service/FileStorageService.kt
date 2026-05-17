package com.rready.copypaste.service

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path

@Service
class FileStorageService(
    @Value("\${app.storage.upload-dir:./uploads}") private val uploadDir: String
) {

    private val log = LoggerFactory.getLogger(FileStorageService::class.java)

    private lateinit var resolvedDir: Path

    @PostConstruct
    fun init() {
        resolvedDir = Path.of(uploadDir).toAbsolutePath().normalize()
        Files.createDirectories(resolvedDir)
        if (!Path.of(uploadDir).isAbsolute) {
            log.warn(
                "app.storage.upload-dir is a relative path ('{}') resolved to '{}'. " +
                "In containerized deployments use an absolute path on a mounted volume " +
                "(e.g. APP_STORAGE_UPLOAD_DIR=/var/copy-paste/uploads) — otherwise uploaded " +
                "files are lost when the container is recreated.",
                uploadDir, resolvedDir
            )
        } else {
            log.info("FileStorageService using upload dir: {}", resolvedDir)
        }
    }

    fun store(token: String, file: MultipartFile): String {
        val dest = resolvedDir.resolve(token)
        file.transferTo(dest)
        return dest.toString()
    }

    fun delete(storagePath: String) {
        Files.deleteIfExists(Path.of(storagePath))
    }

    fun resource(storagePath: String): Resource = FileSystemResource(Path.of(storagePath))
}
