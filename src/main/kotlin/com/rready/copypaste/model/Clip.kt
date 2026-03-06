package com.rready.copypaste.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "clips")
data class Clip(
    @Id
    val id: String? = null,

    @Indexed(unique = true)
    val token: String,

    val uploaderEmail: String,

    val type: ClipType,

    val textContent: String? = null,

    val originalFileName: String? = null,
    val storagePath: String? = null,
    val contentType: String? = null,

    // null = any authenticated user may access; non-null = restricted to listed emails
    val allowedEmails: List<String>? = null,

    @CreatedDate
    val createdAt: Instant? = null,

    @Indexed
    val expiresAt: Instant
)

enum class ClipType { TEXT, FILE }
