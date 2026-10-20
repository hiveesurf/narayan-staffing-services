package com.narayan.staffing.payroll;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttendanceWorkbookTest {

    @Test
    void readsPresentLeaveAndWeeklyOff() throws Exception {
        byte[] file;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Attendance");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("Employee code");
            header.createCell(1).setCellValue("Date");
            header.createCell(2).setCellValue("Status");
            row(sheet, 1, "NSS-EMP-00001", "2026-09-01", "Present");
            row(sheet, 2, "NSS-EMP-00001", "2026-09-02", "Weekly off");
            row(sheet, 3, "NSS-EMP-00001", "02/09/2026", "Leave");
            workbook.write(out);
            file = out.toByteArray();
        }
        List<AttendanceWorkbook.RowDay> days = AttendanceWorkbook.read(new ByteArrayInputStream(file), YearMonth.of(2026, 9));
        assertEquals(3, days.size());
        assertEquals(AttendanceStatus.PRESENT, days.get(0).status());
        assertEquals(AttendanceStatus.WEEKLY_OFF, days.get(1).status());
        assertEquals(LocalDate.of(2026, 9, 2), days.get(2).date());
        assertEquals(AttendanceStatus.LEAVE, days.get(2).status());
        assertTrue(AttendanceStatus.PRESENT.paid());
        assertTrue(AttendanceStatus.WEEKLY_OFF.paid());
        assertTrue(!AttendanceStatus.LEAVE.paid());
    }

    private static void row(org.apache.poi.ss.usermodel.Sheet sheet, int index, String code, String date, String status) {
        var row = sheet.createRow(index);
        row.createCell(0).setCellValue(code);
        row.createCell(1).setCellValue(date);
        row.createCell(2).setCellValue(status);
    }
}
