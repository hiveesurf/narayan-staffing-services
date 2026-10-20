package com.narayan.staffing.common;

public final class SensitiveDataMasker {

    private SensitiveDataMasker() {
    }

    public static String digits(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("\\s+", "").toUpperCase();
    }

    public static String maskAadhaar(String value) {
        String digits = value == null ? null : value.replaceAll("\\D", "");
        if (digits == null || digits.length() < 4) {
            return null;
        }
        return "XXXX XXXX " + digits.substring(digits.length() - 4);
    }

    public static String maskPan(String value) {
        String pan = digits(value);
        if (pan == null || pan.length() < 4) {
            return null;
        }
        return "XXXXX" + pan.substring(pan.length() - 4);
    }

    public static String maskAccount(String value) {
        String account = value == null ? null : value.replaceAll("\\s+", "");
        if (account == null || account.length() < 4) {
            return null;
        }
        return "XXXXXX" + account.substring(account.length() - 4);
    }
}
