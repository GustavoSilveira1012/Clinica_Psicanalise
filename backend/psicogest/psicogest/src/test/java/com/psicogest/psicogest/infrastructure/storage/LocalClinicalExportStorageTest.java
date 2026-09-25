package com.psicogest.psicogest.infrastructure.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalClinicalExportStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void isolatesExportsByFinancialEntity() throws Exception {
        String previous = System.getProperty("app.export-storage.local.path");
        System.setProperty("app.export-storage.local.path", tempDir.resolve("exports").toString());
        try {
            LocalClinicalExportStorage storage = new LocalClinicalExportStorage();
            UUID tenantA = UUID.randomUUID();
            UUID tenantB = UUID.randomUUID();
            UUID exportId = UUID.randomUUID();
            byte[] content = "synthetic clinical export".getBytes();

            storage.store(tenantA, exportId, content, "application/octet-stream");

            try (var export = storage.open(tenantA, exportId.toString())) {
                assertThat(export.readAllBytes()).isEqualTo(content);
            }
            assertThatThrownBy(() -> storage.open(tenantB, exportId.toString()))
                    .isInstanceOf(RuntimeException.class);
            assertThat(Files.exists(tempDir.resolve("exports").resolve(tenantA.toString()).resolve(exportId.toString())))
                    .isTrue();
        } finally {
            if (previous == null) {
                System.clearProperty("app.export-storage.local.path");
            } else {
                System.setProperty("app.export-storage.local.path", previous);
            }
        }
    }

    @Test
    void rejectsNonUuidStorageKeys() throws Exception {
        String previous = System.getProperty("app.export-storage.local.path");
        System.setProperty("app.export-storage.local.path", tempDir.resolve("exports").toString());
        try {
            LocalClinicalExportStorage storage = new LocalClinicalExportStorage();
            assertThatThrownBy(() -> storage.open(UUID.randomUUID(), "../../outside"))
                    .isInstanceOf(IllegalArgumentException.class);
        } finally {
            if (previous == null) {
                System.clearProperty("app.export-storage.local.path");
            } else {
                System.setProperty("app.export-storage.local.path", previous);
            }
        }
    }
}
