package com.narayan.staffing.payroll;

import com.narayan.staffing.common.BusinessException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AttendanceWorkbook {

    public record RowDay(int rowNumber, String employeeCode, LocalDate date, AttendanceStatus status) {
    }

    private static final List<DateTimeFormatter> DATES = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d/M/uuuu"),
            DateTimeFormatter.ofPattern("d-M-uuuu"),
            DateTimeFormatter.ofPattern("d/M/uu"),
            DateTimeFormatter.ofPattern("M/d/uuuu")
    );

    private AttendanceWorkbook() {
    }

    public static byte[] template() {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Attendance");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Employee code");
            header.createCell(1).setCellValue("Date");
            header.createCell(2).setCellValue("Status");
            sheet.setColumnWidth(0, 22 * 256);
            sheet.setColumnWidth(1, 16 * 256);
            sheet.setColumnWidth(2, 16 * 256);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException("The attendance template could not be created.");
        }
    }

    public static List<RowDay> read(InputStream input, YearMonth month) {
        try (Workbook workbook = new XSSFWorkbook(input)) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                throw new BusinessException("The workbook has no rows.");
            }
            DataFormatter formatter = new DataFormatter(Locale.ENGLISH);
            int headerRow = sheet.getFirstRowNum();
            Row header = sheet.getRow(headerRow);
            int codeCol = column(header, formatter, "employee code", "employee", "code");
            int dateCol = column(header, formatter, "date");
            int statusCol = column(header, formatter, "status");
            if (codeCol < 0 || dateCol < 0 || statusCol < 0) {
                throw new BusinessException("The first row must have Employee code, Date, and Status columns.");
            }
            List<RowDay> days = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            for (int i = headerRow + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || blank(row, formatter, codeCol, dateCol, statusCol)) {
                    continue;
                }
                int number = i + 1;
                String code = formatter.formatCellValue(row.getCell(codeCol)).trim();
                if (code.isBlank()) {
                    errors.add("Row " + number + " has no employee code.");
                    continue;
                }
                LocalDate date = date(row.getCell(dateCol), formatter);
                if (date == null) {
                    errors.add("Row " + number + " has a date that cannot be read.");
                    continue;
                }
                if (!YearMonth.from(date).equals(month)) {
                    errors.add("Row " + number + " is outside " + month.getMonth().name().toLowerCase() + " " + month.getYear() + ".");
                    continue;
                }
                AttendanceStatus status = AttendanceStatus.parse(formatter.formatCellValue(row.getCell(statusCol)));
                if (status == null) {
                    errors.add("Row " + number + " status must be Present, Absent, Leave, or Weekly off.");
                    continue;
                }
                days.add(new RowDay(number, code, date, status));
            }
            if (!errors.isEmpty()) {
                throw new BusinessException(String.join(" ", errors.stream().limit(8).toList()));
            }
            if (days.isEmpty()) {
                throw new BusinessException("The workbook has no attendance rows.");
            }
            return days;
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BusinessException("The file is not an Excel workbook.");
        }
    }

    private static int column(Row header, DataFormatter formatter, String... names) {
        if (header == null) {
            return -1;
        }
        for (Cell cell : header) {
            String value = formatter.formatCellValue(cell).trim().toLowerCase(Locale.ENGLISH);
            for (String name : names) {
                if (value.equals(name)) {
                    return cell.getColumnIndex();
                }
            }
        }
        return -1;
    }

    private static boolean blank(Row row, DataFormatter formatter, int... columns) {
        for (int column : columns) {
            if (!formatter.formatCellValue(row.getCell(column)).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static LocalDate date(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String text = formatter.formatCellValue(cell).trim();
        if (text.isBlank()) {
            return null;
        }
        for (DateTimeFormatter pattern : DATES) {
            try {
                return LocalDate.parse(text, pattern);
            } catch (DateTimeParseException ignored) {
                // try the next pattern
            }
        }
        return null;
    }
}
