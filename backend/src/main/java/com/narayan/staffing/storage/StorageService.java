package com.narayan.staffing.storage;

import org.springframework.core.io.Resource;

import java.io.InputStream;

public interface StorageService {
    StoredFile store(String folder, String originalName, InputStream input, long size);

    Resource load(String storagePath);

    void delete(String storagePath);

    record StoredFile(String storagePath, String fileName) {
    }
}
