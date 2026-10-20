package com.narayan.staffing.recruitment;

import com.narayan.staffing.domain.ApplicationStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class ApplicationWorkflow {

    private static final Map<ApplicationStatus, Set<ApplicationStatus>> ALLOWED = new EnumMap<>(ApplicationStatus.class);

    static {
        ALLOWED.put(ApplicationStatus.APPLIED, EnumSet.of(ApplicationStatus.SCREENING, ApplicationStatus.SHORTLISTED, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN, ApplicationStatus.ON_HOLD));
        ALLOWED.put(ApplicationStatus.SCREENING, EnumSet.of(ApplicationStatus.SHORTLISTED, ApplicationStatus.REJECTED, ApplicationStatus.ON_HOLD, ApplicationStatus.WITHDRAWN));
        ALLOWED.put(ApplicationStatus.SHORTLISTED, EnumSet.of(ApplicationStatus.INTERVIEW_SCHEDULED, ApplicationStatus.REJECTED, ApplicationStatus.ON_HOLD, ApplicationStatus.WITHDRAWN));
        ALLOWED.put(ApplicationStatus.INTERVIEW_SCHEDULED, EnumSet.of(ApplicationStatus.INTERVIEWED, ApplicationStatus.REJECTED, ApplicationStatus.ON_HOLD, ApplicationStatus.WITHDRAWN, ApplicationStatus.SHORTLISTED));
        ALLOWED.put(ApplicationStatus.INTERVIEWED, EnumSet.of(ApplicationStatus.SELECTED, ApplicationStatus.REJECTED, ApplicationStatus.ON_HOLD, ApplicationStatus.SHORTLISTED));
        ALLOWED.put(ApplicationStatus.SELECTED, EnumSet.of(ApplicationStatus.JOINED, ApplicationStatus.REJECTED, ApplicationStatus.ON_HOLD));
        ALLOWED.put(ApplicationStatus.REJECTED, EnumSet.of(ApplicationStatus.SCREENING));
        ALLOWED.put(ApplicationStatus.ON_HOLD, EnumSet.of(ApplicationStatus.SCREENING, ApplicationStatus.SHORTLISTED, ApplicationStatus.INTERVIEW_SCHEDULED, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN));
        ALLOWED.put(ApplicationStatus.WITHDRAWN, EnumSet.of(ApplicationStatus.APPLIED));
        ALLOWED.put(ApplicationStatus.JOINED, EnumSet.noneOf(ApplicationStatus.class));
    }

    private ApplicationWorkflow() {
    }

    public static boolean canTransition(ApplicationStatus from, ApplicationStatus to) {
        if (from == null || to == null) {
            return false;
        }
        if (from == to) {
            return true;
        }
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }
}
