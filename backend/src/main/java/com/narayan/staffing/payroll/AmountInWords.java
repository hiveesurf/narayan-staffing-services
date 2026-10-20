package com.narayan.staffing.payroll;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class AmountInWords {

    private static final String[] ONES = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
            "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };
    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    private AmountInWords() {
    }

    public static String rupees(BigDecimal amount) {
        if (amount == null) {
            return "Zero Rupees Only";
        }
        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        if (scaled.signum() < 0) {
            return "Minus " + rupees(scaled.abs());
        }
        long rupees = scaled.longValue();
        int paise = scaled.remainder(BigDecimal.ONE).movePointRight(2).intValue();
        String words = rupees == 0 ? "Zero" : indian(rupees).trim();
        String result = words + " Rupees";
        if (paise > 0) {
            result += " and " + indian(paise).trim() + " Paise";
        }
        return result + " Only";
    }

    private static String indian(long number) {
        if (number < 20) {
            return ONES[(int) number];
        }
        if (number < 100) {
            return TENS[(int) (number / 10)] + (number % 10 == 0 ? "" : " " + ONES[(int) (number % 10)]);
        }
        if (number < 1000) {
            return ONES[(int) (number / 100)] + " Hundred" + rest(number % 100);
        }
        if (number < 100_000) {
            return indian(number / 1000) + " Thousand" + rest(number % 1000);
        }
        if (number < 10_000_000) {
            return indian(number / 100_000) + " Lakh" + rest(number % 100_000);
        }
        return indian(number / 10_000_000) + " Crore" + rest(number % 10_000_000);
    }

    private static String rest(long remainder) {
        if (remainder == 0) {
            return "";
        }
        return " " + indian(remainder).trim();
    }
}
