package com.sorenkai.persistence.repository

import com.sorenkai.core.model.AdministrativeCase
import com.sorenkai.core.model.CaseId
import com.sorenkai.core.model.CaseSubject
import com.sorenkai.core.model.IdentityId
import com.sorenkai.core.model.LeaseToken
import com.sorenkai.core.repository.LockAcquisitionResult
import com.sorenkai.persistence.PostgresIntegrationTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * PostgreSQL integration tests for [PostgresCaseLockRepository] covering atomic lease acquisition,
 * concurrency guarantees with separate connections, renewal, expiration recovery, and release semantics.
 */
class PostgresCaseLockRepositoryTest : PostgresIntegrationTestBase() {
    private lateinit var caseRepository: PostgresCaseRepository
    private lateinit var lockRepository: PostgresCaseLockRepository

    private val caseId = CaseId("case-lock-100")
    private val admin1 = IdentityId("admin-1")
    private val admin2 = IdentityId("admin-2")
    private val baseTime = Instant.now().truncatedTo(ChronoUnit.MILLIS)

    @BeforeEach
    fun initRepositories() {
        caseRepository = PostgresCaseRepository(dataSource)
        lockRepository = PostgresCaseLockRepository(dataSource)

        caseRepository.save(
            AdministrativeCase(
                id = caseId,
                createdBy = IdentityId("moderator-1"),
                createdAt = baseTime,
                subject = CaseSubject("subject-lock-test"),
                reason = "Testing locking behavior.",
            ),
        )
    }

    @Test
    fun `atomically acquires lock when no lock exists`() {
        val token = LeaseToken.generate()
        val expiresAt = baseTime.plusSeconds(900)

        val result =
            lockRepository.tryAcquire(
                caseId = caseId,
                holderId = admin1,
                token = token,
                acquiredAt = baseTime,
                expiresAt = expiresAt,
                now = baseTime,
            )

        assertTrue(result is LockAcquisitionResult.Acquired)
        val lock = (result as LockAcquisitionResult.Acquired).lock
        assertEquals(caseId, lock.caseId)
        assertEquals(admin1, lock.holderId)
        assertEquals(token, lock.token)
        assertEquals(baseTime, lock.acquiredAt)
        assertEquals(expiresAt, lock.expiresAt)

        val stored = lockRepository.findLock(caseId)
        assertEquals(lock, stored)
    }

    @Test
    fun `rejects competing acquisition when active unexpired lock exists`() {
        val token1 = LeaseToken.generate()
        val expiresAt = baseTime.plusSeconds(900)

        val result1 =
            lockRepository.tryAcquire(
                caseId = caseId,
                holderId = admin1,
                token = token1,
                acquiredAt = baseTime,
                expiresAt = expiresAt,
                now = baseTime,
            )
        assertTrue(result1 is LockAcquisitionResult.Acquired)

        val token2 = LeaseToken.generate()
        val result2 =
            lockRepository.tryAcquire(
                caseId = caseId,
                holderId = admin2,
                token = token2,
                acquiredAt = baseTime.plusSeconds(10),
                expiresAt = baseTime.plusSeconds(910),
                now = baseTime.plusSeconds(10),
            )

        assertTrue(result2 is LockAcquisitionResult.AlreadyLocked)
        val activeLock = (result2 as LockAcquisitionResult.AlreadyLocked).activeLock
        assertEquals(admin1, activeLock.holderId)
        assertEquals(token1, activeLock.token)
    }

    @Test
    fun `allows replacement of expired lock by another administrator`() {
        val token1 = LeaseToken.generate()
        val expiresAt = baseTime.plusSeconds(60)

        lockRepository.tryAcquire(
            caseId = caseId,
            holderId = admin1,
            token = token1,
            acquiredAt = baseTime,
            expiresAt = expiresAt,
            now = baseTime,
        )

        val afterExpiration = baseTime.plusSeconds(70)
        val token2 = LeaseToken.generate()
        val newExpiresAt = afterExpiration.plusSeconds(900)

        val result2 =
            lockRepository.tryAcquire(
                caseId = caseId,
                holderId = admin2,
                token = token2,
                acquiredAt = afterExpiration,
                expiresAt = newExpiresAt,
                now = afterExpiration,
            )

        assertTrue(result2 is LockAcquisitionResult.Acquired)
        val newLock = (result2 as LockAcquisitionResult.Acquired).lock
        assertEquals(admin2, newLock.holderId)
        assertEquals(token2, newLock.token)
        assertEquals(afterExpiration, newLock.acquiredAt)
        assertEquals(newExpiresAt, newLock.expiresAt)
    }

