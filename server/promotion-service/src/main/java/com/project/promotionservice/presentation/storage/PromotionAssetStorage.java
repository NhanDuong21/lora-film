package com.project.promotionservice.presentation.storage;

import org.springframework.core.io.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

public interface PromotionAssetStorage {

    Logger LOGGER = LoggerFactory.getLogger(PromotionAssetStorage.class);

    StoredAsset storeCover(MultipartFile file);

    LoadedAsset load(String storageKey);

    void delete(String storageKey);

    String provider();

    default void deleteAfterCommit(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) return;
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            delete(storageKey);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteQuietly(storageKey, "committed asset replacement");
            }
        });
    }

    default void deleteOnRollback(String storageKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    deleteQuietly(storageKey, "rolled-back asset upload");
                }
            }
        });
    }

    private void deleteQuietly(String storageKey, String operation) {
        try {
            delete(storageKey);
        } catch (RuntimeException exception) {
            LOGGER.warn("Unable to clean up promotion asset {} after {}",
                    storageKey, operation, exception);
        }
    }

    record StoredAsset(
            String storageKey,
            String url,
            String contentType,
            long bytes) {
    }

    record LoadedAsset(Resource resource, String contentType) {
    }
}
