package com.buglist.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** Bumps list observers after a snapshot is applied. SQLCipher does not always do this itself. */
@Singleton
class ChangeSignal @Inject constructor() {
    private val _version = MutableStateFlow(0L)
    val version = _version.asStateFlow()

    fun bump() {
        _version.update { it + 1 }
    }
}
