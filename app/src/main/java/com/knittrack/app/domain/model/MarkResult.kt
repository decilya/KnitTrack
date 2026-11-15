package com.knittrack.app.domain.model

sealed class MarkResult {
    data object Added : MarkResult()
    data object AlreadyExists : MarkResult()
}
