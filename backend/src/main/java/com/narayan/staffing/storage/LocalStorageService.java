package com.narayan.staffing.storage;

import com.narayan.staffing.common.NotFoundException;
import com.narayan.staffing.config.AppProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private final Path root;

    public LocalStorageService(AppProperties properties) {
        this.root = Path.of(properties.getStorage().getPath()).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(root);
    }

    @Override
    public StoredFile store(String folder, String originalName, InputStream input, long size) {
        String safeName = originalName == null ? "file" : Path.of(originalName).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
        String storedName = UUID.randomUUID() + "-" + safeName;
        Path directory = root.resolve(folder).normalize();
        if (!directory.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage folder");
        }
        try {
            Files.createDirectories(directory);
            Path target = directory.resolve(storedName);
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredFile(root.relativize(target).toString(), safeName);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not store file", ex);
        }
    }

    @Override
    public Resource load(String storagePath) {
        Path path = root.resolve(storagePath).normalize();
        if (!path.startsWith(root) || !Files.exists(path)) {
            throw new NotFoundException("File not found");
        }
        return new FileSystemResource(path);
    }

    @Override
    public void delete(String storagePath) {
        if (storagePath == null) {
            return;
        }
        Path path = root.resolve(storagePath).normalize();
        if (!path.startsWith(root)) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // A missing file should not block the business record update.
        }
    }
}
