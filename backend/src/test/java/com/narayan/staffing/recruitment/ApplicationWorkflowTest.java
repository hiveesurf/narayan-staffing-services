package com.narayan.staffing.recruitment;

import com.narayan.staffing.domain.ApplicationStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationWorkflowTest {

    @Test
    void allowsTheRecruitmentPath() {
        assertTrue(ApplicationWorkflow.canTransition(ApplicationStatus.APPLIED, ApplicationStatus.SCREENING));
        assertTrue(ApplicationWorkflow.canTransition(ApplicationStatus.APPLIED, ApplicationStatus.SHORTLISTED));
        assertTrue(ApplicationWorkflow.canTransition(ApplicationStatus.APPLIED, ApplicationStatus.REJECTED));
        assertTrue(ApplicationWorkflow.canTransition(ApplicationStatus.SCREENING, ApplicationStatus.SHORTLISTED));
        assertTrue(ApplicationWorkflow.canTransition(ApplicationStatus.SHORTLISTED, ApplicationStatus.INTERVIEW_SCHEDULED));
        assertTrue(ApplicationWorkflow.canTransition(ApplicationStatus.INTERVIEWED, ApplicationStatus.SELECTED));
        assertTrue(ApplicationWorkflow.canTransition(ApplicationStatus.SELECTED, ApplicationStatus.JOINED));
    }

    @Test
    void blocksSkippingToSelected() {
        assertFalse(ApplicationWorkflow.canTransition(ApplicationStatus.APPLIED, ApplicationStatus.SELECTED));
        assertFalse(ApplicationWorkflow.canTransition(ApplicationStatus.JOINED, ApplicationStatus.APPLIED));
    }
}
