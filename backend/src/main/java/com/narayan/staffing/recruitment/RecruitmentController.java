package com.narayan.staffing.recruitment;

import com.narayan.staffing.common.PageResponse;
import com.narayan.staffing.document.DocumentRequirement;
import com.narayan.staffing.document.DocumentService;
import com.narayan.staffing.domain.ApplicationStatus;
import com.narayan.staffing.domain.InterviewStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class RecruitmentController {

    private final RecruitmentService recruitmentService;
    private final DocumentService documentService;

    @GetMapping("/api/public/document-requirements")
    public List<DocumentRequirement> requirements() {
        return documentService.requirements(true);
    }

    @PostMapping(value = "/api/public/jobs/{slug}/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RecruitmentDtos.ApplyResponse apply(@PathVariable String slug,
                                               @RequestPart("data") @Valid RecruitmentDtos.ApplyRequest data,
                                               @RequestPart(value = "resume", required = false) MultipartFile resume,
                                               @RequestPart(value = "photograph", required = false) MultipartFile photograph,
                                               @RequestPart(value = "idProof", required = false) MultipartFile idProof) {
        return recruitmentService.apply(slug, data, resume, photograph, idProof);
    }

    @GetMapping("/api/applications")
    @PreAuthorize("hasAuthority('APPLICATION_READ')")
    public PageResponse<RecruitmentDtos.ApplicationCard> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID jobId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) BigDecimal minExperience,
            @RequestParam(required = false) BigDecimal maxSalary,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return recruitmentService.search(q, jobId, categoryId, location, status, minExperience, maxSalary, from, to, page, size);
    }

    @GetMapping("/api/applications/board")
    @PreAuthorize("hasAuthority('APPLICATION_READ')")
    public Map<String, List<RecruitmentDtos.ApplicationCard>> board(@RequestParam(required = false) UUID jobId) {
        return recruitmentService.board(jobId);
    }

    @GetMapping("/api/applications/{id}")
    @PreAuthorize("hasAuthority('APPLICATION_READ')")
    public RecruitmentDtos.ApplicationDetail get(@PathVariable UUID id) {
        return recruitmentService.detail(id);
    }

    @PatchMapping("/api/applications/{id}/status")
    @PreAuthorize("hasAuthority('APPLICATION_WRITE')")
    public RecruitmentDtos.ApplicationDetail status(@PathVariable UUID id, @Valid @RequestBody RecruitmentDtos.StatusChange request) {
        return recruitmentService.changeStatus(id, request.status(), request.reason(), request.note());
    }

    @PostMapping("/api/applications/{id}/shortlist")
    @PreAuthorize("hasAuthority('APPLICATION_WRITE')")
    public RecruitmentDtos.ApplicationDetail shortlist(@PathVariable UUID id) {
        return recruitmentService.changeStatus(id, ApplicationStatus.SHORTLISTED, null, "Shortlisted");
    }

    @PostMapping("/api/applications/bulk-status")
    @PreAuthorize("hasAuthority('APPLICATION_WRITE')")
    public Map<String, Integer> bulk(@Valid @RequestBody RecruitmentDtos.BulkStatus request) {
        return Map.of("updated", recruitmentService.bulkStatus(request.ids(), request.status(), request.note()));
    }

    @PatchMapping("/api/applications/{id}/assign")
    @PreAuthorize("hasAuthority('APPLICATION_WRITE')")
    public RecruitmentDtos.ApplicationDetail assign(@PathVariable UUID id, @Valid @RequestBody RecruitmentDtos.AssignHr request) {
        return recruitmentService.assign(id, request.userId());
    }

    @PostMapping("/api/applications/{id}/notes")
    @PreAuthorize("hasAuthority('APPLICATION_WRITE')")
    public RecruitmentDtos.NoteItem note(@PathVariable UUID id, @Valid @RequestBody RecruitmentDtos.NoteRequest request) {
        return recruitmentService.addNote(id, request.note());
    }

    @GetMapping("/api/candidates")
    @PreAuthorize("hasAuthority('CANDIDATE_READ')")
    public PageResponse<RecruitmentDtos.CandidateView> candidates(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return recruitmentService.candidates(q, city, page, size);
    }

    @GetMapping("/api/candidates/{id}")
    @PreAuthorize("hasAuthority('CANDIDATE_READ')")
    public RecruitmentDtos.CandidateView candidate(@PathVariable UUID id) {
        return recruitmentService.candidate(id);
    }

    @PutMapping("/api/candidates/{id}")
    @PreAuthorize("hasAuthority('CANDIDATE_WRITE')")
    public RecruitmentDtos.CandidateView updateCandidate(@PathVariable UUID id, @Valid @RequestBody RecruitmentDtos.CandidateUpdate request) {
        return recruitmentService.updateCandidate(id, request);
    }

    @GetMapping("/api/interviews")
    @PreAuthorize("hasAuthority('INTERVIEW_READ')")
    public List<RecruitmentDtos.InterviewResponse> interviews(@RequestParam(required = false) UUID applicationId) {
        return recruitmentService.interviews(applicationId);
    }

    @PostMapping("/api/interviews")
    @PreAuthorize("hasAuthority('INTERVIEW_WRITE')")
    public RecruitmentDtos.InterviewResponse schedule(@Valid @RequestBody RecruitmentDtos.InterviewRequest request) {
        return recruitmentService.schedule(request);
    }

    @PatchMapping("/api/interviews/{id}/status")
    @PreAuthorize("hasAuthority('INTERVIEW_WRITE')")
    public RecruitmentDtos.InterviewResponse interviewStatus(@PathVariable UUID id, @Valid @RequestBody RecruitmentDtos.InterviewStatusUpdate request) {
        return recruitmentService.interviewStatus(id, request.status());
    }

    @PostMapping("/api/interviews/{id}/feedback")
    @PreAuthorize("hasAuthority('INTERVIEW_WRITE')")
    public RecruitmentDtos.InterviewResponse feedback(@PathVariable UUID id, @RequestBody RecruitmentDtos.Feedback request) {
        return recruitmentService.feedback(id, request);
    }
}
