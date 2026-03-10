package com.rready.copypaste.service

import com.rready.copypaste.model.Clip
import com.rready.copypaste.model.ClipType
import com.rready.copypaste.repository.ClipRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.multipart.MultipartFile
import java.time.Instant

class ClipServiceTest {

    private val clipRepository: ClipRepository = mockk()
    private val fileStorageService: FileStorageService = mockk()
    private lateinit var clipService: ClipService

    @BeforeEach
    fun setUp() {
        clipService = ClipService(clipRepository, fileStorageService, ttlHours = 24)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun clip(
        id: String? = null,
        token: String = "1",
        uploaderEmail: String = "user@example.com",
        type: ClipType = ClipType.TEXT,
        textContent: String? = "some text",
        storagePath: String? = null,
        originalFileName: String? = null,
        contentType: String? = null,
        allowedEmails: List<String>? = null,
        expiresAt: Instant = Instant.now().plusSeconds(3600)
    ) = Clip(
        id = id,
        token = token,
        uploaderEmail = uploaderEmail,
        type = type,
        textContent = textContent,
        storagePath = storagePath,
        originalFileName = originalFileName,
        contentType = contentType,
        allowedEmails = allowedEmails,
        expiresAt = expiresAt
    )

    // -------------------------------------------------------------------------
    // createTextClip — token assignment
    // -------------------------------------------------------------------------

    @Test
    fun `createTextClip assigns token 1 when database is empty`() {
        every { clipRepository.findAll() } returns emptyList()
        val slot = slot<Clip>()
        every { clipRepository.save(capture(slot)) } answers { slot.captured }

        clipService.createTextClip("user@example.com", "hello", null)

        assertEquals("1", slot.captured.token)
    }

    @Test
    fun `createTextClip assigns next sequential token when clips 1 and 2 exist`() {
        every { clipRepository.findAll() } returns listOf(clip(token = "1"), clip(token = "2"))
        val slot = slot<Clip>()
        every { clipRepository.save(capture(slot)) } answers { slot.captured }

        clipService.createTextClip("user@example.com", "hello", null)

        assertEquals("3", slot.captured.token)
    }

    @Test
    fun `createTextClip fills gap when tokens 1 and 3 exist`() {
        every { clipRepository.findAll() } returns listOf(clip(token = "1"), clip(token = "3"))
        val slot = slot<Clip>()
        every { clipRepository.save(capture(slot)) } answers { slot.captured }

        clipService.createTextClip("user@example.com", "hello", null)

        assertEquals("2", slot.captured.token)
    }

    // -------------------------------------------------------------------------
    // createTextClip — saved clip fields
    // -------------------------------------------------------------------------

    @Test
    fun `createTextClip saves clip with correct type, text and metadata`() {
        every { clipRepository.findAll() } returns emptyList()
        val slot = slot<Clip>()
        every { clipRepository.save(capture(slot)) } answers { slot.captured }
        val before = Instant.now()

        clipService.createTextClip("user@example.com", "my text", listOf("other@example.com"))

        with(slot.captured) {
            assertEquals(ClipType.TEXT, type)
            assertEquals("user@example.com", uploaderEmail)
            assertEquals("my text", textContent)
            assertEquals(listOf("other@example.com"), allowedEmails)
            assertTrue(expiresAt.isAfter(before))
        }
    }

    @Test
    fun `createTextClip returns whatever the repository returns`() {
        val savedClip = clip(id = "mongo-id", token = "1")
        every { clipRepository.findAll() } returns emptyList()
        every { clipRepository.save(any()) } returns savedClip

        val result = clipService.createTextClip("user@example.com", "hello", null)

        assertSame(savedClip, result)
    }

    // -------------------------------------------------------------------------
    // createFileClip
    // -------------------------------------------------------------------------

    @Test
    fun `createFileClip stores the file and saves clip with FILE type`() {
        val file = mockk<MultipartFile>().also {
            every { it.originalFilename } returns "report.pdf"
            every { it.contentType } returns "application/pdf"
        }
        every { clipRepository.findAll() } returns emptyList()
        every { fileStorageService.store("1", file) } returns "/uploads/1"
        val slot = slot<Clip>()
        every { clipRepository.save(capture(slot)) } answers { slot.captured }

        clipService.createFileClip("user@example.com", file, null)

        with(slot.captured) {
            assertEquals(ClipType.FILE, type)
            assertEquals("report.pdf", originalFileName)
            assertEquals("/uploads/1", storagePath)
            assertEquals("application/pdf", contentType)
        }
        verify { fileStorageService.store("1", file) }
    }

    @Test
    fun `createFileClip falls back to 'file' when originalFilename is null`() {
        val file = mockk<MultipartFile>().also {
            every { it.originalFilename } returns null
            every { it.contentType } returns "application/octet-stream"
        }
        every { clipRepository.findAll() } returns emptyList()
        every { fileStorageService.store(any(), any()) } returns "/uploads/1"
        val slot = slot<Clip>()
        every { clipRepository.save(capture(slot)) } answers { slot.captured }

        clipService.createFileClip("user@example.com", file, null)

        assertEquals("file", slot.captured.originalFileName)
    }

    // -------------------------------------------------------------------------
    // getClipForViewer
    // -------------------------------------------------------------------------

    @Test
    fun `getClipForViewer returns clip for the uploader`() {
        val clip = clip(uploaderEmail = "owner@example.com")
        every { clipRepository.findByToken("1") } returns clip

        val result = clipService.getClipForViewer("1", "owner@example.com")

        assertEquals(clip, result)
    }

    @Test
    fun `getClipForViewer allows any viewer when allowedEmails is null`() {
        val clip = clip(uploaderEmail = "owner@example.com", allowedEmails = null)
        every { clipRepository.findByToken("1") } returns clip

        val result = clipService.getClipForViewer("1", "anyone@example.com")

        assertEquals(clip, result)
    }

    @Test
    fun `getClipForViewer returns clip for email listed in allowedEmails`() {
        val clip = clip(uploaderEmail = "owner@example.com", allowedEmails = listOf("guest@example.com"))
        every { clipRepository.findByToken("1") } returns clip

        val result = clipService.getClipForViewer("1", "guest@example.com")

        assertEquals(clip, result)
    }

    @Test
    fun `getClipForViewer returns null when token not found`() {
        every { clipRepository.findByToken("99") } returns null

        val result = clipService.getClipForViewer("99", "user@example.com")

        assertNull(result)
    }

    @Test
    fun `getClipForViewer returns null when clip is expired`() {
        val expired = clip(expiresAt = Instant.now().minusSeconds(1))
        every { clipRepository.findByToken("1") } returns expired

        val result = clipService.getClipForViewer("1", "user@example.com")

        assertNull(result)
    }

    @Test
    fun `getClipForViewer throws AccessDeniedException for viewer not in allowedEmails`() {
        val clip = clip(uploaderEmail = "owner@example.com", allowedEmails = listOf("allowed@example.com"))
        every { clipRepository.findByToken("1") } returns clip

        assertThrows(AccessDeniedException::class.java) {
            clipService.getClipForViewer("1", "stranger@example.com")
        }
    }

    @Test
    fun `getClipForViewer uploader check is case-insensitive`() {
        val clip = clip(uploaderEmail = "Owner@Example.com", allowedEmails = listOf("other@example.com"))
        every { clipRepository.findByToken("1") } returns clip

        val result = clipService.getClipForViewer("1", "OWNER@EXAMPLE.COM")

        assertNotNull(result)
    }

    // -------------------------------------------------------------------------
    // deleteExpired
    // -------------------------------------------------------------------------

    @Test
    fun `deleteExpired deletes all expired clips and removes their stored files`() {
        val clipWithFile = clip(token = "1", storagePath = "/uploads/1")
        val clipTextOnly = clip(token = "2", storagePath = null)
        every { clipRepository.findByExpiresAtBefore(any()) } returns listOf(clipWithFile, clipTextOnly)
        every { fileStorageService.delete("/uploads/1") } just Runs
        every { clipRepository.delete(any()) } just Runs

        clipService.deleteExpired()

        verify(exactly = 1) { fileStorageService.delete("/uploads/1") }
        verify(exactly = 2) { clipRepository.delete(any()) }
    }

    @Test
    fun `deleteExpired does nothing when there are no expired clips`() {
        every { clipRepository.findByExpiresAtBefore(any()) } returns emptyList()

        clipService.deleteExpired()

        verify(exactly = 0) { clipRepository.delete(any()) }
        verify(exactly = 0) { fileStorageService.delete(any()) }
    }
}
