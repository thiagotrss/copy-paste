package com.rready.copypaste.service

import com.rready.copypaste.model.UserPreferences
import com.rready.copypaste.repository.UserPreferencesRepository
import org.springframework.stereotype.Service

@Service
class UserPreferencesService(private val repo: UserPreferencesRepository) {

    fun getPreferences(email: String): UserPreferences =
        repo.findByEmail(email) ?: UserPreferences(email = email)

    fun savePreferences(email: String, alwaysPrivate: Boolean): UserPreferences {
        val existing = repo.findByEmail(email)
        val prefs = existing?.copy(alwaysPrivate = alwaysPrivate)
            ?: UserPreferences(email = email, alwaysPrivate = alwaysPrivate)
        return repo.save(prefs)
    }
}
