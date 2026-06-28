package zm.co.tbz.goldenleaf

import org.junit.Assert.assertEquals
import org.junit.Test
import zm.co.tbz.goldenleaf.ui.registration.calculateYieldPerHa
import zm.co.tbz.goldenleaf.ui.registration.formatPhoneNumber

class RegistrationModelsTest {

    @Test
    fun yieldPerHa_uses1500WhenHectarageAtMostNine() {
        assertEquals(1500, calculateYieldPerHa(9.0))
        assertEquals(1500, calculateYieldPerHa(0.5))
    }

    @Test
    fun yieldPerHa_uses3000WhenHectarageAboveNine() {
        assertEquals(3000, calculateYieldPerHa(9.1))
        assertEquals(3000, calculateYieldPerHa(50.0))
    }

    @Test
    fun formatPhoneNumber_stripsLeadingZeroForZambia() {
        assertEquals("+260971234567", formatPhoneNumber("Zambia", "0971234567"))
    }
}
