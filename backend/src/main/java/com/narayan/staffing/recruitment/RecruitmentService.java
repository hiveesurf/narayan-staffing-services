package com.narayan.staffing.recruitment;

import com.narayan.staffing.audit.AuditService;
import com.narayan.staffing.candidate.Candidate;
import com.narayan.staffing.candidate.CandidateEducation;
import com.narayan.staffing.candidate.CandidateEducationRepository;
import com.narayan.staffing.candidate.CandidateExperience;
import com.narayan.staffing.candidate.CandidateExperienceRepository;
import com.narayan.staffing.candidate.CandidateRepository;
import com.narayan.staffing.candidate.CandidateSkill;
import com.narayan.staffing.candidate.CandidateSkillRepository;
import com.narayan.staffing.common.BusinessException;
import com.narayan.staffing.common.CodeGenerator;
import com.narayan.staffing.common.ConflictException;
import com.narayan.staffing.common.NotFoundException;
import com.narayan.staffing.common.PageRequests;
import com.narayan.staffing.common.PageResponse;
import com.narayan.staffing.common.SecurityUtils;
import com.narayan.staffing.common.SensitiveDataMasker;
import com.narayan.staffing.document.DocumentRequirementRepository;
import com.narayan.staffing.document.DocumentService;
import com.narayan.staffing.domain.ApplicationStatus;
import com.narayan.staffing.domain.InterviewStatus;
import com.narayan.staffing.identity.User;
import com.narayan.staffing.identity.UserRepository;
import com.narayan.staffing.job.Job;
import com.narayan.staffing.job.JobService;
import com.narayan.staffing.notification.NotificationService;
import com.narayan.staffing.security.FieldCrypto;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecruitmentService {

    private final JobApplicationRepository applications;
    private final ApplicationStatusEventRepository history;
    private final ApplicationNoteRepository notes;
    private final InterviewRepository interviews;
    private final InterviewFeedbackRepository feedbackRepository;
    private final CandidateRepository candidates;
    private final CandidateEducationRepository education;
    private final CandidateExperienceRepository experience;
    private final CandidateSkillRepository skills;
    private final DocumentRequirementRepository requirements;
    private final DocumentService documents;
    private final JobService jobs;
    private final UserRepository users;
    private final CodeGenerator codes;
    private final AuditService auditService;
    private final NotificationService notifications;
    private final FieldCrypto crypto;

    @Transactional
    public RecruitmentDtos.ApplyResponse apply(String slug, RecruitmentDtos.ApplyRequest request, MultipartFile resume, MultipartFile photograph, MultipartFile idProof) {
        if (!request.consent()) {
            throw new BusinessException("Please confirm the information is accurate.");
        }
        Job job = jobs.requirePublishedSlug(slug);
        if (job.getDeadline() != null && job.getDeadline().isBefore(LocalDate.now())) {
            throw new BusinessException("This job is no longer accepting applications.");
        }
        requirements.findByActiveTrueOrderByDisplayOrderAsc().forEach(requirement -> {
            if (!requirement.isRequired()) {
                return;
            }
            MultipartFile file = switch (requirement.getCode()) {
                case "RESUME" -> resume;
                case "PHOTOGRAPH" -> photograph;
                case "ID_PROOF" -> idProof;
                default -> null;
            };
            if (file == null || file.isEmpty()) {
                throw new BusinessException(requirement.getLabel() + " is required.");
            }
        });
        Candidate candidate = candidates.findByEmailIgnoreCase(request.email().trim()).orElseGet(Candidate::new);
        boolean fresh = candidate.getId() == null;
        candidate.setFullName(request.fullName().trim());
        candidate.setDateOfBirth(request.dateOfBirth());
        candidate.setGender(request.gender());
        candidate.setMobile(request.mobile().trim());
        candidate.setEmail(request.email().trim().toLowerCase());
        candidate.setAlternatePhone(request.alternatePhone());
        candidate.setAddress(request.address());
        candidate.setCity(request.city());
        candidate.setState(request.state());
        candidate.setPincode(request.pincode());
        candidate.setHighestQualification(request.highestQualification());
        candidate.setSpecialization(request.specialization());
        candidate.setTotalExperience(request.totalExperience());
        candidate.setRelevantExperience(request.relevantExperience());
        candidate.setCurrentCompany(request.currentCompany());
        candidate.setCurrentJobTitle(request.currentJobTitle());
        candidate.setCurrentSalary(request.currentSalary());
        candidate.setExpectedSalary(request.expectedSalary());
        candidate.setNoticePeriod(request.noticePeriod());
        candidates.save(candidate);
        replaceSkills(candidate, request.skills());
        if (!fresh && applications.existsByCandidateIdAndJobId(candidate.getId(), job.getId())) {
            throw new ConflictException("An application for this email already exists for this job.");
        }
        if (fresh && applications.existsByCandidateIdAndJobId(candidate.getId(), job.getId())) {
            throw new ConflictException("An application for this email already exists for this job.");
        }
        JobApplication application = new JobApplication();
        application.setApplicationNumber(codes.next("application_no_seq", "NSS-APP-" + Year.now().getValue() + "-", 5));
        application.setCandidate(candidate);
        application.setJob(job);
        application.setStatus(ApplicationStatus.APPLIED);
        application.setConsent(true);
        applications.save(application);
        recordStatus(application, null, ApplicationStatus.APPLIED, null, "Application submitted");
        storeOptional(application.getId(), "RESUME", resume, false);
        storeOptional(application.getId(), "PHOTOGRAPH", photograph, false);
        storeOptional(application.getId(), "ID_PROOF", idProof, false);
        notifications.email(candidate.getEmail(), "APPLICATION_RECEIVED", "Application received — " + job.getTitle(),
                "Thank you, " + candidate.getFullName() + ".\nYour application number is " + application.getApplicationNumber() + " for " + job.getTitle() + " at " + job.getLocation() + ".",
                "APPLICATION", application.getId());
        return new RecruitmentDtos.ApplyResponse(application.getApplicationNumber(), "Application submitted.");
    }

    @Transactional(readOnly = true)
    public PageResponse<RecruitmentDtos.ApplicationCard> search(String q, UUID jobId, UUID categoryId, String location, String status,
                                                                 BigDecimal minExperience, BigDecimal maxSalary, Instant from, Instant to,
                                                                 int page, int size) {
        Pageable pageable = PageRequests.of(page, size, "createdAt", "desc", Set.of("createdAt", "status"), "createdAt");
        Specification<JobApplication> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<JobApplication, Candidate> candidate = root.join("candidate");
            Join<JobApplication, Job> job = root.join("job");
            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(candidate.get("fullName")), like),
                        cb.like(cb.lower(candidate.get("email")), like),
                        cb.like(cb.lower(root.get("applicationNumber")), like),
                        cb.like(cb.lower(job.get("title")), like)
                ));
            }
            if (jobId != null) predicates.add(cb.equal(job.get("id"), jobId));
            if (categoryId != null) predicates.add(cb.equal(job.get("category").get("id"), categoryId));
            if (location != null && !location.isBlank()) predicates.add(cb.like(cb.lower(candidate.get("city")), "%" + location.toLowerCase() + "%"));
            if (status != null && !status.isBlank()) predicates.add(cb.equal(root.get("status"), ApplicationStatus.valueOf(status)));
            if (minExperience != null) predicates.add(cb.greaterThanOrEqualTo(candidate.get("totalExperience"), minExperience));
            if (maxSalary != null) predicates.add(cb.lessThanOrEqualTo(candidate.get("expectedSalary"), maxSalary));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            query.distinct(true);
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return PageResponse.from(applications.findAll(spec, pageable).map(this::card));
    }

    @Transactional(readOnly = true)
    public Map<String, List<RecruitmentDtos.ApplicationCard>> board(UUID jobId) {
        List<JobApplication> rows = applications.findAll((root, query, cb) -> jobId == null ? cb.conjunction() : cb.equal(root.get("job").get("id"), jobId));
        Map<String, List<RecruitmentDtos.ApplicationCard>> columns = new LinkedHashMap<>();
        for (String key : List.of("APPLIED", "SCREENING", "SHORTLISTED", "INTERVIEW", "SELECTED", "JOINED", "REJECTED", "ON_HOLD", "WITHDRAWN")) {
            columns.put(key, new ArrayList<>());
        }
        for (JobApplication application : rows) {
            String key = switch (application.getStatus()) {
                case INTERVIEW_SCHEDULED, INTERVIEWED -> "INTERVIEW";
                default -> application.getStatus().name();
            };
            columns.computeIfAbsent(key, k -> new ArrayList<>()).add(card(application));
        }
        return columns;
    }

    @Transactional(readOnly = true)
    public RecruitmentDtos.ApplicationDetail detail(UUID id) {
        return toDetail(requireApplication(id));
    }

    @Transactional
    public RecruitmentDtos.ApplicationDetail changeStatus(UUID id, ApplicationStatus status, String reason, String note) {
        JobApplication application = requireApplication(id);
        move(application, status, reason, note);
        return toDetail(application);
    }

    @Transactional
    public int bulkStatus(List<UUID> ids, ApplicationStatus status, String note) {
        int count = 0;
        for (UUID id : ids) {
            JobApplication application = requireApplication(id);
            if (ApplicationWorkflow.canTransition(application.getStatus(), status)) {
                move(application, status, null, note);
                count++;
            }
        }
        return count;
    }

    @Transactional
    public RecruitmentDtos.ApplicationDetail assign(UUID id, UUID userId) {
        JobApplication application = requireApplication(id);
        User hr = users.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        application.setAssignedHr(hr);
        return toDetail(application);
    }

    @Transactional
    public RecruitmentDtos.NoteItem addNote(UUID id, String note) {
        JobApplication application = requireApplication(id);
        ApplicationNote entity = new ApplicationNote();
        entity.setApplication(application);
        entity.setNote(note.trim());
        if (SecurityUtils.currentUserId() != null) {
            entity.setAuthor(users.findById(SecurityUtils.currentUserId()).orElse(null));
        }
        notes.save(entity);
        return new RecruitmentDtos.NoteItem(entity.getId(), entity.getAuthor() == null ? "Staff" : entity.getAuthor().getFullName(), entity.getNote(), entity.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public PageResponse<RecruitmentDtos.CandidateView> candidates(String q, String city, int page, int size) {
        Pageable pageable = PageRequests.of(page, size, "fullName", "asc", Set.of("fullName", "createdAt", "city"), "fullName");
        Specification<Candidate> spec = (root, query, cb) -> {
            if (q == null || q.isBlank()) {
                if (city == null || city.isBlank()) return cb.conjunction();
                return cb.like(cb.lower(root.get("city")), "%" + city.toLowerCase() + "%");
            }
            String like = "%" + q.toLowerCase() + "%";
            Predicate text = cb.or(cb.like(cb.lower(root.get("fullName")), like), cb.like(cb.lower(root.get("email")), like), cb.like(cb.lower(root.get("mobile")), like));
            if (city != null && !city.isBlank()) {
                return cb.and(text, cb.like(cb.lower(root.get("city")), "%" + city.toLowerCase() + "%"));
            }
            return text;
        };
        return PageResponse.from(candidates.findAll(spec, pageable).map(this::candidateView));
    }

    @Transactional(readOnly = true)
    public RecruitmentDtos.CandidateView candidate(UUID id) {
        return candidateView(requireCandidate(id));
    }

    @Transactional
    public RecruitmentDtos.CandidateView updateCandidate(UUID id, RecruitmentDtos.CandidateUpdate request) {
        Candidate candidate = requireCandidate(id);
        candidate.setFullName(request.fullName().trim());
        candidate.setDateOfBirth(request.dateOfBirth());
        candidate.setGender(request.gender());
        candidate.setMobile(request.mobile().trim());
        if (!candidate.getEmail().equalsIgnoreCase(request.email()) && candidates.findByEmailIgnoreCase(request.email()).isPresent()) {
            throw new ConflictException("Another candidate already uses that email.");
        }
        candidate.setEmail(request.email().trim().toLowerCase());
        candidate.setAlternatePhone(request.alternatePhone());
        candidate.setAddress(request.address());
        candidate.setCity(request.city());
        candidate.setState(request.state());
        candidate.setPincode(request.pincode());
        candidate.setCountry(blank(request.country()) ? "India" : request.country());
        candidate.setPermanentAddress(request.permanentAddress());
        candidate.setFatherName(request.fatherName());
        candidate.setMotherName(request.motherName());
        candidate.setMaritalStatus(request.maritalStatus());
        candidate.setEmergencyContactName(request.emergencyContactName());
        candidate.setEmergencyContactPhone(request.emergencyContactPhone());
        candidate.setHighestQualification(request.highestQualification());
        candidate.setSpecialization(request.specialization());
        candidate.setTotalExperience(request.totalExperience());
        candidate.setRelevantExperience(request.relevantExperience());
        candidate.setCurrentCompany(request.currentCompany());
        candidate.setCurrentJobTitle(request.currentJobTitle());
        candidate.setCurrentSalary(request.currentSalary());
        candidate.setExpectedSalary(request.expectedSalary());
        candidate.setNoticePeriod(request.noticePeriod());
        candidate.setOtherId(request.otherId());
        if (request.aadhaar() != null && !request.aadhaar().contains("X") && !request.aadhaar().isBlank()) {
            candidate.setAadhaarEncrypted(crypto.encrypt(request.aadhaar().replaceAll("\\D", "")));
        }
        if (request.pan() != null && !request.pan().contains("X") && !request.pan().isBlank()) {
            candidate.setPanEncrypted(crypto.encrypt(request.pan().trim().toUpperCase()));
        }
        replaceSkills(candidate, request.skills() == null ? "" : String.join(", ", request.skills()));
        education.deleteAll(education.findByCandidateIdOrderByPassingYearDesc(id));
        if (request.education() != null) {
            request.education().forEach(item -> {
                if (item.degree() == null || item.degree().isBlank()) return;
                CandidateEducation row = new CandidateEducation();
                row.setCandidate(candidate);
                row.setDegree(item.degree());
                row.setSpecialization(item.specialization());
                row.setInstitution(item.institution());
                row.setPassingYear(item.passingYear());
                row.setScore(item.score());
                education.save(row);
            });
        }
        experience.deleteAll(experience.findByCandidateIdOrderByJoiningDateDesc(id));
        if (request.experience() != null) {
            request.experience().forEach(item -> {
                if (item.company() == null || item.company().isBlank()) return;
                CandidateExperience row = new CandidateExperience();
                row.setCandidate(candidate);
                row.setCompany(item.company());
                row.setDesignation(item.designation());
                row.setJoiningDate(item.joiningDate());
                row.setLeavingDate(item.leavingDate());
                row.setSalary(item.salary());
                row.setResponsibilities(item.responsibilities());
                experience.save(row);
            });
        }
        return candidateView(candidate);
    }

    @Transactional
    public RecruitmentDtos.InterviewResponse schedule(RecruitmentDtos.InterviewRequest request) {
        JobApplication application = requireApplication(request.applicationId());
        if (application.getStatus() == ApplicationStatus.SHORTLISTED || application.getStatus() == ApplicationStatus.INTERVIEW_SCHEDULED) {
            move(application, ApplicationStatus.INTERVIEW_SCHEDULED, null, "Interview scheduled");
        }
        Interview interview = new Interview();
        interview.setApplication(application);
        interview.setCandidate(application.getCandidate());
        interview.setJob(application.getJob());
        if (request.interviewerId() != null) {
            interview.setInterviewer(users.findById(request.interviewerId()).orElseThrow(() -> new NotFoundException("Interviewer not found")));
        }
        interview.setScheduledDate(request.date());
        interview.setScheduledTime(request.time());
        interview.setMode(request.mode());
        interview.setMeetingLink(request.meetingLink());
        interview.setLocation(request.location());
        interview.setRemarks(request.remarks());
        interview.setStatus(InterviewStatus.SCHEDULED);
        interviews.save(interview);
        notifications.email(application.getCandidate().getEmail(), "INTERVIEW_SCHEDULED", "Interview scheduled — " + application.getJob().getTitle(),
                "Your interview is on " + request.date() + " at " + request.time() + " (" + request.mode() + ").", "INTERVIEW", interview.getId());
        auditService.log("INTERVIEW_SCHEDULED", "INTERVIEW", interview.getId().toString(), Map.of());
        return interviewView(interview);
    }

    @Transactional(readOnly = true)
    public List<RecruitmentDtos.InterviewResponse> interviews(UUID applicationId) {
        List<Interview> rows = applicationId == null ? interviews.findAll() : interviews.findByApplicationIdOrderByScheduledDateDesc(applicationId);
        return rows.stream().map(this::interviewView).toList();
    }

    @Transactional
    public RecruitmentDtos.InterviewResponse interviewStatus(UUID id, InterviewStatus status) {
        Interview interview = interviews.findById(id).orElseThrow(() -> new NotFoundException("Interview not found"));
        interview.setStatus(status);
        if (status == InterviewStatus.COMPLETED && ApplicationWorkflow.canTransition(interview.getApplication().getStatus(), ApplicationStatus.INTERVIEWED)) {
            move(interview.getApplication(), ApplicationStatus.INTERVIEWED, null, "Interview completed");
        }
        return interviewView(interview);
    }

    @Transactional
    public RecruitmentDtos.InterviewResponse feedback(UUID id, RecruitmentDtos.Feedback request) {
        Interview interview = interviews.findById(id).orElseThrow(() -> new NotFoundException("Interview not found"));
        InterviewFeedback feedback = feedbackRepository.findByInterviewId(id).orElseGet(InterviewFeedback::new);
        feedback.setInterview(interview);
        feedback.setRating(request.rating());
        feedback.setStrengths(request.strengths());
        feedback.setWeaknesses(request.weaknesses());
        feedback.setRecommendation(request.recommendation());
        feedback.setRemarks(request.remarks());
        if (SecurityUtils.currentUserId() != null) {
            feedback.setSubmittedBy(users.findById(SecurityUtils.currentUserId()).orElse(null));
        }
        feedbackRepository.save(feedback);
        interview.setStatus(InterviewStatus.COMPLETED);
        if (ApplicationWorkflow.canTransition(interview.getApplication().getStatus(), ApplicationStatus.INTERVIEWED)) {
            move(interview.getApplication(), ApplicationStatus.INTERVIEWED, null, "Feedback recorded");
        }
        return interviewView(interview);
    }

    public JobApplication requireApplication(UUID id) {
        return applications.findById(id).orElseThrow(() -> new NotFoundException("Application not found"));
    }

    public Candidate requireCandidate(UUID id) {
        return candidates.findById(id).orElseThrow(() -> new NotFoundException("Candidate not found"));
    }

    public void move(JobApplication application, ApplicationStatus status, String reason, String note) {
        if (!ApplicationWorkflow.canTransition(application.getStatus(), status)) {
            throw new BusinessException("Cannot move an application from " + application.getStatus() + " to " + status + ".");
        }
        ApplicationStatus previous = application.getStatus();
        if (previous != status) {
            application.setStatus(status);
            recordStatus(application, previous, status, reason, note);
            String action = switch (status) {
                case SHORTLISTED -> "CANDIDATE_SHORTLISTED";
                case REJECTED -> "CANDIDATE_REJECTED";
                case SELECTED -> "CANDIDATE_SELECTED";
                default -> "APPLICATION_STATUS";
            };
            auditService.log(action, "APPLICATION", application.getId().toString(), Map.of("from", previous.name(), "to", status.name()));
            if (status == ApplicationStatus.SHORTLISTED) {
                notifications.email(application.getCandidate().getEmail(), "CANDIDATE_SHORTLISTED", "You have been shortlisted",
                        "Your application " + application.getApplicationNumber() + " has been shortlisted.", "APPLICATION", application.getId());
            }
            if (status == ApplicationStatus.SELECTED) {
                notifications.email(application.getCandidate().getEmail(), "CANDIDATE_SELECTED", "Update on your application",
                        "Your application " + application.getApplicationNumber() + " has been selected. Our team will share joining details.", "APPLICATION", application.getId());
            }
        }
    }

    private void recordStatus(JobApplication application, ApplicationStatus from, ApplicationStatus to, String reason, String note) {
        ApplicationStatusEvent event = new ApplicationStatusEvent();
        event.setApplication(application);
        event.setOldStatus(from);
        event.setNewStatus(to);
        event.setReason(reason);
        event.setNote(note);
        event.setChangedAt(Instant.now());
        if (SecurityUtils.currentUserId() != null) {
            event.setChangedBy(users.findById(SecurityUtils.currentUserId()).orElse(null));
        }
        history.save(event);
    }

    private void replaceSkills(Candidate candidate, String raw) {
        if (candidate.getId() != null) {
            skills.deleteByCandidateId(candidate.getId());
        }
        if (raw == null || raw.isBlank()) return;
        Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isBlank()).distinct().limit(30).forEach(name -> {
            CandidateSkill skill = new CandidateSkill();
            skill.setCandidate(candidate);
            skill.setSkillName(name);
            skills.save(skill);
        });
    }

    private void storeOptional(UUID applicationId, String type, MultipartFile file, boolean image) {
        if (file == null || file.isEmpty()) return;
        documents.store("APPLICATION", applicationId, type, file, false, image);
    }

    private RecruitmentDtos.ApplicationCard card(JobApplication application) {
        Candidate candidate = application.getCandidate();
        Job job = application.getJob();
        UUID resumeId = documents.list("APPLICATION", application.getId()).stream()
                .filter(document -> "RESUME".equals(document.getDocumentType()))
                .map(document -> document.getId())
                .findFirst()
                .orElse(null);
        return new RecruitmentDtos.ApplicationCard(application.getId(), application.getApplicationNumber(), candidate.getFullName(),
                job.getTitle(), candidate.getCity() == null ? job.getLocation() : candidate.getCity(), candidate.getTotalExperience(),
                candidate.getExpectedSalary(), application.getStatus(), application.getCreatedAt(), candidate.getId(), job.getId(),
                candidate.getEmail(), candidate.getMobile(), candidate.getHighestQualification(), resumeId);
    }

    private RecruitmentDtos.ApplicationDetail toDetail(JobApplication application) {
        return new RecruitmentDtos.ApplicationDetail(
                application.getId(), application.getApplicationNumber(), application.getStatus(), application.isConsent(), application.getCoverNote(),
                application.getAssignedHr() == null ? null : application.getAssignedHr().getId(),
                application.getAssignedHr() == null ? null : application.getAssignedHr().getFullName(),
                application.getCreatedAt(), candidateView(application.getCandidate()),
                new RecruitmentDtos.JobDtosLite(application.getJob().getId(), application.getJob().getTitle(), application.getJob().getJobCode(), application.getJob().getLocation(), application.getJob().getCompany().getCompanyName()),
                history.findByApplicationIdOrderByChangedAtAsc(application.getId()).stream().map(event -> new RecruitmentDtos.HistoryItem(
                        event.getId(), event.getOldStatus(), event.getNewStatus(),
                        event.getChangedBy() == null ? "System" : event.getChangedBy().getFullName(), event.getChangedAt(), event.getReason(), event.getNote()
                )).toList(),
                notes.findByApplicationIdOrderByCreatedAtAsc(application.getId()).stream().map(note -> new RecruitmentDtos.NoteItem(
                        note.getId(), note.getAuthor() == null ? "Staff" : note.getAuthor().getFullName(), note.getNote(), note.getCreatedAt()
                )).toList()
        );
    }

    private RecruitmentDtos.CandidateView candidateView(Candidate candidate) {
        boolean reveal = SecurityUtils.hasAuthority("SENSITIVE_READ");
        String aadhaarPlain = decrypt(candidate.getAadhaarEncrypted());
        String panPlain = decrypt(candidate.getPanEncrypted());
        return new RecruitmentDtos.CandidateView(
                candidate.getId(), candidate.getFullName(), candidate.getDateOfBirth(), candidate.getGender(), candidate.getMobile(), candidate.getEmail(),
                candidate.getAlternatePhone(), candidate.getAddress(), candidate.getCity(), candidate.getState(), candidate.getPincode(), candidate.getCountry(),
                candidate.getPermanentAddress(), candidate.getFatherName(), candidate.getMotherName(), candidate.getMaritalStatus(),
                candidate.getEmergencyContactName(), candidate.getEmergencyContactPhone(), candidate.getHighestQualification(), candidate.getSpecialization(),
                candidate.getTotalExperience(), candidate.getRelevantExperience(), candidate.getCurrentCompany(), candidate.getCurrentJobTitle(),
                candidate.getCurrentSalary(), candidate.getExpectedSalary(), candidate.getNoticePeriod(), candidate.getPhotoPath(),
                reveal ? aadhaarPlain : SensitiveDataMasker.maskAadhaar(aadhaarPlain),
                reveal ? panPlain : SensitiveDataMasker.maskPan(panPlain),
                candidate.getOtherId(),
                skills.findByCandidateIdOrderBySkillNameAsc(candidate.getId()).stream().map(CandidateSkill::getSkillName).toList(),
                education.findByCandidateIdOrderByPassingYearDesc(candidate.getId()).stream().map(row -> new RecruitmentDtos.EducationItem(row.getId(), row.getDegree(), row.getSpecialization(), row.getInstitution(), row.getPassingYear(), row.getScore())).toList(),
                experience.findByCandidateIdOrderByJoiningDateDesc(candidate.getId()).stream().map(row -> new RecruitmentDtos.ExperienceItem(row.getId(), row.getCompany(), row.getDesignation(), row.getJoiningDate(), row.getLeavingDate(), row.getSalary(), row.getResponsibilities())).toList()
        );
    }

    private RecruitmentDtos.InterviewResponse interviewView(Interview interview) {
        InterviewFeedback feedback = feedbackRepository.findByInterviewId(interview.getId()).orElse(null);
        return new RecruitmentDtos.InterviewResponse(
                interview.getId(), interview.getApplication().getId(), interview.getCandidate().getFullName(), interview.getJob().getTitle(),
                interview.getInterviewer() == null ? null : interview.getInterviewer().getFullName(),
                interview.getScheduledDate(), interview.getScheduledTime(), interview.getMode(), interview.getMeetingLink(), interview.getLocation(),
                interview.getRemarks(), interview.getStatus(),
                feedback == null ? null : new RecruitmentDtos.Feedback(feedback.getRating(), feedback.getStrengths(), feedback.getWeaknesses(), feedback.getRecommendation(), feedback.getRemarks())
        );
    }

    private String decrypt(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return crypto.decrypt(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
