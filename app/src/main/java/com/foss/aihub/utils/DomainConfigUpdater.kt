package com.foss.aihub.utils

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

object ConfigUpdater {
    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000L
            connectTimeoutMillis = 10_000L
            socketTimeoutMillis = 15_000L
        }
    }

    suspend fun updateBothIfNeeded(context: Context): Pair<Boolean, Boolean> =
        withContext(Dispatchers.IO) {
            val domainMessage = updateDomainRules(context)
            val servicesMessage = updateAiServices(context)
            domainMessage to servicesMessage
        }

    private suspend fun <T> updateConfig(
        context: Context,
        fileName: String,
        decode: (String) -> T,
        getCurrentVersion: (SettingsManager) -> String?,
        getRemoteVersion: (T) -> String,
        saveAndProcess: (SettingsManager, T, Context) -> Unit
    ): Boolean {
        val response = client.get(CLOUD_BASE_URL + fileName)
        if (!response.status.isSuccess()) {
            throw io.ktor.client.plugins.ResponseException(response, "HTTP ${response.status}")
        }

        val json = response.bodyAsText()
        val remote = decode(json)

        val manager = SettingsManager(context)
        val current = getCurrentVersion(manager) ?: "0.0.0"

        if (getRemoteVersion(remote) == current) {
            return false
        }

        saveAndProcess(manager, remote, context)
        return true
    }

    private suspend fun updateDomainRules(context: Context): Boolean = updateConfig(
        context = context,
        fileName = DOMAIN_AND_RULES_FILE,
        decode = { Json.decodeFromString<RemoteDomainConfig>(it) },
        getCurrentVersion = SettingsManager::getDomainConfigVersion,
        getRemoteVersion = { it.version },
        saveAndProcess = { manager, remote, _ ->
            manager.saveDomainConfig(
                version = remote.version,
                serviceDomains = remote.service_domains,
                alwaysBlockedDomains = remote.always_blocked_domains,
                commonAuthDomains = remote.common_auth_domains,
                trackingParams = remote.tracking_params
            )
        },
    )

    private suspend fun updateAiServices(context: Context): Boolean = updateConfig(
        context = context,
        fileName = AI_SERVICES_FILE,
        decode = { Json.decodeFromString<AiServiceConfig>(it) },
        getCurrentVersion = SettingsManager::getAiVersion,
        getRemoteVersion = { it.version },
        saveAndProcess = { manager, remote, ctx ->
            manager.saveAiServices(
                version = remote.version, aiServices = remote.ai_services
            )
            manager.cleanupAndFixServices(ctx)
        },
    )
}