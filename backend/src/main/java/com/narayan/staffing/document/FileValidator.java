package com.narayan.staffing.document;

import com.narayan.staffing.common.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class FileValidator {

    private static final long MAX = 10 * 1024 * 1024;
    private static final Set<String> EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png", "doc", "docx");
    private static final Map<String, String> MIME = Map.of(
            "pdf", "application/pdf",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("A file is required.");
        }
        if (file.getSize() > MAX) {
            throw new BusinessException("Files must be 10 MB or smaller.");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = extension(name);
        if (!EXTENSIONS.contains(ext)) {
            throw new BusinessException("Allowed files are PDF, JPG, JPEG, PNG, DOC and DOCX.");
        }
        String mime = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String expected = MIME.get(ext);
        if (!mime.equals(expected) && !(ext.equals("jpg") && mime.equals("image/jpg"))) {
            throw new BusinessException("The file type does not match its contents.");
        }
        try (InputStream in = file.getInputStream()) {
            byte[] header = in.readNBytes(8);
            if (!matchesMagic(ext, header)) {
                throw new BusinessException("The file content is not a valid " + ext.toUpperCase(Locale.ROOT) + " file.");
            }
        } catch (IOException ex) {
            throw new BusinessException("The file could not be read.");
        }
    }

    public void validateImage(MultipartFile file) {
        validate(file);
        String ext = extension(file.getOriginalFilename());
        if (!Set.of("jpg", "jpeg", "png").contains(ext)) {
            throw new BusinessException("Please upload a JPG or PNG image.");
        }
    }

    private boolean matchesMagic(String ext, byte[] header) {
        if (header.length < 4) {
            return false;
        }
        return switch (ext) {
            case "pdf" -> header[0] == '%' && header[1] == 'P' && header[2] == 'D' && header[3] == 'F';
            case "jpg", "jpeg" -> (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8;
            case "png" -> (header[0] & 0xFF) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G';
            case "docx" -> header[0] == 'P' && header[1] == 'K';
            case "doc" -> (header[0] & 0xFF) == 0xD0 && (header[1] & 0xFF) == 0xCF;
            default -> false;
        };
    }

    private String extension(String name) {
        int dot = name == null ? -1 : name.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
