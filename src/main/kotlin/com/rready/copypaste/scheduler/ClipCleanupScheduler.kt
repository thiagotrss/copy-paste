package com.rready.copypaste.scheduler

import com.rready.copypaste.service.ClipService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ClipCleanupScheduler(private val clipService: ClipService) {

    private val logger = LoggerFactory.getLogger(ClipCleanupScheduler::class.java)

    @Scheduled(cron = "\${app.cleanup.cron:0 0 * * * *}")
    fun cleanup() {
        logger.info("Running expired clips cleanup...")
        clipService.deleteExpired()
        logger.info("Cleanup complete.")
    }
}
