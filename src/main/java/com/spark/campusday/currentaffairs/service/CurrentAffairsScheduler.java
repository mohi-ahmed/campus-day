package com.spark.campusday.currentaffairs.service;

import com.spark.campusday.currentaffairs.config.CurrentAffairsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrentAffairsScheduler {
    private final CurrentAffairsProperties properties;
    private final CurrentAffairsImportService importer;

    @Scheduled(
            initialDelayString = "${app.current-affairs.import-initial-delay:PT30S}",
            fixedDelayString = "${app.current-affairs.import-fixed-delay:PT1H}")
    public void refreshCurrentAffairs() {
        if (!properties.enabled()) return;
        try {
            var result = importer.importLatest();
            log.info(
                    "Current Affairs import {} completed: fetched={}, persisted={}, rejected={}",
                    result.batchId(),
                    result.fetchedCount(),
                    result.persistedCount(),
                    result.rejectedCount());
        } catch (RuntimeException exception) {
            log.error("Current Affairs import failed; stored feed remains available", exception);
        }
    }
}
