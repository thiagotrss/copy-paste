package com.rready.copypaste.service

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

    fun store(token: String, file: MultipartFile): String {
        val dir = Path.of(uploadDir)
        Files.createDirectories(dir)
        val dest = dir.resolve(token)
        file.transferTo(dest)
        return dest.toAbsolutePath().toString()
    }

    fun delete(storagePath: String) {
        Files.deleteIfExists(Path.of(storagePath))
    }

    fun resource(storagePath: String): Resource = FileSystemResource(Path.of(storagePath))
}
