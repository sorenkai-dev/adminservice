package com.sorenkai.core.repository

import com.sorenkai.core.model.CaseId
import com.sorenkai.core.model.IdentityId
import com.sorenkai.core.model.LeaseToken
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

/**
 * Unit tests verifying [InMemoryCaseLockRepository] atomic lease acquisition, renewal, and release.
 */
class InMemoryCaseLockRepositoryTest {
    private lateinit var repository: InMemoryCaseLockRepository

    private val caseId = CaseId("case-100")
    private val admin1 = IdentityId("admin-1")
    private val admin2 = IdentityId("admin-2")
    private val token1 = LeaseToken("token-1")
    private val token2 = LeaseToken("token-2")

    private val t0 = Instant.parse("2026-10-30T10:00:00Z")
    private val t10 = t0.plus(Duration.ofMinutes(10))

    @BeforeEach
    fun setUp() {
        repository = InMemoryCaseLockRepository()
    }

    @Test
    fun `acquires lock on unlocked case`() {
        val result = repository.tryAcquire(caseId, admin1, token1, t0, t10, t0)

        assertInstanceOf(LockAcquisitionResult.Acquired::class.java, result)
        val acquired = (result as LockAcquisitionResult.Acquired).lock
        assertEquals(caseId, acquired.caseId)
        assertEquals(admin1, acquired.holderId)
        assertEquals(token1, acquired.token)
        assertEquals(t0, acquired.acquiredAt)
        assertEquals(t10, acquired.expiresAt)
    }

    @Test
    fun `prevents second acquisition while lock is active`() {
        repository.tryAcquire(caseId, admin1, token1, t0, t10, t0)

        val secondResult = repository.tryAcquire(caseId, admin2, token2, t0, t10, t0)
        assertInstanceOf(LockAcquisitionResult.AlreadyLocked::class.java, secondResult)
        val locked = (secondResult as LockAcquisitionResult.AlreadyLocked).activeLock
        assertEquals(admin1, locked.holderId)
    }

    @Test
    fun `allows acquisition when previous lock has expired`() {
        repository.tryAcquire(caseId, admin1, token1, t0, t10, t0)

        val t15 = t0.plus(Duration.ofMinutes(15))
        val t25 = t0.plus(Duration.ofMinutes(25))

        val result = repository.tryAcquire(caseId, admin2, token2, t15, t25, t15)
        assertInstanceOf(LockAcquisitionResult.Acquired::class.java, result)
        val acquired = (result as LockAcquisitionResult.Acquired).lock
        assertEquals(admin2, acquired.holderId)
        assertEquals(token2, acquired.token)
    }

    @Test
    fun `renews active lock when token and holder match`() {
        repository.tryAcquire(caseId, admin1, token1, t0, t10, t0)

        val t5 = t0.plus(Duration.ofMinutes(5))
        val t20 = t0.plus(Duration.ofMinutes(20))

        val renewed = repository.tryRenew(caseId, admin1, token1, t5, t20, t5)
        assertNotNull(renewed)
        assertEquals(admin1, renewed?.holderId)
        assertEquals(token1, renewed?.token)
        assertEquals(t5, renewed?.acquiredAt)
        assertEquals(t20, renewed?.expiresAt)
    }

    @Test
    fun `fails renewal if token or holder does not match`() {
        repository.tryAcquire(caseId, admin1, token1, t0, t10, t0)

        val t5 = t0.plus(Duration.ofMinutes(5))
        val t20 = t0.plus(Duration.ofMinutes(20))

        assertNull(repository.tryRenew(caseId, admin2, token1, t5, t20, t5))
        assertNull(repository.tryRenew(caseId, admin1, token2, t5, t20, t5))
    }

    @Test
    fun `fails renewal if lock is expired`() {
        repository.tryAcquire(caseId, admin1, token1, t0, t10, t0)

        val t15 = t0.plus(Duration.ofMinutes(15))
        val t25 = t0.plus(Duration.ofMinutes(25))

        assertNull(repository.tryRenew(caseId, admin1, token1, t15, t25, t15))
    }

    @Test
    fun `releases lock only when holder and token match`() {
        repository.tryAcquire(caseId, admin1, token1, t0, t10, t0)

        assertFalse(repository.releaseLock(caseId, admin2, token1))
        assertFalse(repository.releaseLock(caseId, admin1, token2))
        assertNotNull(repository.findLock(caseId))

        assertTrue(repository.releaseLock(caseId, admin1, token1))
        assertNull(repository.findLock(caseId))
    }

    @Test
    fun `forceRelease clears lock unconditionally`() {
        repository.tryAcquire(caseId, admin1, token1, t0, t10, t0)
        assertNotNull(repository.findLock(caseId))

        repository.forceRelease(caseId)
        assertNull(repository.findLock(caseId))
    }
}
