package com.rready.copypaste.config

import com.rready.copypaste.service.UserPreferencesService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.web.servlet.HandlerInterceptor

class LocalePreferenceInterceptor(
    private val userPreferencesService: UserPreferencesService
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val lang = request.getParameter("lang") ?: return true
        val auth = SecurityContextHolder.getContext().authentication
        val principal = auth?.principal as? OAuth2User ?: return true
        val email = principal.getAttribute<String>("email") ?: return true
        userPreferencesService.savePreferredLanguage(email, lang)
        return true
    }
}
