package com.narayan.staffing.document;

import com.narayan.staffing.audit.AuditService;
import com.narayan.staffing.common.NotFoundException;
import com.narayan.staffing.common.SecurityUtils;
import com.narayan.staffing.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final Set<String> SENSITIVE = Set.of("AADHAAR", "PAN", "ID_PROOF", "BANK", "OTHER_ID");

    private final StoredDocumentRepository documents;
    private final DocumentRequirementRepository requirements;
    private final StorageService storageService;
    private final FileValidator fileValidator;
    private final AuditService auditService;

    @Transactional
    public StoredDocument store(String entityType, UUID entityId, String documentType, MultipartFile file, boolean publicVisible, boolean imageOnly) {
        if (imageOnly) {
            fileValidator.validateImage(file);
        } else {
            fileValidator.validate(file);
        }
        try {
            StorageService.StoredFile stored = storageService.store(entityType.toLowerCase(), file.getOriginalFilename(), file.getInputStream(), file.getSize());
            StoredDocument document = new StoredDocument();
            document.setEntityType(entityType);
            document.setEntityId(entityId);
            document.setDocumentType(documentType);
            document.setFileName(stored.fileName());
            document.setStoragePath(stored.storagePath());
            document.setMimeType(file.getContentType());
            document.setFileSize(file.getSize());
            document.setPublicVisible(publicVisible);
            document.setUploadedBy(SecurityUtils.currentUserId());
            return documents.save(document);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read upload", ex);
        }
    }

    @Transactional(readOnly = true)
    public List<StoredDocument> list(String entityType, UUID entityId) {
        return documents.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
    }

    @Transactional(readOnly = true)
    public StoredDocument require(UUID id) {
        return documents.findById(id).orElseThrow(() -> new NotFoundException("Document not found"));
    }

    @Transactional(readOnly = true)
    public Resource loadPublic(StoredDocument document) {
        return storageService.load(document.getStoragePath());
    }

    @Transactional(readOnly = true)
    public Resource download(UUID id) {
        StoredDocument document = documents.findById(id).orElseThrow(() -> new NotFoundException("Document not found"));
        if (SENSITIVE.contains(document.getDocumentType())) {
            auditService.log("SENSITIVE_DOCUMENT_ACCESSED", document.getEntityType(), document.getEntityId().toString(),
                    Map.of("documentId", id.toString(), "type", document.getDocumentType()));
        }
        return storageService.load(document.getStoragePath());
    }

    @Transactional(readOnly = true)
    public StoredDocument requirePublic(UUID id) {
        StoredDocument document = documents.findById(id).orElseThrow(() -> new NotFoundException("File not found"));
        if (!document.isPublicVisible()) {
            throw new NotFoundException("File not found");
        }
        return document;
    }

    @Transactional
    public void delete(UUID id) {
        StoredDocument document = documents.findById(id).orElseThrow(() -> new NotFoundException("Document not found"));
        storageService.delete(document.getStoragePath());
        documents.delete(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentRequirement> requirements(boolean activeOnly) {
        return activeOnly ? requirements.findByActiveTrueOrderByDisplayOrderAsc() : requirements.findAllByOrderByDisplayOrderAsc();
    }

    @Transactional
    public DocumentRequirement saveRequirement(DocumentRequirement input) {
        return requirements.save(input);
    }

    public DocumentRequirement requireRequirement(UUID id) {
        return requirements.findById(id).orElseThrow(() -> new NotFoundException("Requirement not found"));
    }
}
