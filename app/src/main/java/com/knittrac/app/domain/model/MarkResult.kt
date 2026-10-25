package com.knittrac.app.domain.model

sealed class MarkResult {
    data object Added : MarkResult()
    data object AlreadyExists : MarkResult()
}
