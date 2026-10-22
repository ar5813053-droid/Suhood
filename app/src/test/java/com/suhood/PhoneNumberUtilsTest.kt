package com.suhood

import com.suhood.feature.phone.NumeralStyle
import com.suhood.feature.phone.PhoneNumberUtils
import org.junit.Assert.*
import org.junit.Test

class PhoneNumberUtilsTest {
    @Test
    fun normalizeWestern() {
        assertEquals("9876543210", PhoneNumberUtils.normalize("987 654 3210"))
        assertEquals("+919876543210", PhoneNumberUtils.normalize("+91 98765-43210"))
    }

    @Test
    fun normalizeArabicIndic() {
        assertEquals("0123456789", PhoneNumberUtils.normalize("٠١٢٣٤٥٦٧٨٩"))
    }

    @Test
    fun normalizePersian() {
        assertEquals("0123456789", PhoneNumberUtils.normalize("۰۱۲۳۴۵۶۷۸۹"))
    }

    @Test
    fun displayArabic() {
        assertEquals("٠١٢", PhoneNumberUtils.display("012", NumeralStyle.ARABIC_INDIC))
    }

    @Test
    fun displayPersian() {
        assertEquals("۰۱۲", PhoneNumberUtils.display("012", NumeralStyle.PERSIAN))
    }

    @Test
    fun validDial() {
        assertTrue(PhoneNumberUtils.isValidForDial("123"))
        assertFalse(PhoneNumberUtils.isValidForDial("+++"))
        assertFalse(PhoneNumberUtils.isValidForDial(""))
    }

    @Test
    fun matchesSuffix() {
        assertTrue(PhoneNumberUtils.matches("+919876543210", "9876543210"))
        assertFalse(PhoneNumberUtils.matches("111", "222"))
    }
}
