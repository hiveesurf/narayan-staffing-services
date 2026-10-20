package com.narayan.staffing.common;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
public class CodeGenerator {

    private static final Set<String> ALLOWED = Set.of(
            "job_code_seq", "company_code_seq", "application_no_seq", "employee_code_seq", "slip_no_seq"
    );

    private final EntityManager entityManager;

    public CodeGenerator(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public long nextValue(String sequence) {
        if (!ALLOWED.contains(sequence)) {
            throw new IllegalArgumentException("Unknown sequence");
        }
        Number value = (Number) entityManager.createNativeQuery("select nextval('" + sequence + "')").getSingleResult();
        return value.longValue();
    }

    public String next(String sequence, String prefix, int width) {
        return prefix + String.format("%0" + width + "d", nextValue(sequence));
    }
}
