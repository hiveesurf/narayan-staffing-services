package com.narayan.staffing.payroll;

import com.narayan.staffing.domain.CalculationType;
import com.narayan.staffing.domain.ComponentType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SalaryCalculator {

    public static final String BASIC = "BASIC";

    private SalaryCalculator() {
    }

    public record ComponentInput(
            String code,
            String name,
            ComponentType type,
            CalculationType calculationType,
            BigDecimal value,
            int displayOrder
    ) {
    }

    public record Line(
            String code,
            String name,
            ComponentType type,
            BigDecimal amount,
            int displayOrder
    ) {
    }

    public record Breakdown(
            BigDecimal basic,
            BigDecimal gross,
            BigDecimal deductions,
            BigDecimal net,
            List<Line> lines
    ) {
    }

    public static Breakdown calculate(List<ComponentInput> components, BigDecimal payableDays, BigDecimal workingDays) {
        BigDecimal factor = factor(payableDays, workingDays);
        BigDecimal basic = proratedBasic(components, factor);

        List<ComponentInput> earnings = components.stream()
                .filter(c -> c.type() == ComponentType.EARNING)
                .sorted(Comparator.comparingInt(ComponentInput::displayOrder))
                .toList();
        List<ComponentInput> deductions = components.stream()
                .filter(c -> c.type() == ComponentType.DEDUCTION)
                .sorted(Comparator.comparingInt(ComponentInput::displayOrder))
                .toList();

        List<Line> lines = new ArrayList<>();
        BigDecimal grossBase = BigDecimal.ZERO;
        List<ComponentInput> percentOfGrossEarnings = new ArrayList<>();
        for (ComponentInput component : earnings) {
            if (component.calculationType() == CalculationType.PERCENT_OF_GROSS && !BASIC.equals(component.code())) {
                percentOfGrossEarnings.add(component);
                continue;
            }
            BigDecimal amount = amount(component, basic, factor, BigDecimal.ZERO);
            if (BASIC.equals(component.code())) {
                amount = basic;
            }
            lines.add(line(component, amount));
            grossBase = grossBase.add(amount);
        }
        BigDecimal gross = grossBase;
        for (ComponentInput component : percentOfGrossEarnings) {
            BigDecimal amount = money(grossBase.multiply(nz(component.value())).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP));
            lines.add(line(component, amount));
            gross = gross.add(amount);
        }

        BigDecimal deductionTotal = BigDecimal.ZERO;
        List<ComponentInput> percentOfGrossDeductions = new ArrayList<>();
        for (ComponentInput component : deductions) {
            if (component.calculationType() == CalculationType.PERCENT_OF_GROSS) {
                percentOfGrossDeductions.add(component);
                continue;
            }
            BigDecimal amount = amount(component, basic, factor, gross);
            lines.add(line(component, amount));
            deductionTotal = deductionTotal.add(amount);
        }
        for (ComponentInput component : percentOfGrossDeductions) {
            BigDecimal amount = money(gross.multiply(nz(component.value())).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP));
            lines.add(line(component, amount));
            deductionTotal = deductionTotal.add(amount);
        }

        lines.sort(Comparator.comparingInt(Line::displayOrder));
        BigDecimal net = money(gross.subtract(deductionTotal));
        return new Breakdown(money(basic), money(gross), money(deductionTotal), net, List.copyOf(lines));
    }

    private static BigDecimal proratedBasic(List<ComponentInput> components, BigDecimal factor) {
        return components.stream()
                .filter(c -> BASIC.equals(c.code()))
                .findFirst()
                .map(basic -> money(fixedLike(basic).multiply(factor)))
                .orElse(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    }

    private static BigDecimal amount(ComponentInput component, BigDecimal basic, BigDecimal factor, BigDecimal gross) {
        BigDecimal value = nz(component.value());
        return switch (component.calculationType()) {
            case PERCENT_OF_BASIC -> money(basic.multiply(value).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP));
            case PERCENT_OF_GROSS -> money(gross.multiply(value).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP));
            case FIXED, CUSTOM -> money(value.multiply(factor));
        };
    }

    private static BigDecimal fixedLike(ComponentInput component) {
        if (component.calculationType() == CalculationType.PERCENT_OF_BASIC
                || component.calculationType() == CalculationType.PERCENT_OF_GROSS) {
            return BigDecimal.ZERO;
        }
        return nz(component.value());
    }

    private static BigDecimal factor(BigDecimal payableDays, BigDecimal workingDays) {
        if (workingDays == null || payableDays == null || workingDays.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return payableDays.divide(workingDays, 8, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal money(BigDecimal value) {
        return nz(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static Line line(ComponentInput component, BigDecimal amount) {
        return new Line(component.code(), component.name(), component.type(), amount, component.displayOrder());
    }
}
