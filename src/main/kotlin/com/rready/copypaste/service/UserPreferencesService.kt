package com.rready.copypaste.service

import com.rready.copypaste.model.UserPreferences
import com.rready.copypaste.repository.UserPreferencesRepository
import org.springframework.stereotype.Service

@Service
class UserPreferencesService(private val repo: UserPreferencesRepository) {

    fun getPreferences(email: String): UserPreferences =
        repo.findByEmail(email) ?: UserPreferences(email = email)

    fun savePreferences(email: String, alwaysPrivate: Boolean, preferredLanguage: String): UserPreferences {
        val existing = repo.findByEmail(email)
        val prefs = existing?.copy(alwaysPrivate = alwaysPrivate, preferredLanguage = preferredLanguage)
            ?: UserPreferences(email = email, alwaysPrivate = alwaysPrivate, preferredLanguage = preferredLanguage)
        return repo.save(prefs)
    }

    fun savePreferredLanguage(email: String, language: String): UserPreferences {
        val existing = repo.findByEmail(email)
        val prefs = existing?.copy(preferredLanguage = language)
            ?: UserPreferences(email = email, preferredLanguage = language)
        return repo.save(prefs)
    }
}
