package com.manush.common.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Default storage: saves to ./uploads directory.
 * For dev/university deployment only — not for production use.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "manush.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalStorageProvider implements StorageProvider {

    private static final Path UPLOAD_DIR = Paths.get("uploads");

    @Override
    public String store(InputStream file, String filename, String contentType) {
        try {
            Files.createDirectories(UPLOAD_DIR);
            String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf('.')) : "";
            String savedName = UUID.randomUUID() + ext;
            Files.copy(file, UPLOAD_DIR.resolve(savedName));
            log.debug("Stored file: {}", savedName);
            return savedName;
        } catch (IOException e) {
            throw new RuntimeException("File storage failed", e);
        }
    }

    @Override
    public String toViewUrl(String fileId) {
        return "/uploads/" + fileId;
    }

    @Override
    public InputStream retrieve(String fileId) {
        try {
            return Files.newInputStream(UPLOAD_DIR.resolve(fileId));
        } catch (IOException e) {
            throw new RuntimeException("File retrieval failed: " + fileId, e);
        }
    }
}
