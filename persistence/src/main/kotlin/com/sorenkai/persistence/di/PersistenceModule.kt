package com.sorenkai.persistence.di

import com.sorenkai.core.authorization.AuthorizationService
import com.sorenkai.core.authorization.DefaultAuthorizationService
import com.sorenkai.core.authorization.DefaultRoleManagementService
import com.sorenkai.core.authorization.RoleManagementService
import com.sorenkai.core.cases.CaseManagementService
import com.sorenkai.core.cases.DefaultCaseManagementService
import com.sorenkai.core.repository.CaseLockRepository
import com.sorenkai.core.repository.CaseRepository
import com.sorenkai.core.repository.RoleAssignmentRepository
import com.sorenkai.core.transaction.TransactionManager
import com.sorenkai.persistence.DatabaseConfig
import com.sorenkai.persistence.DatabaseMigrator
import com.sorenkai.persistence.PostgresDataSourceFactory
import com.sorenkai.persistence.repository.PostgresCaseLockRepository
import com.sorenkai.persistence.repository.PostgresCaseRepository
import com.sorenkai.persistence.repository.PostgresRoleAssignmentRepository
import com.sorenkai.persistence.transaction.ExposedTransactionManager
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.dsl.module
import org.koin.dsl.onClose
import java.time.Clock
import javax.sql.DataSource

/**
 * Koin module configuring persistence infrastructure, database connection pooling,
 * Flyway migrations, transaction manager, repository bindings, and core domain services.
 */
val persistenceModule =
    module {
        single { DatabaseConfig() }

        single<HikariDataSource> {
            PostgresDataSourceFactory.create(get())
        } onClose { dataSource ->
            dataSource?.close()
        }

        single<DataSource> { get<HikariDataSource>() }

        single {
            Database.connect(get<DataSource>())
        }

        single {
            DatabaseMigrator(get())
        }

        single<TransactionManager> {
            ExposedTransactionManager(get())
        }

        single<RoleAssignmentRepository> {
            PostgresRoleAssignmentRepository(get<Database>())
        }

        single<CaseRepository> {
            PostgresCaseRepository(get<Database>())
        }

        single<CaseLockRepository> {
            PostgresCaseLockRepository(get<Database>())
        }

        single<Clock> { Clock.systemUTC() }

        single<AuthorizationService> {
            DefaultAuthorizationService(
                repository = get(),
            )
        }

        single<RoleManagementService> {
            DefaultRoleManagementService(
                repository = get(),
            )
        }

        single<CaseManagementService> {
            DefaultCaseManagementService(
                caseRepository = get(),
                lockRepository = get(),
                authorizationService = get(),
                tx = get(),
                clock = get(),
            )
        }
    }
