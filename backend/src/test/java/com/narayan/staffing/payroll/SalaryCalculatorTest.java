package com.narayan.staffing.payroll;

import com.narayan.staffing.domain.CalculationType;
import com.narayan.staffing.domain.ComponentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SalaryCalculatorTest {

    @Test
    void calculatesGrossDeductionsAndNetFromConfiguredComponents() {
        List<SalaryCalculator.ComponentInput> components = List.of(
                input("BASIC", "Basic", ComponentType.EARNING, CalculationType.FIXED, "20000", 10),
                input("HRA", "HRA", ComponentType.EARNING, CalculationType.PERCENT_OF_BASIC, "40", 20),
                input("PF", "PF", ComponentType.DEDUCTION, CalculationType.PERCENT_OF_BASIC, "12", 110),
                input("ESI", "ESI", ComponentType.DEDUCTION, CalculationType.PERCENT_OF_GROSS, "0.75", 120)
        );
        SalaryCalculator.Breakdown breakdown = SalaryCalculator.calculate(components, new BigDecimal("30"), new BigDecimal("30"));
        assertEquals(new BigDecimal("20000.00"), breakdown.basic());
        assertEquals(new BigDecimal("28000.00"), breakdown.gross());
        assertEquals(new BigDecimal("2610.00"), breakdown.deductions());
        assertEquals(new BigDecimal("25390.00"), breakdown.net());
    }

    @Test
    void proratesByPayableDays() {
        List<SalaryCalculator.ComponentInput> components = List.of(
                input("BASIC", "Basic", ComponentType.EARNING, CalculationType.FIXED, "30000", 10)
        );
        SalaryCalculator.Breakdown breakdown = SalaryCalculator.calculate(components, new BigDecimal("15"), new BigDecimal("30"));
        assertEquals(new BigDecimal("15000.00"), breakdown.basic());
        assertEquals(new BigDecimal("15000.00"), breakdown.net());
    }

    private SalaryCalculator.ComponentInput input(String code, String name, ComponentType type, CalculationType calc, String value, int order) {
        return new SalaryCalculator.ComponentInput(code, name, type, calc, new BigDecimal(value), order);
    }
}
