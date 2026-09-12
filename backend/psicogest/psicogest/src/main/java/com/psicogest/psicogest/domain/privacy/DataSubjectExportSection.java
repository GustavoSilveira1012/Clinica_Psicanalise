package com.psicogest.psicogest.domain.privacy;

import java.util.Map;

/**
 * Seção produzida por um contributor de exportação após autorização e revisão.
 */
public record DataSubjectExportSection(
        String domain,
        Map<String, Object> data
) {
}
