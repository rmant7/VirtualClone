package com.virtualclone.app.feature.onboarding.mapper

import com.virtualclone.app.core.common.model.ModelUi
import com.virtualclone.app.core.domain.model.DownloadStatus
import com.virtualclone.app.core.domain.model.LLMModel
import com.virtualclone.app.feature.onboarding.ModelDownloadUiStatus
import java.util.Locale

object ModelUiMapper {

    private fun idToTitle(id: String): String {
        // e.g. "gemma3_1b_it_cpu" -> "Gemma3 1b It Cpu"
        return id.split('_')
            .joinToString(" ") { part ->
                part.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
    }

    fun toUi(domain: LLMModel): ModelUi = ModelUi(
        id = domain.id,
        displayName = idToTitle(domain.id),
        needsAuth = domain.needsAuth,
        thinking = domain.thinking,
        defaultTemperature = domain.defaultTemperature,
        defaultTopK = domain.defaultTopK,
        defaultTopP = domain.defaultTopP,
        preferredBackend = domain.preferredBackend
    )

    fun toUiList(list: List<LLMModel>): List<ModelUi> = list.map { toUi(it) }

    fun mapDomainStatusToUi(
        domainStatus: DownloadStatus,
        isValid: Boolean
    ): ModelDownloadUiStatus {
        return when (domainStatus) {

            DownloadStatus.NOT_STARTED ->
                ModelDownloadUiStatus.NOT_STARTED

            DownloadStatus.DOWNLOADING ->
                ModelDownloadUiStatus.DOWNLOADING

            DownloadStatus.PAUSED ->
                ModelDownloadUiStatus.PAUSED

            DownloadStatus.FAILED ->
                ModelDownloadUiStatus.FAILED

            DownloadStatus.DELETED ->
                ModelDownloadUiStatus.DELETED

            DownloadStatus.COMPLETED ->
                ModelDownloadUiStatus.COMPLETED
        }
    }
}