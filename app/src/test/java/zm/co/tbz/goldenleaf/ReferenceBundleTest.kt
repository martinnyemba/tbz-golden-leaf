package zm.co.tbz.goldenleaf

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import zm.co.tbz.goldenleaf.data.remote.dto.ReferenceBundleResponse

/**
 * Guards the contract with `GET /api/v1/mobile/reference/`
 * (apps/mobile_api/services/reference_service.py).
 *
 * The bundle previously failed to deserialize because the DTOs required
 * `id`/`name` on provinces, districts, tobacco_types and barn_types while the
 * portal sends `{code, label}` / `{code, name, province}`. kotlinx threw a
 * MissingFieldException and every server-driven dropdown stayed empty.
 */
class ReferenceBundleTest {

    // Same configuration as NetworkModule.provideJson().
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    /** Exactly the shape build_full_reference_payload() emits for a privileged user. */
    private val fullPayload = """
        {
          "version": "abc123def456",
          "generated_at": "2026-07-01T08:00:00+00:00",
          "provinces": [{"code": "EASTERN", "label": "Eastern"}],
          "districts": [{"code": "ZAMBIA-EASTERN-CHIPATA", "name": "CHIPATA", "province": "EASTERN"}],
          "barn_types": [{"code": "RIB", "label": "Rocket Barn"}],
          "tobacco_types": [{"code": "FLUE_CURED", "label": "Flue Cured Virginia"}],
          "permit_purposes": [{"code": "SALES", "label": "Sales"}],
          "sponsors": [{"id": "11111111-1111-1111-1111-111111111111", "name": "Acme", "code": "ACM"}],
          "salesfloors": [{"id": "22222222-2222-2222-2222-222222222222", "name": "Lusaka Floor", "code": "LSK", "province": "LUSAKA", "district": "Lusaka", "address": "Plot 1"}],
          "buyers": [{"id": "33333333-3333-3333-3333-333333333333", "name": "BuyCo", "company_name": "BuyCo Ltd", "code": "BC", "license_number": "", "contact_phone": "+260970000000"}],
          "stakeholders": [{"id": "44444444-4444-4444-4444-444444444444", "name": "Coop", "code": "CO"}]
        }
    """.trimIndent()

    @Test
    fun fullPayload_deserializesEverySection() {
        val bundle = json.decodeFromString(ReferenceBundleResponse.serializer(), fullPayload)

        assertEquals("abc123def456", bundle.version)
        assertEquals("EASTERN", bundle.provinces.single().code)
        assertEquals("Eastern", bundle.provinces.single().label)
        assertEquals("CHIPATA", bundle.districts.single().name)
        assertEquals("EASTERN", bundle.districts.single().province)
        assertEquals("FLUE_CURED", bundle.tobacco_types.single().code)
        assertEquals("RIB", bundle.barn_types.single().code)
        assertEquals("BuyCo Ltd", bundle.buyers.single().company_name)
        assertEquals("Coop", bundle.stakeholders.single().name)
        assertTrue(bundle.salesfloors.single().district == "Lusaka")
    }

    /** A field officer without marketing/validation perms gets sections popped server-side. */
    @Test
    fun scopedPayload_withSectionsOmitted_stillParses() {
        val scoped = """
            {
              "version": "v2",
              "provinces": [{"code": "LUSAKA", "label": "Lusaka"}],
              "districts": [{"code": "ZAMBIA-LUSAKA-LUSAKA", "name": "LUSAKA", "province": "LUSAKA"}],
              "tobacco_types": [{"code": "BURLEY", "label": "Burley"}],
              "barn_types": [{"code": "AIR", "label": "Air Cured"}]
            }
        """.trimIndent()

        val bundle = json.decodeFromString(ReferenceBundleResponse.serializer(), scoped)

        assertEquals(1, bundle.provinces.size)
        assertTrue(bundle.sponsors.isEmpty())
        assertTrue(bundle.salesfloors.isEmpty())
        assertTrue(bundle.buyers.isEmpty())
        assertTrue(bundle.stakeholders.isEmpty())
    }
}
