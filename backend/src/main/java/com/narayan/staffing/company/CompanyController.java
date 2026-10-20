package com.narayan.staffing.company;

import com.narayan.staffing.document.DocumentService;
import com.narayan.staffing.domain.CompanyStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final DocumentService documentService;

    @GetMapping
    @PreAuthorize("hasAuthority('COMPANY_READ')")
    public com.narayan.staffing.common.PageResponse<CompanyDtos.CompanyResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) CompanyStatus status,
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        return companyService.list(q, status, city, page, size, sort, direction);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COMPANY_READ')")
    public CompanyDtos.CompanyResponse get(@PathVariable UUID id) {
        return companyService.get(id);
    }

    @GetMapping("/{id}/profile")
    @PreAuthorize("hasAuthority('COMPANY_READ')")
    public CompanyDtos.CompanyProfile profile(@PathVariable UUID id) {
        return companyService.profile(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('COMPANY_WRITE')")
    public CompanyDtos.CompanyResponse create(@Valid @RequestBody CompanyDtos.CompanyRequest request) {
        return companyService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('COMPANY_WRITE')")
    public CompanyDtos.CompanyResponse update(@PathVariable UUID id, @Valid @RequestBody CompanyDtos.CompanyRequest request) {
        return companyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('COMPANY_WRITE')")
    public void delete(@PathVariable UUID id) {
        companyService.delete(id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('COMPANY_WRITE')")
    public CompanyDtos.CompanyResponse status(@PathVariable UUID id, @Valid @RequestBody CompanyDtos.StatusRequest request) {
        return companyService.setStatus(id, request.status());
    }

    @PostMapping("/{id}/logo")
    @PreAuthorize("hasAuthority('COMPANY_WRITE')")
    public CompanyDtos.CompanyResponse logo(@PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        var doc = documentService.store("COMPANY", id, "LOGO", file, true, true);
        companyService.updateLogo(id, "/api/public/media/" + doc.getId());
        return companyService.get(id);
    }

    @PostMapping("/{id}/covers")
    @PreAuthorize("hasAuthority('COMPANY_WRITE')")
    public CompanyDtos.CompanyResponse cover(@PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        var doc = documentService.store("COMPANY", id, "COVER", file, true, true);
        return companyService.addCover(id, "/api/public/media/" + doc.getId());
    }

    @DeleteMapping("/{id}/covers")
    @PreAuthorize("hasAuthority('COMPANY_WRITE')")
    public CompanyDtos.CompanyResponse removeCover(@PathVariable UUID id, @RequestParam String path) {
        return companyService.removeCover(id, path);
    }
}
