package com.rready.copypaste.service

import com.rready.copypaste.model.Clip
import com.rready.copypaste.model.ClipType
import com.rready.copypaste.repository.ClipRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.time.Instant

@Service
class ClipService(
    private val clipRepository: ClipRepository,
    private val fileStorageService: FileStorageService,
    @Value("\${app.clip.ttl-hours:24}") private val ttlHours: Long
) {

    fun createTextClip(uploaderEmail: String, text: String, allowedEmails: List<String>?): Clip {
        val token = nextToken()
        val clip = Clip(
            token = token,
            uploaderEmail = uploaderEmail,
            type = ClipType.TEXT,
            textContent = text,
            allowedEmails = allowedEmails,
            expiresAt = Instant.now().plusSeconds(ttlHours * 3600)
        )
        return clipRepository.save(clip)
    }

    fun createFileClip(uploaderEmail: String, file: MultipartFile, allowedEmails: List<String>?): Clip {
        val token = nextToken()
        val storagePath = fileStorageService.store(token, file)
        val clip = Clip(
            token = token,
            uploaderEmail = uploaderEmail,
            type = ClipType.FILE,
            originalFileName = file.originalFilename ?: "file",
            storagePath = storagePath,
            contentType = file.contentType,
            allowedEmails = allowedEmails,
            expiresAt = Instant.now().plusSeconds(ttlHours * 3600)
        )
        return clipRepository.save(clip)
    }

    fun getClipForViewer(token: String, viewerEmail: String): Clip? {
        val clip = clipRepository.findByToken(token) ?: return null
        if (clip.expiresAt.isBefore(Instant.now())) return null
        if (!isAllowed(clip, viewerEmail)) throw AccessDeniedException("You don't have access to this clip.")
        return clip
    }

    fun deleteExpired() {
        val expired = clipRepository.findByExpiresAtBefore(Instant.now())
        expired.forEach { clip ->
            clip.storagePath?.let { fileStorageService.delete(it) }
            clipRepository.delete(clip)
        }
    }

    private fun isAllowed(clip: Clip, viewerEmail: String): Boolean {
        val allowed = clip.allowedEmails ?: return true
        return viewerEmail.lowercase() in allowed || viewerEmail.lowercase() == clip.uploaderEmail.lowercase()
    }

    private fun nextToken(): String {
        val activeTokens = clipRepository.findAllTokens()
            .mapNotNull { it.token.toIntOrNull() }
            .toSet()
        var candidate = 1
        while (candidate in activeTokens) candidate++
        return candidate.toString()
    }
}
