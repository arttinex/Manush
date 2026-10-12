package com.manush.common.storage;

import java.io.InputStream;

/**
 * Provider interface for file storage.
 * Default impl: local temp storage.
 * Production: GoogleDriveStorageProvider (manush.storage.provider=gdrive).
 */
public interface StorageProvider {
    /**
     * @return file identifier (Drive fileId, GridFS id, or local path)
     */
    String store(InputStream file, String filename, String contentType);

    /**
     * @return public-facing URL for display inside <img> or <a> tags
     */
    String toViewUrl(String fileId);

    InputStream retrieve(String fileId);
}
