package com.psicogest.psicogest.service.finance;

import com.psicogest.psicogest.model.entity.ReceivableCancellation.StatusConverter;
import com.psicogest.psicogest.model.entity.ReceivableCancellation.ReceivableCancellationStatus;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ReceivableCancellationMappingTest {
    private final StatusConverter converter = new StatusConverter();

    @Test void preservesStoredSettlingStatusWithoutChangingTheApi() {
        assertThat(converter.convertToDatabaseColumn(ReceivableCancellationStatus.PENDING)).isEqualTo("SETTLING");
        for (var status : ReceivableCancellationStatus.values()) {
            assertThat(converter.convertToEntityAttribute(converter.convertToDatabaseColumn(status))).isEqualTo(status);
        }
    }

    @Test void preservesNullAndRejectsUnknownState() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThatThrownBy(() -> converter.convertToEntityAttribute("PAID")).isInstanceOf(IllegalArgumentException.class);
    }
}
