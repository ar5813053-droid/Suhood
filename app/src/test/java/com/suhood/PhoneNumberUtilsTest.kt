package com.suhood

import com.suhood.feature.phone.NumeralStyle
import com.suhood.feature.phone.PhoneNumberUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumberUtilsTest {
    @Test
    fun normalizeStripsNonDigitsExceptPlus() {
        assertEquals("+911234567890", PhoneNumberUtils.normalize("+91 123-456-7890"))
        assertEquals("0123456789", PhoneNumberUtils.normalize("012-345-6789"))
    }

    @Test
    fun arabicIndicDisplay() {
        assertEquals("٠١٢٣", PhoneNumberUtils.display("0123", NumeralStyle.ARABIC_INDIC))
    }

    @Test
    fun persianDisplay() {
        assertEquals("۰۱۲۳", PhoneNumberUtils.display("0123", NumeralStyle.PERSIAN))
    }

    @Test
    fun westernRoundTrip() {
        assertEquals("5551234", PhoneNumberUtils.normalize(PhoneNumberUtils.display("5551234", NumeralStyle.WESTERN)))
    }

    @Test
    fun emptyStaysEmpty() {
        assertEquals("", PhoneNumberUtils.normalize(""))
        assertEquals("", PhoneNumberUtils.display("", NumeralStyle.WESTERN))
    }
}