    @Test
    fun `renews active lock with valid token and holder`() {
        val token = LeaseToken.generate()
        val expiresAt = baseTime.plusSeconds(300)

        lockRepository.tryAcquire(
            caseId = caseId,
            holderId = admin1,
            token = token,
            acquiredAt = baseTime,
            expiresAt = expiresAt,
            now = baseTime,
        )

        val renewalTime = baseTime.plusSeconds(100)
        val newExpiresAt = renewalTime.plusSeconds(600)

        val renewed =
            lockRepository.tryRenew(
                caseId = caseId,
                holderId = admin1,
                token = token,
                renewedAt = renewalTime,
                newExpiresAt = newExpiresAt,
                now = renewalTime,
            )

        assertNotNull(renewed)
        assertEquals(renewalTime, renewed?.acquiredAt)
        assertEquals(newExpiresAt, renewed?.expiresAt)
        assertEquals(token, renewed?.token)
        assertEquals(admin1, renewed?.holderId)
    }

    @Test
    fun `rejects renewal with stale or mismatched token or wrong holder`() {
        val token = LeaseToken.generate()
        val expiresAt = baseTime.plusSeconds(300)

        lockRepository.tryAcquire(
            caseId = caseId,
            holderId = admin1,
            token = token,
            acquiredAt = baseTime,
            expiresAt = expiresAt,
            now = baseTime,
        )

        val wrongToken = LeaseToken.generate()
        val renewalTime = baseTime.plusSeconds(50)

        val rejectedWrongToken =
            lockRepository.tryRenew(
                caseId = caseId,
                holderId = admin1,
                token = wrongToken,
                renewedAt = renewalTime,
                newExpiresAt = renewalTime.plusSeconds(600),
                now = renewalTime,
            )
        assertNull(rejectedWrongToken)

        val rejectedWrongHolder =
            lockRepository.tryRenew(
                caseId = caseId,
                holderId = admin2,
                token = token,
                renewedAt = renewalTime,
                newExpiresAt = renewalTime.plusSeconds(600),
                now = renewalTime,
            )
        assertNull(rejectedWrongHolder)
    }

    @Test
    fun `releases lock with matching holder and token`() {
        val token = LeaseToken.generate()
        lockRepository.tryAcquire(
            caseId = caseId,
            holderId = admin1,
            token = token,
            acquiredAt = baseTime,
            expiresAt = baseTime.plusSeconds(300),
            now = baseTime,
        )

        val released = lockRepository.releaseLock(caseId, admin1, token)
        assertTrue(released)
        assertNull(lockRepository.findLock(caseId))
    }

    @Test
    fun `rejects release with stale or mismatched token or wrong holder`() {
        val token = LeaseToken.generate()
        lockRepository.tryAcquire(
            caseId = caseId,
            holderId = admin1,
            token = token,
            acquiredAt = baseTime,
            expiresAt = baseTime.plusSeconds(300),
            now = baseTime,
        )

        val wrongToken = LeaseToken.generate()
        assertFalse(lockRepository.releaseLock(caseId, admin1, wrongToken))
        assertFalse(lockRepository.releaseLock(caseId, admin2, token))
        assertNotNull(lockRepository.findLock(caseId))
    }

    @Test
    fun `forceRelease removes any lock unconditionally`() {
        val token = LeaseToken.generate()
        lockRepository.tryAcquire(
            caseId = caseId,
            holderId = admin1,
            token = token,
            acquiredAt = baseTime,
            expiresAt = baseTime.plusSeconds(300),
            now = baseTime,
        )

        lockRepository.forceRelease(caseId)
        assertNull(lockRepository.findLock(caseId))
    }

    @Test
    fun `concurrent lock acquisitions on separate connections guarantee exactly one winner`() {
        val threadCount = 6
        val executor = Executors.newFixedThreadPool(threadCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)

        val acquiredCount = AtomicInteger(0)
        val lockedCount = AtomicInteger(0)

        for (i in 1..threadCount) {
            executor.submit {
                try {
                    startLatch.await()
                    val repo = PostgresCaseLockRepository(dataSource)
                    val token = LeaseToken.generate()
                    val result =
                        repo.tryAcquire(
                            caseId = caseId,
                            holderId = IdentityId("admin-$i"),
                            token = token,
                            acquiredAt = baseTime,
                            expiresAt = baseTime.plusSeconds(900),
                            now = baseTime,
                        )
                    when (result) {
                        is LockAcquisitionResult.Acquired -> acquiredCount.incrementAndGet()
                        is LockAcquisitionResult.AlreadyLocked -> lockedCount.incrementAndGet()
                    }
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS))
        executor.shutdown()

        assertEquals(1, acquiredCount.get())
        assertEquals(threadCount - 1, lockedCount.get())
    }
}
