package com.sorenkai.app

import com.sorenkai.admin.api.v1.DeploymentInfo
import com.sorenkai.admin.api.v1.deploymentInfo
import java.util.UUID

/**
 * Provider for deployment environment and instance identification metadata.
 *
 * Populates deployment metadata from environment variables such as `ENV` and `K_REVISION`
 * and assigns a unique UUID to each running process instance.
 */
class DeploymentInfoProvider {
    private val processId = UUID.randomUUID().toString()

    /**
     * Resolves and returns current deployment metadata.
     *
     * @return A [DeploymentInfo] protobuf message containing the active environment name,
     * container/revision tag, and process instance ID.
     */
    fun getInfo(): DeploymentInfo =
        deploymentInfo {
            environment = System.getenv("ENV") ?: "LOCAL"
            revision = System.getenv("K_REVISION") ?: "local"
            instanceId = processId
        }
}
