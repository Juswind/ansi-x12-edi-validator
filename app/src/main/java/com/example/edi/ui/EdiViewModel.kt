package com.example.edi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edi.model.EdiParseResult
import com.example.edi.model.EdiSamples
import com.example.edi.parser.EdiValidatorParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EdiViewModel : ViewModel() {

    private val parser = EdiValidatorParser()

    private val _rawEdiText = MutableStateFlow("")
    val rawEdiText: StateFlow<String> = _rawEdiText.asStateFlow()

    private val _parseResult = MutableStateFlow<EdiParseResult?>(null)
    val parseResult: StateFlow<EdiParseResult?> = _parseResult.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _selectedSampleId = MutableStateFlow<String?>(null)
    val selectedSampleId: StateFlow<String?> = _selectedSampleId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        // Initialize with default 850 Purchase Order sample
        loadSample("850_sample")
    }

    fun updateRawEdiText(text: String) {
        _rawEdiText.value = text
    }

    fun loadSample(sampleId: String) {
        val sample = EdiSamples.getSample(sampleId)
        if (sample != null) {
            _selectedSampleId.value = sampleId
            _rawEdiText.value = sample.rawContent
            validateAndParse()
        }
    }

    fun validateAndParse() {
        viewModelScope.launch {
            val result = parser.parseAndValidate(_rawEdiText.value)
            _parseResult.value = result
        }
    }

    fun formatEdiText() {
        val current = _rawEdiText.value.trim()
        if (current.isEmpty()) return

        // If delimited by ~, format with each segment on a newline
        val formatted = if (current.contains("~")) {
            current.split("~")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString("~\n") + "~"
        } else {
            current
        }
        _rawEdiText.value = formatted
        _snackbarMessage.value = "Formatted with clean segment line breaks"
    }

    fun clearEditor() {
        _rawEdiText.value = ""
        _selectedSampleId.value = null
        _parseResult.value = null
    }

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSnackbarMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}
