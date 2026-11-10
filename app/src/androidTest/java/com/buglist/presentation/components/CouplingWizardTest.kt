package com.buglist.presentation.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.buglist.data.sync.PairingCode
import com.buglist.domain.model.PairingLink
import com.buglist.domain.model.SyncRole
import com.buglist.presentation.person_detail.CoupleWizardContent
import com.buglist.presentation.person_detail.qrBitmap
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CouplingWizardTest {
    @get:Rule val compose = createComposeRule()
    private val pending = PairingLink(SyncRole.PENDING, null, null, true)

    @Test fun selectingARoleDoesNotPublishUntilExplicitConfirmation() {
        var claimed: Boolean? = null
        compose.setContent {
            CoupleWizardContent("QA", pending, null, null, false, false,
                {}, {}, { claimed = it }, {}, {}, {})
        }
        compose.onNodeWithText("Liste führen (Host)").performClick()
        compose.runOnIdle { assertNull(claimed) }
        compose.onNodeWithText("ROLLE BESTÄTIGEN").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(true, claimed) }
    }

    @Test fun viewerSelectionExplainsReplacementBeforeConfirmation() {
        compose.setContent {
            CoupleWizardContent("QA", pending, null, null, false, false,
                {}, {}, {}, {}, {}, {})
        }
        compose.onNodeWithText("Nur ansehen").performClick()
        compose.onNodeWithText("Mit deiner Bestätigung werden vorhandene Schulden und Zahlungen dieser Person durch den Host-Stand ersetzt. Andere Personen bleiben unverändert.")
            .performScrollTo().assertIsDisplayed()
    }

    @Test fun manualCodeEntryRemainsAvailableWithoutCamera() {
        var accepted: String? = null
        val code = PairingCode.encode(PairingCode.randomRoomId(), PairingCode.randomKey(), false)
        compose.setContent {
            CoupleWizardContent("QA", null, null, null, false, false,
                {}, { accepted = it }, {}, {}, {}, {})
        }
        compose.onNodeWithText("Code stattdessen eingeben").performScrollTo().performClick()
        compose.onNode(hasSetTextAction()).performScrollTo().performTextInput(code)
        compose.onNodeWithText("CODE ÜBERNEHMEN").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(code, accepted) }
    }

    @Test fun generatedQrCodeDecodesToTheExactPairingOffer() {
        val code = PairingCode.encode(PairingCode.randomRoomId(), PairingCode.randomKey(), false)
        val bitmap = qrBitmap(code)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        val result = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source)))
        assertEquals(code, result.text)
        assertNotNull(PairingCode.decode(result.text))
    }
}
