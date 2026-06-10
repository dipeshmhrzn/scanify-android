package com.scanify.app.domain.model
sealed interface InAppUpdateState {
    data object Idle : InAppUpdateState
    data class Downloading(val progressPercent: Int) : InAppUpdateState
    data object Downloaded : InAppUpdateState
    data object Failed : InAppUpdateState
}