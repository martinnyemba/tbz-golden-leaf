package zm.co.tbz.goldenleaf

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import zm.co.tbz.goldenleaf.ui.scan.IdDocumentScanning

class IdDocumentScanningTest {

    @Test
    fun shouldScan_onlyAppliesToIdFrontAndBack() {
        assertTrue(IdDocumentScanning.shouldScan(IdDocumentScanning.TARGET_ID_FRONT))
        assertTrue(IdDocumentScanning.shouldScan(IdDocumentScanning.TARGET_ID_BACK))
        assertFalse(IdDocumentScanning.shouldScan(IdDocumentScanning.TARGET_PROFILE))
        assertFalse(IdDocumentScanning.shouldScan("farm"))
    }
}
