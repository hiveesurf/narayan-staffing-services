package com.narayan.staffing.document;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping("/api/documents")
    @PreAuthorize("isAuthenticated()")
    public List<StoredDocument> list(@RequestParam String entityType, @RequestParam UUID entityId) {
        return documentService.list(entityType, entityId);
    }

    @PostMapping(value = "/api/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public StoredDocument upload(@RequestParam String entityType,
                                 @RequestParam UUID entityId,
                                 @RequestParam String documentType,
                                 @RequestParam(defaultValue = "false") boolean publicVisible,
                                 @RequestPart("file") MultipartFile file) {
        return documentService.store(entityType, entityId, documentType, file, publicVisible, false);
    }

    @GetMapping("/api/documents/{id}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        StoredDocument meta = documentService.require(id);
        Resource resource = documentService.download(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + meta.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(meta.getMimeType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : meta.getMimeType()))
                .body(resource);
    }

    @DeleteMapping("/api/documents/{id}")
    @PreAuthorize("isAuthenticated()")
    public void delete(@PathVariable UUID id) {
        documentService.delete(id);
    }

    @GetMapping("/api/public/media/{id}")
    public ResponseEntity<Resource> publicMedia(@PathVariable UUID id) {
        StoredDocument meta = documentService.requirePublic(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .contentType(MediaType.parseMediaType(meta.getMimeType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : meta.getMimeType()))
                .body(documentService.loadPublic(meta));
    }

    @GetMapping("/api/settings/document-requirements")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE') or hasAuthority('CMS_READ') or hasAuthority('JOB_READ')")
    public List<DocumentRequirement> requirements() {
        return documentService.requirements(false);
    }

    @PutMapping("/api/settings/document-requirements/{id}")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public DocumentRequirement updateRequirement(@PathVariable UUID id, @RequestBody RequirementUpdate update) {
        DocumentRequirement requirement = documentService.requireRequirement(id);
        requirement.setLabel(update.label());
        requirement.setDescription(update.description());
        requirement.setRequired(update.required());
        requirement.setActive(update.active());
        requirement.setDisplayOrder(update.displayOrder());
        return documentService.saveRequirement(requirement);
    }

    public record RequirementUpdate(@NotBlank String label, String description, boolean required, boolean active, int displayOrder) {
    }
}
