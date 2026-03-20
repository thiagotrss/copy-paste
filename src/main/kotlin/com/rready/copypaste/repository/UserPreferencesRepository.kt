package com.rready.copypaste.repository

import com.rready.copypaste.model.UserPreferences
import org.springframework.data.mongodb.repository.MongoRepository

interface UserPreferencesRepository : MongoRepository<UserPreferences, String> {
    fun findByEmail(email: String): UserPreferences?
}
