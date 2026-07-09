package zm.co.tbz.goldenleaf

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import zm.co.tbz.goldenleaf.data.remote.dto.GrowerDto
import zm.co.tbz.goldenleaf.data.repository.apiStatusForFilter
import zm.co.tbz.goldenleaf.data.repository.changedFieldLabels
import zm.co.tbz.goldenleaf.data.repository.growerTypeFromWizardKey
import zm.co.tbz.goldenleaf.data.repository.toEntity

class GrowerMappersTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun apiStatusForFilter_mapsPortalFilters() {
        assertNull(apiStatusForFilter("All"))
        assertEquals("PENDING", apiStatusForFilter("Pending"))
        assertEquals("RETURNED_FOR_CORRECTION", apiStatusForFilter("Returned"))
    }

    @Test
    fun growerTypeFromWizardKey_mapsCategoryAndType() {
        assertEquals("SMALL_SCALE" to "INDIVIDUAL", growerTypeFromWizardKey("small"))
        assertEquals("COMPANY" to "COMPANY", growerTypeFromWizardKey("company"))
    }

    @Test
    fun changedFieldLabels_formatsPatchKeys() {
        val patch = json.encodeToString(
            kotlinx.serialization.json.JsonObject.serializer(),
            buildJsonObject {
                put("phone_number", "+260971234567")
                put("province", "prov-1")
            },
        )
        val labels = changedFieldLabels(patch, json)
        assertTrue(labels.contains("Phone"))
        assertTrue(labels.contains("Location"))
    }

    @Test
    fun growerDto_toEntity_mapsCorrectionFields() {
        val entity = GrowerDto(
            id = "grower-1",
            first_name = "Mary",
            last_name = "Phiri",
            status = "RETURNED_FOR_CORRECTION",
            correction_reason = "Fix NRC",
            correction_requested_at = "2026-06-01",
            correction_reviewer_name = "Reviewer",
        ).toEntity()
        assertEquals("Fix NRC", entity.correction_reason)
        assertEquals("Reviewer", entity.correction_reviewer_name)
    }
}
