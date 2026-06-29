package zm.co.tbz.goldenleaf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import zm.co.tbz.goldenleaf.ui.components.extractNrcOrPassport

class NrcExtractTest {

    @Test
    fun extractNrcOrPassport_normalizesZambianNrc() {
        assertEquals("224018/61/1", extractNrcOrPassport("ID 224018 / 61 / 1 issued"))
        assertEquals("000000/00/0", extractNrcOrPassport("000000/00/0"))
    }

    @Test
    fun extractNrcOrPassport_findsPassportNumber() {
        assertEquals("AB1234567", extractNrcOrPassport("Passport no AB1234567"))
    }

    @Test
    fun extractNrcOrPassport_returnsNullWhenNoMatch() {
        assertNull(extractNrcOrPassport("no id here"))
        assertNull(extractNrcOrPassport(""))
    }
}
