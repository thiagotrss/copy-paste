package com.rready.copypaste.controller

import com.rready.copypaste.service.ClipService
import com.rready.copypaste.service.FileStorageService
import com.rready.copypaste.service.UserPreferencesService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.core.io.support.ResourceRegion
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.support.RedirectAttributes

@Controller
class ClipController(
    private val clipService: ClipService,
    private val fileStorageService: FileStorageService,
    private val userPreferencesService: UserPreferencesService,
    private val messageSource: MessageSource
) {

    private fun msg(key: String) = messageSource.getMessage(key, null, LocaleContextHolder.getLocale())

    @GetMapping("/")
    fun home(
        model: Model,
        @AuthenticationPrincipal principal: OAuth2User
    ): String {
        val email = principal.getAttribute<String>("email")!!
        model.addAttribute("userEmail", email)
        model.addAttribute("userName", principal.getAttribute<String>("name"))
        model.addAttribute("myClips", clipService.getActiveClipsForUser(email))
        model.addAttribute("prefs", userPreferencesService.getPreferences(email))
        return "index"
    }

    @PostMapping("/clip/text")
    fun createTextClip(
        @RequestParam text: String,
        @RequestParam(required = false) allowedEmails: String?,
        @AuthenticationPrincipal principal: OAuth2User,
        redirectAttributes: RedirectAttributes
    ): String {
        if (text.isBlank()) {
            redirectAttributes.addFlashAttribute("error", msg("index.error.textEmpty"))
            return "redirect:/"
        }
        val uploaderEmail = principal.getAttribute<String>("email")!!
        val emails = parseEmails(allowedEmails)
        val clip = clipService.createTextClip(uploaderEmail, text, emails)
        return "redirect:/c/${clip.token}"
    }

    @PostMapping("/clip/file")
    fun createFileClip(
        @RequestParam file: MultipartFile,
        @RequestParam(required = false) allowedEmails: String?,
        @AuthenticationPrincipal principal: OAuth2User,
        redirectAttributes: RedirectAttributes
    ): String {
        if (file.isEmpty) {
            redirectAttributes.addFlashAttribute("error", msg("index.error.fileEmpty"))
            return "redirect:/"
        }
        val uploaderEmail = principal.getAttribute<String>("email")!!
        val emails = parseEmails(allowedEmails)
        val clip = clipService.createFileClip(uploaderEmail, file, emails)
        return "redirect:/c/${clip.token}"
    }

    @GetMapping("/c/{token}")
    fun viewClip(
        @PathVariable token: String,
        model: Model,
        @AuthenticationPrincipal principal: OAuth2User,
        request: HttpServletRequest
    ): String {
        val viewerEmail = principal.getAttribute<String>("email")!!
        val clip = try {
            clipService.getClipForViewer(token, viewerEmail)
        } catch (e: AccessDeniedException) {
            model.addAttribute("error", msg("clip.error.noAccess"))
            return "error"
        } ?: run {
            model.addAttribute("error", msg("clip.error.notFound"))
            return "error"
        }

        model.addAttribute("clip", clip)
        model.addAttribute("userEmail", viewerEmail)
        model.addAttribute("shareUrl", request.requestURL.toString())
        return "clip"
    }

    @PostMapping("/c/{token}/renew")
    fun renewClip(
        @PathVariable token: String,
        @RequestParam(required = false) next: String?,
        @AuthenticationPrincipal principal: OAuth2User,
        redirectAttributes: RedirectAttributes
    ): String {
        val email = principal.getAttribute<String>("email")!!
        clipService.renewClip(token, email)
        return "redirect:" + safeNext(next, default = "/")
    }

    private fun safeNext(next: String?, default: String): String {
        if (next.isNullOrBlank()) return default
        if (!next.startsWith("/") || next.startsWith("//") || next.contains("://")) return default
        return next
    }

    @PostMapping("/c/{token}/delete")
    fun deleteClip(
        @PathVariable token: String,
        @AuthenticationPrincipal principal: OAuth2User,
        redirectAttributes: RedirectAttributes
    ): String {
        val email = principal.getAttribute<String>("email")!!
        clipService.deleteClip(token, email)
        return "redirect:/"
    }

    @GetMapping("/c/{token}/download")
    fun downloadFile(
        @PathVariable token: String,
        @AuthenticationPrincipal principal: OAuth2User,
        @RequestHeader headers: HttpHeaders
    ): ResponseEntity<ResourceRegion> {
        val viewerEmail = principal.getAttribute<String>("email")!!
        val clip = try {
            clipService.getClipForViewer(token, viewerEmail)
        } catch (e: AccessDeniedException) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        } ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        val storagePath = clip.storagePath
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        val resource = fileStorageService.resource(storagePath)
        val contentLength = resource.contentLength()
        val ranges = headers.range

        val (region, status) = if (ranges.isNotEmpty()) {
            val range = ranges[0]
            val start = range.getRangeStart(contentLength)
            val end = range.getRangeEnd(contentLength)
            ResourceRegion(resource, start, end - start + 1) to HttpStatus.PARTIAL_CONTENT
        } else {
            ResourceRegion(resource, 0, contentLength) to HttpStatus.OK
        }

        val mediaType = clip.contentType
            ?.let { runCatching { MediaType.parseMediaType(it) }.getOrNull() }
            ?: MediaType.APPLICATION_OCTET_STREAM

        return ResponseEntity.status(status)
            .contentType(mediaType)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${clip.originalFileName ?: "file"}\"")
            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
            .body(region)
    }

    private fun parseEmails(raw: String?): List<String>? {
        val emails = raw
            ?.split(",", "\n", ";")
            ?.map { it.trim().lowercase() }
            ?.filter { it.isNotBlank() }
            ?: return null
        return emails.ifEmpty { null }
    }
}
