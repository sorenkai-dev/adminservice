package com.sorenkai.app.di

import com.sorenkai.app.ApplicationLifecycle
import com.sorenkai.app.GrpcServer
import com.sorenkai.app.metrics.GrpcObservability
import com.sorenkai.app.service.AdminServiceImpl
import com.sorenkai.core.authorization.AuthorizationService
import com.sorenkai.core.authorization.RoleManagementService
import com.sorenkai.core.cases.CaseManagementService
import com.sorenkai.core.repository.CaseLockRepository
import com.sorenkai.core.repository.CaseRepository
import com.sorenkai.core.repository.RoleAssignmentRepository
import com.sorenkai.core.transaction.TransactionManager
import com.sorenkai.persistence.DatabaseMigrator
import com.sorenkai.persistence.di.persistenceModule
import com.zaxxer.hikari.HikariDataSource
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.observation.ObservationRegistry
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.koin.dsl.koinApplication
import java.time.Clock
import javax.sql.DataSource

/**
 * Unit tests validating Koin dependency injection configuration and graph completeness.
 */
class DependencyGraphTest {
    /**
     * Verifies that all required singletons (server, services, registries, persistence) resolve successfully as singletons.
     */
    @Test
    fun `Koin resolves the Admin application dependency graph`() {
        val application =
            koinApplication {
                properties(
                    mapOf(
                        "grpc.port" to 0,
                    ),
                )

                modules(
                    persistenceModule,
                    serviceModule,
                    observabilityModule,
                    serverModule,
                )
            }

        var hikariDataSource: HikariDataSource? = null
        try {
            val koin = application.koin

            val server = koin.get<GrpcServer>()
            val adminService = koin.get<AdminServiceImpl>()
            val observability = koin.get<GrpcObservability>()
            val meterRegistry = koin.get<MeterRegistry>()
            val observationRegistry = koin.get<ObservationRegistry>()

            val dataSource = koin.get<DataSource>()
            hikariDataSource = koin.get<HikariDataSource>()
            val database = koin.get<Database>()
            val migrator = koin.get<DatabaseMigrator>()
            val txManager = koin.get<TransactionManager>()

            val roleRepo = koin.get<RoleAssignmentRepository>()
            val caseRepo = koin.get<CaseRepository>()
            val lockRepo = koin.get<CaseLockRepository>()
            val clock = koin.get<Clock>()

            val authService = koin.get<AuthorizationService>()
            val roleService = koin.get<RoleManagementService>()
            val caseService = koin.get<CaseManagementService>()

            assertNotNull(server)
            assertNotNull(adminService)
            assertNotNull(observability)
            assertNotNull(dataSource)
            assertNotNull(hikariDataSource)
            assertNotNull(database)
            assertNotNull(migrator)
            assertNotNull(txManager)
            assertNotNull(roleRepo)
            assertNotNull(caseRepo)
            assertNotNull(lockRepo)
            assertNotNull(clock)
            assertNotNull(authService)
            assertNotNull(roleService)
            assertNotNull(caseService)

            assertSame(server, koin.get<GrpcServer>())
            assertSame(adminService, koin.get<AdminServiceImpl>())
            assertSame(observability, koin.get<GrpcObservability>())
            assertSame(meterRegistry, koin.get<MeterRegistry>())
            assertSame(observationRegistry, koin.get<ObservationRegistry>())
            assertSame(dataSource, koin.get<DataSource>())
            assertSame(hikariDataSource, koin.get<HikariDataSource>())
            assertSame(database, koin.get<Database>())
            assertSame(migrator, koin.get<DatabaseMigrator>())
            assertSame(txManager, koin.get<TransactionManager>())
            assertSame(roleRepo, koin.get<RoleAssignmentRepository>())
            assertSame(caseRepo, koin.get<CaseRepository>())
            assertSame(lockRepo, koin.get<CaseLockRepository>())
            assertSame(authService, koin.get<AuthorizationService>())
            assertSame(roleService, koin.get<RoleManagementService>())
            assertSame(caseService, koin.get<CaseManagementService>())
        } finally {
            val server = application.koin.get<GrpcServer>()
            val lifecycle = ApplicationLifecycle(server, application)
            lifecycle.shutdown()
            assertTrue(hikariDataSource?.isClosed == true, "HikariDataSource should be closed on application shutdown")
        }
    }
}
