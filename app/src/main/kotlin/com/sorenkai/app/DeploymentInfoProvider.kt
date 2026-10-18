package com.sorenkai.app

import com.sorenkai.admin.api.v1.DeploymentInfo
import com.sorenkai.admin.api.v1.deploymentInfo
import java.util.UUID

class DeploymentInfoProvider {
    private val processId = UUID.randomUUID().toString()

    fun getInfo(): DeploymentInfo =
        deploymentInfo {
            environment = System.getenv("ENV") ?: "LOCAL"
            revision = System.getenv("K_REVISION") ?: "local"
            instanceId = processId
        }
}
