package com.narayan.staffing.payroll;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.narayan.staffing.common.SensitiveDataMasker;
import com.narayan.staffing.domain.ComponentType;
import com.narayan.staffing.security.FieldCrypto;
import com.narayan.staffing.workforce.Employee;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Month;
import java.util.List;

@Component
public class SalarySlipPdfService {

    private static final Color INK = new Color(20, 48, 40);
    private static final Color PINE = new Color(29, 107, 69);
    private static final Color LINE = new Color(221, 214, 200);
    private static final Color PAPER = new Color(244, 241, 235);

    private final FieldCrypto crypto;

    public SalarySlipPdfService(FieldCrypto crypto) {
        this.crypto = crypto;
    }

    public byte[] render(PayrollRecord record, List<PayrollLine> lines, String slipNumber) {
        Employee employee = record.getEmployee();
        var run = record.getPayrollRun();
        var company = run.getCompany();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, out);
        document.open();
        Font brand = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, PINE);
        Font small = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(61, 86, 72));
        Font label = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, PINE);
        Font body = FontFactory.getFont(FontFactory.HELVETICA, 10, INK);
        Font money = FontFactory.getFont(FontFactory.HELVETICA, 10, INK);

        document.add(new Paragraph("NARAYAN STAFFING SERVICES", brand));
        document.add(new Paragraph("24, 3rd floor, Shriram Sankul, Chakan, Talegaon Road, Pune 410501", small));
        document.add(new Paragraph("Salary Slip — " + Month.of(run.getPeriodMonth()) + " " + run.getPeriodYear(), label));
        document.add(new Paragraph(company.getCompanyName() + (company.getAddress() == null ? "" : " · " + company.getCity()), small));
        document.add(spacer());

        PdfPTable info = new PdfPTable(new float[]{1.2f, 2f, 1.2f, 2f});
        info.setWidthPercentage(100);
        addInfo(info, "Employee", employee.getFullName(), body, small);
        addInfo(info, "Employee ID", employee.getEmployeeCode(), body, small);
        addInfo(info, "Designation", nullToDash(record.getDesignation()), body, small);
        addInfo(info, "Department", nullToDash(record.getDepartment()), body, small);
        addInfo(info, "Company", company.getCompanyName(), body, small);
        addInfo(info, "Date of joining", employee.getJoiningDate() == null ? "—" : employee.getJoiningDate().toString(), body, small);
        addInfo(info, "Bank account", maskAccount(employee), body, small);
        addInfo(info, "PAN", maskPan(employee), body, small);
        addInfo(info, "Slip number", slipNumber, body, small);
        addInfo(info, "Paid days", record.getPayableDays().stripTrailingZeros().toPlainString() + " / " + record.getWorkingDays(), body, small);
        document.add(info);
        document.add(spacer());

        PdfPTable days = new PdfPTable(4);
        days.setWidthPercentage(100);
        headerCell(days, "Total days");
        headerCell(days, "Working days");
        headerCell(days, "Paid days");
        headerCell(days, "Leave days");
        bodyCell(days, String.valueOf(record.getTotalDays()), money);
        bodyCell(days, String.valueOf(record.getWorkingDays()), money);
        bodyCell(days, record.getPayableDays().stripTrailingZeros().toPlainString(), money);
        bodyCell(days, record.getLeaveDays().stripTrailingZeros().toPlainString(), money);
        document.add(days);
        document.add(spacer());

        PdfPTable pay = new PdfPTable(new float[]{3f, 1.4f, 3f, 1.4f});
        pay.setWidthPercentage(100);
        headerCell(pay, "Earnings");
        headerCell(pay, "Amount (₹)");
        headerCell(pay, "Deductions");
        headerCell(pay, "Amount (₹)");
        List<PayrollLine> earnings = lines.stream().filter(line -> line.getLineType() == ComponentType.EARNING).toList();
        List<PayrollLine> deductions = lines.stream().filter(line -> line.getLineType() == ComponentType.DEDUCTION).toList();
        int rows = Math.max(earnings.size(), deductions.size());
        for (int i = 0; i < rows; i++) {
            if (i < earnings.size()) {
                bodyCell(pay, earnings.get(i).getComponentName(), body);
                bodyCell(pay, money(earnings.get(i).getAmount()), money);
            } else {
                bodyCell(pay, "", body);
                bodyCell(pay, "", money);
            }
            if (i < deductions.size()) {
                bodyCell(pay, deductions.get(i).getComponentName(), body);
                bodyCell(pay, money(deductions.get(i).getAmount()), money);
            } else {
                bodyCell(pay, "", body);
                bodyCell(pay, "", money);
            }
        }
        document.add(pay);
        document.add(spacer());

        PdfPTable totals = new PdfPTable(new float[]{2f, 1f});
        totals.setWidthPercentage(50);
        totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalRow(totals, "Gross earnings", record.getGrossAmount(), body);
        totalRow(totals, "Total deductions", record.getDeductionAmount(), body);
        totalRow(totals, "Net pay", record.getNetAmount(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, INK));
        document.add(totals);
        document.add(new Paragraph("Amount in words: " + AmountInWords.rupees(record.getNetAmount()), small));
        document.add(spacer());
        document.add(new Paragraph("This is a computer-generated salary slip and does not require a signature.", small));
        document.close();
        return out.toByteArray();
    }

    private void addInfo(PdfPTable table, String label, String value, Font valueFont, Font labelFont) {
        PdfPCell l = new PdfPCell(new Phrase(label, labelFont));
        l.setBorder(0);
        l.setPadding(4);
        PdfPCell v = new PdfPCell(new Phrase(value == null ? "—" : value, valueFont));
        v.setBorder(0);
        v.setPadding(4);
        table.addCell(l);
        table.addCell(v);
    }

    private void headerCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
        cell.setBackgroundColor(PINE);
        cell.setPadding(6);
        cell.setBorderColor(PINE);
        table.addCell(cell);
    }

    private void bodyCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        cell.setBorderColor(LINE);
        cell.setBackgroundColor(PAPER);
        table.addCell(cell);
    }

    private void totalRow(PdfPTable table, String label, BigDecimal amount, Font font) {
        PdfPCell l = new PdfPCell(new Phrase(label, font));
        l.setBorder(0);
        l.setPadding(3);
        PdfPCell v = new PdfPCell(new Phrase(money(amount), font));
        v.setBorder(0);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        v.setPadding(3);
        table.addCell(l);
        table.addCell(v);
    }

    private Paragraph spacer() {
        return new Paragraph(" ");
    }

    private String money(BigDecimal amount) {
        return amount == null ? "0.00" : String.format("%,.2f", amount);
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private String maskAccount(Employee employee) {
        if (employee.getBankAccountEncrypted() == null) return "—";
        try {
            return SensitiveDataMasker.maskAccount(crypto.decrypt(employee.getBankAccountEncrypted()));
        } catch (Exception ex) {
            return "—";
        }
    }

    private String maskPan(Employee employee) {
        if (employee.getCandidate() == null || employee.getCandidate().getPanEncrypted() == null) return "—";
        try {
            return SensitiveDataMasker.maskPan(crypto.decrypt(employee.getCandidate().getPanEncrypted()));
        } catch (Exception ex) {
            return "—";
        }
    }
}
