package com.rready.copypaste.controller

import com.rready.copypaste.service.UserPreferencesService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/preferences")
class PreferencesController(
    private val userPreferencesService: UserPreferencesService
) {

    @GetMapping
    fun showPreferences(
        model: Model,
        @AuthenticationPrincipal principal: OAuth2User
    ): String {
        val email = principal.getAttribute<String>("email")!!
        model.addAttribute("prefs", userPreferencesService.getPreferences(email))
        model.addAttribute("userEmail", email)
        return "preferences"
    }

    @PostMapping
    fun savePreferences(
        @RequestParam(required = false, defaultValue = "false") alwaysPrivate: Boolean,
        @AuthenticationPrincipal principal: OAuth2User
    ): String {
        val email = principal.getAttribute<String>("email")!!
        userPreferencesService.savePreferences(email, alwaysPrivate)
        return "redirect:/"
    }
}
