package com.manush.common.storage;

import com.manush.common.exception.ValidationException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

@Component
public class FileSecurityValidator {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");
    private static final Set<String> ALLOWED_DOC_EXTENSIONS = Set.of(".pdf", ".jpg", ".jpeg", ".png");

    private static final List<String> ALLOWED_IMAGE_MIMES = List.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final List<String> ALLOWED_DOC_MIMES = List.of(
            "application/pdf", "image/jpeg", "image/png"
    );

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final long MAX_DOC_SIZE = 10 * 1024 * 1024;  // 10 MB

    public void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Image file cannot be empty");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new ValidationException("File size exceeds maximum image limit of 5MB");
        }

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf('.')) : "";
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(ext)) {
            throw new ValidationException("Unsupported image format: " + ext);
        }

        String mime = file.getContentType();
        if (mime == null || !ALLOWED_IMAGE_MIMES.contains(mime.toLowerCase())) {
            throw new ValidationException("Invalid MIME type for image: " + mime);
        }

        verifyImageMagicBytes(file);
    }

    public void validateDocumentFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Document file cannot be empty");
        }
        if (file.getSize() > MAX_DOC_SIZE) {
            throw new ValidationException("File size exceeds limit of 10MB");
        }

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf('.')) : "";
        if (!ALLOWED_DOC_EXTENSIONS.contains(ext)) {
            throw new ValidationException("Unsupported document format: " + ext);
        }

        String mime = file.getContentType();
        if (mime == null || !ALLOWED_DOC_MIMES.contains(mime.toLowerCase())) {
            throw new ValidationException("Invalid MIME type for document: " + mime);
        }
    }

    private void verifyImageMagicBytes(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[8];
            int read = is.read(header);
            if (read < 4) {
                throw new ValidationException("Invalid file: corrupted header");
            }
            // JPEG: FF D8 FF
            boolean isJpeg = (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF);
            // PNG: 89 50 4E 47
            boolean isPng = (header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47);
            // WEBP / RIFF: 52 49 46 46
            boolean isRiff = (header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46);

            if (!isJpeg && !isPng && !isRiff) {
                throw new ValidationException("Corrupted or tampered image content");
            }
        } catch (IOException e) {
            throw new ValidationException("Failed to read and inspect file");
        }
    }
}
