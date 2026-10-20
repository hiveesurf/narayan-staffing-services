package com.narayan.staffing.payroll;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AmountInWordsTest {

    @Test
    void writesIndianGrouping() {
        assertEquals("One Lakh Twenty Three Thousand Four Hundred Fifty Six Rupees and Seventy Eight Paise Only",
                AmountInWords.rupees(new BigDecimal("123456.78")));
    }

    @Test
    void writesZero() {
        assertEquals("Zero Rupees Only", AmountInWords.rupees(BigDecimal.ZERO));
    }
}
