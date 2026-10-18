package com.sorenkai.app.service

import com.sorenkai.admin.api.v1.AdminServiceGrpcKt
import com.sorenkai.admin.api.v1.ServiceInfoRequest
import com.sorenkai.admin.api.v1.ServiceInfoResponse
import com.sorenkai.admin.api.v1.serviceInfoResponse
import com.sorenkai.app.AppVersion
import com.sorenkai.app.DeploymentInfoProvider
import com.sorenkai.app.RuntimeInfoProvider

class AdminServiceImpl(
    private val runtimeInfoProvider: RuntimeInfoProvider,
    private val deploymentInfoProvider: DeploymentInfoProvider,
) : AdminServiceGrpcKt.AdminServiceCoroutineImplBase() {
    override suspend fun getServiceInfo(request: ServiceInfoRequest): ServiceInfoResponse =
        serviceInfoResponse {
            serviceName = "Admin"
            version = AppVersion.value
            runtime = runtimeInfoProvider.getInfo()
            deployment = deploymentInfoProvider.getInfo()
        }
}
