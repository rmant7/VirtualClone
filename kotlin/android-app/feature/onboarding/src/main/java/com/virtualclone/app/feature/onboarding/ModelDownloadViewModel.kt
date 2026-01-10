package com.virtualclone.app.feature.onboarding

import android.content.Context
import com.virtualclone.app.core.common.Logger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.virtualclone.app.core.domain.model.DownloadStatus
import com.virtualclone.app.core.domain.usecase.download.*
import com.virtualclone.app.core.domain.usecase.model.*
import com.virtualclone.app.feature.onboarding.mapper.ModelUiMapper
import com.virtualclone.app.feature.onboarding.mapper.ModelUiMapper.mapDomainStatusToUi
import com.virtualclone.app.core.common.model.ModelUi
import com.virtualclone.app.feature.onboarding.ui.ModelDownloadUiItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import com.virtualclone.app.core.common.Result
import com.virtualclone.app.core.common.UiEvent
import com.virtualclone.app.core.domain.repository.HfTokenProvider
import com.virtualclone.app.core.domain.repository.NetworkMonitor
import com.virtualclone.app.core.domain.util.NetworkUtils
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
class ModelDownloadViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getAvailableModels: GetAvailableModelsUseCase,
    private val getAllModelDownloads: GetAllModelDownloadsUseCase,
    private val startModelDownload: StartModelDownloadUseCase,
    private val pauseModelDownload: PauseModelDownloadUseCase,
    private val resumeModelDownload: ResumeModelDownloadUseCase,
    private val cancelModelDownload: CancelModelDownloadUseCase,
    private val observeDownloadProgress: ObserveModelDownloadProgressUseCase,
    private val observeModelMetadataUseCase: ObserveModelMetadataUseCase,
    private val modelExists: ModelExistsUseCase,
    private val hfTokenProvider: HfTokenProvider,
    private val initializeModelMetadataUseCase: InitializeModelMetadataUseCase,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val tag = ModelDownloadViewModel::class.java.simpleName

    private val _uiState = MutableStateFlow<DownloadUiState>(DownloadUiState.Idle)
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events = _events.asSharedFlow()

    private val selectedIds = mutableSetOf<String>()
    private var allModels: List<ModelUi> = emptyList()

    // Cache to avoid re-checking filesystem repeatedly
    private val validityCache = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    init {
        observeModelMetadata()
        loadModels()
        observeNetworkChanges()
    }

    // -----------------------------------------------------------
    // NETWORK
    // -----------------------------------------------------------

    /**
     * Network is ONLY used to trigger metadata refresh.
     * UI never blocks on network availability.
     */
    private fun observeNetworkChanges() = viewModelScope.launch {
        networkMonitor.isOnline
            .distinctUntilChanged()
            .filter { it }
            .collect {
                Logger.i("Internet available → refreshing visible model metadata", tag)
                refreshAllVisibleModelMetadata()
            }
    }

    /**
     * Centralized internet check with UI feedback.
     */
    private suspend fun requireInternetOrNotify(): Boolean {
        if (!NetworkUtils.isInternetAvailable(context)) {
            _events.emit(UiEvent.ShowError("No internet connection"))
            return false
        }
        return true
    }

    // -----------------------------------------------------------
    // LOAD MODELS (OFFLINE SAFE)
    // -----------------------------------------------------------

    private fun loadModels() = viewModelScope.launch {
        when (val result = getAvailableModels()) {
            is Result.Success -> {
                allModels = ModelUiMapper.toUiList(result.data)

                Logger.i("Loaded ${allModels.size} models", tag)

                _uiState.value = DownloadUiState.Loaded(
                    userSelected = selectedIds.toSet(),
                    allModels = allModels,
                    statusMap = allModels.associate {
                        it.id to ModelDownloadUiItem(it.id)
                    },
                    completed = emptySet()
                )

                preloadValidity()
                subscribeToProgress()

                // Trigger metadata fetch ONCE screen opens
                refreshAllVisibleModelMetadata()
            }

            is Result.Error -> {
                _events.emit(UiEvent.ShowError("Failed to load model catalog"))
            }

            Result.Loading -> Unit
        }
    }

    // -----------------------------------------------------------
    // FILE EXISTENCE (NO INTERNET)
    // -----------------------------------------------------------

    private fun preloadValidity() = viewModelScope.launch(Dispatchers.IO) {
        validityCache.value = allModels.associate {
            it.id to modelExists(it.id)
        }
    }

    // -----------------------------------------------------------
    // METADATA FETCH (THE FIX)
    // -----------------------------------------------------------

    /**
     * Called:
     * 1. When screen opens
     * 2. When internet becomes available
     *
     * SAFE:
     * - Does nothing offline
     * - Uses Room cache
     * - HF-token aware
     */
    private fun refreshAllVisibleModelMetadata() = viewModelScope.launch {
        if (!requireInternetOrNotify()) return@launch
        if (allModels.isEmpty()) return@launch

        Logger.i("Refreshing metadata for ${allModels.size} models", tag)

        allModels.forEach { model ->
            initializeModelMetadataUseCase(listOf(model.id))
        }
    }

    // -----------------------------------------------------------
    // RESTORE UI STATE (SINGLE SOURCE = ROOM)
    // -----------------------------------------------------------

    private fun restoreDownloadStates() = viewModelScope.launch(Dispatchers.IO) {

        val metadataMap = observeModelMetadataUseCase()
            .first()
            .associateBy { it.modelId }

        val stored = when (val result = getAllModelDownloads()) {
            is Result.Success -> result.data
            else -> return@launch
        }

        val validity = validityCache.value
        val statusMap = mutableMapOf<String, ModelDownloadUiItem>()
        val completedSet = mutableSetOf<String>()

        Logger.i("restoreDownloadStates(): metadata=${metadataMap.keys}", tag)

        for (uiModel in allModels) {

            val record = stored.find { it.modelId == uiModel.id }
            val meta = metadataMap[uiModel.id]
            val exists = validity[uiModel.id] == true

            val uiStatus = mapDomainStatusToUi(
                record?.status ?: DownloadStatus.NOT_STARTED,
                exists
            )

            val totalBytes =
                record?.totalBytes
                    ?: meta?.totalBytes
                    ?: -1L

            Logger.i(
                "Model=${uiModel.id}, totalBytes=$totalBytes",
                tag
            )

            statusMap[uiModel.id] = ModelDownloadUiItem(
                modelId = uiModel.id,
                bytesDownloaded = record?.bytesDownloaded ?: 0L,
                totalBytes = totalBytes,
                status = uiStatus
            )

            if (uiStatus == ModelDownloadUiStatus.COMPLETED) {
                completedSet.add(uiModel.id)
            }
        }

        _uiState.value = DownloadUiState.Loaded(
            userSelected = selectedIds.toSet(),
            allModels = allModels,
            statusMap = statusMap,
            completed = completedSet
        )
    }

    // -----------------------------------------------------------
    // METADATA OBSERVER
    // -----------------------------------------------------------

    private fun observeModelMetadata() = viewModelScope.launch {
        observeModelMetadataUseCase().collectLatest { list ->
            Logger.i("Metadata updated → ${list.size} items", tag)
            restoreDownloadStates()
        }
    }

    // -----------------------------------------------------------
    // DOWNLOAD PROGRESS
    // -----------------------------------------------------------

    private fun subscribeToProgress() = viewModelScope.launch {
        allModels.forEach { model ->
            launch {
                observeDownloadProgress(model.id).collect {
                    restoreDownloadStates()
                }
            }
        }
    }

    // -----------------------------------------------------------
    // USER ACTIONS
    // -----------------------------------------------------------

    fun startDownloads() = viewModelScope.launch {
        if (!requireInternetOrNotify()) return@launch

        val state = uiState.value as? DownloadUiState.Loaded ?: return@launch
        val targets = state.userSelected.filter {
            state.statusMap[it]?.status in setOf(
                ModelDownloadUiStatus.NOT_STARTED,
                ModelDownloadUiStatus.PAUSED,
                ModelDownloadUiStatus.FAILED,
                ModelDownloadUiStatus.DELETED
            )
        }

        targets.forEach { id ->
            startModelDownload(id)
        }
    }

    fun pause(modelId: String) = viewModelScope.launch {
        pauseModelDownload(modelId)
    }

    fun resume(modelId: String) = viewModelScope.launch {
        if (!requireInternetOrNotify()) return@launch
        resumeModelDownload(modelId)
    }

    fun cancel(modelId: String) = viewModelScope.launch {
        cancelModelDownload(modelId)
        selectedIds.remove(modelId)
        restoreDownloadStates()
    }

    fun saveHfToken(token: String) = viewModelScope.launch {
        hfTokenProvider.saveToken(token)
        refreshAllVisibleModelMetadata()
    }

    fun toggleSelection(modelId: String, selected: Boolean) {
        if (selected) selectedIds.add(modelId) else selectedIds.remove(modelId)
        restoreDownloadStates()
    }
}