package com.project.userservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    Logger LOGGER = LoggerFactory.getLogger(FileStorageService.class);

    StoredFile storeAvatar(MultipartFile file);

    StoredFile storeEmployeeDocument(MultipartFile file);

    Resource load(String category, String publicId);

    void delete(String category, String publicId);

    default void deleteAfterCommit(String category, String publicId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            delete(category, publicId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    delete(category, publicId);
                } catch (RuntimeException exception) {
                    LOGGER.error("Unable to remove committed cloud asset category={} publicId={}",
                            category, publicId, exception);
                }
            }
        });
    }

    default void deleteOnRollback(String category, String publicId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    try {
                        delete(category, publicId);
                    } catch (RuntimeException exception) {
                        LOGGER.error("Unable to remove rolled-back cloud asset category={} publicId={}",
                                category, publicId, exception);
                    }
                }
            }
        });
    }

    record StoredFile(String publicId, String fileUrl, String contentType, long fileSize) {
    }
}
