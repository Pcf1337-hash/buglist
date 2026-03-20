package com.buglist.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Single DataStore instance for all BugList app preferences.
 *
 * IMPORTANT: Per the Android DataStore docs, there must be exactly ONE DataStore
 * instance per file per process. This extension property is the canonical declaration.
 * All ViewModels that need preferences must reference this property — never declare
 * another `preferencesDataStore("buglist_settings")` elsewhere.
 */
internal val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "buglist_settings"
)
