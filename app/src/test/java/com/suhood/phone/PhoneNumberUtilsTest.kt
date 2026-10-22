package com.suhood.phone

import com.suhood.feature.phone.NumeralStyle
import com.suhood.feature.phone.PhoneNumberUtils
import org.junit.Assert.*
import org.junit.Test

class PhoneNumberUtilsTest {
    @Test fun normalizeStripsFormatting() {
        assertEquals("+966501234567", PhoneNumberUtils.normalize("+966 50-123-4567"))
    }
    @Test fun normalizeArabicIndicDigits() {
        assertEquals("0123456789", PhoneNumberUtils.normalize("٠١٢٣٤٥٦٧٨٩"))
    }
    @Test fun normalizePersianDigits() {
        assertEquals("0123456789", PhoneNumberUtils.normalize("۰۱۲۳۴۵۶۷۸۹"))
    }
    @Test fun displayArabicIndic() {
        assertEquals("٠١٢", PhoneNumberUtils.display("012", NumeralStyle.ARABIC_INDIC))
    }
    @Test fun validDialRequiresDigit() {
        assertFalse(PhoneNumberUtils.isValidForDial(""))
        assertTrue(PhoneNumberUtils.isValidForDial("*123#"))
    }
}
