package com.buglist.data.backup

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class BackupCompatibilityTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun pre26BackupKeepsInactiveGroupingAndMotionDefaults() {
        val oldBackup = """{
            "schemaVersion":6,"exportedAt":1,
            "settings":{"currency":"EUR","autoLockTimeoutSeconds":60,"showDescription":false},
            "debtEntries":[{"id":1,"personId":1,"amount":125.50,"isOwedToMe":true,"date":1,"createdAt":1}]
        }"""

        val restored = json.decodeFromString<BackupPayload>(oldBackup)

        assertTrue(restored.settings.groupInactive)
        assertTrue(restored.settings.ambientMotion)
        assertNull(restored.debtEntries.single().syncSerial)
        assertEquals(125.50, restored.debtEntries.single().amount, 0.001)
    }

    @Test
    fun newPreferencesAndHostReceiptSurviveBackupSerialization() {
        val payload = BackupPayload(
            settings = SettingsBackup(groupInactive = false, ambientMotion = false),
            debtEntries = listOf(DebtEntryBackup(
                id = 9, personId = 1, amount = 125.50, isOwedToMe = false, date = 1,
                createdAt = 1, syncSerial = "BL-20261110-0001"
            ))
        )

        val restored = json.decodeFromString<BackupPayload>(json.encodeToString(payload))

        assertFalse(restored.settings.groupInactive)
        assertFalse(restored.settings.ambientMotion)
        assertEquals("BL-20261110-0001", restored.debtEntries.single().syncSerial)
        assertFalse(restored.debtEntries.single().isOwedToMe)
    }
}
