package com.rready.copypaste.repository

import com.rready.copypaste.model.Clip
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import java.time.Instant

interface ClipRepository : MongoRepository<Clip, String> {
    fun findByToken(token: String): Clip?
    fun findByExpiresAtBefore(now: Instant): List<Clip>

    @Query(value = "{}", fields = "{ token: 1, _id: 0 }")
    fun findAllTokens(): List<TokenProjection>

    interface TokenProjection {
        val token: String
    }
}
