package com.rready.copypaste.service

import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
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

    fun stream(storagePath: String, contentType: String?, fileName: String?, response: HttpServletResponse) {
        response.contentType = contentType ?: "application/octet-stream"
        response.setHeader("Content-Disposition", "attachment; filename=\"${fileName ?: "file"}\"")
        response.setContentLengthLong(Files.size(Path.of(storagePath)))
        Files.copy(Path.of(storagePath), response.outputStream)
    }
}
