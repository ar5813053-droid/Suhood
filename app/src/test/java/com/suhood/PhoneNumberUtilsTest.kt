package com.suhood

import com.suhood.feature.phone.NumeralStyle
import com.suhood.feature.phone.PhoneNumberUtils
import org.junit.Assert.*
import org.junit.Test

class PhoneNumberUtilsTest {
    @Test
    fun normalizeWesternAndArabicIndic() {
        assertEquals("0123456789", PhoneNumberUtils.normalize("0123456789"))
        assertEquals("0123456789", PhoneNumberUtils.normalize("٠١٢٣٤٥٦٧٨٩"))
        assertEquals("0123456789", PhoneNumberUtils.normalize("۰۱۲۳۴۵۶۷۸۹"))
        assertEquals("+919876543210", PhoneNumberUtils.normalize("+91 98765-43210"))
    }

    @Test
    fun displayStyles() {
        assertEquals("123", PhoneNumberUtils.display("123", NumeralStyle.WESTERN))
        assertEquals("١٢٣", PhoneNumberUtils.display("123", NumeralStyle.ARABIC_INDIC))
        assertEquals("۱۲۳", PhoneNumberUtils.display("123", NumeralStyle.PERSIAN))
    }

    @Test
    fun validForDial() {
        assertTrue(PhoneNumberUtils.isValidForDial("9876543210"))
        assertFalse(PhoneNumberUtils.isValidForDial(""))
        assertFalse(PhoneNumberUtils.isValidForDial("+++"))
    }

    @Test
    fun matchesSuffix() {
        assertTrue(PhoneNumberUtils.matches("+919876543210", "9876543210"))
        assertFalse(PhoneNumberUtils.matches("111", "222"))
    }
}
