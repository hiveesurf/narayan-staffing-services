package com.narayan.staffing.payroll;

public enum AttendanceStatus {
    PRESENT,
    ABSENT,
    LEAVE,
    WEEKLY_OFF;

    public boolean paid() {
        return this == PRESENT || this == WEEKLY_OFF;
    }

    public static AttendanceStatus parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String key = raw.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        return switch (key) {
            case "PRESENT", "P" -> PRESENT;
            case "ABSENT", "A" -> ABSENT;
            case "LEAVE", "L" -> LEAVE;
            case "WEEKLY_OFF", "WEEKLYOFF", "WO", "OFF" -> WEEKLY_OFF;
            default -> null;
        };
    }
}
