package com.rready.copypaste.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "user_preferences")
data class UserPreferences(
    @Id
    val id: String? = null,
    @Indexed(unique = true)
    val email: String,
    val alwaysPrivate: Boolean = false
)
