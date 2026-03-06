package com.rready.copypaste.repository

import com.rready.copypaste.model.Clip
import org.springframework.data.mongodb.repository.MongoRepository
import java.time.Instant

interface ClipRepository : MongoRepository<Clip, String> {
    fun findByToken(token: String): Clip?
    fun findByExpiresAtBefore(now: Instant): List<Clip>
}
