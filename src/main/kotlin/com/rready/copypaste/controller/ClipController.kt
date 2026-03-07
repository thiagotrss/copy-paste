package com.rready.copypaste.controller

import com.rready.copypaste.service.ClipService
import com.rready.copypaste.service.FileStorageService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.support.RedirectAttributes

@Controller
class ClipController(
    private val clipService: ClipService,
    private val fileStorageService: FileStorageService,
    private val messageSource: MessageSource
) {

    private fun msg(key: String) = messageSource.getMessage(key, null, LocaleContextHolder.getLocale())

    @GetMapping("/")
    fun home(
        model: Model,
        @AuthenticationPrincipal principal: OAuth2User
    ): String {
        model.addAttribute("userEmail", principal.getAttribute<String>("email"))
        model.addAttribute("userName", principal.getAttribute<String>("name"))
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

    @GetMapping("/c/{token}/download")
    fun downloadFile(
        @PathVariable token: String,
        @AuthenticationPrincipal principal: OAuth2User,
        response: HttpServletResponse
    ) {
        val viewerEmail = principal.getAttribute<String>("email")!!
        val clip = try {
            clipService.getClipForViewer(token, viewerEmail)
        } catch (e: AccessDeniedException) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN)
            return
        } ?: run {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            return
        }

        if (clip.storagePath == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            return
        }

        fileStorageService.stream(clip.storagePath, clip.contentType, clip.originalFileName, response)
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
