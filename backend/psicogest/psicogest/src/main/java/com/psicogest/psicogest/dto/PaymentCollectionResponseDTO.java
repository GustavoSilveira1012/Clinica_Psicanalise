package com.psicogest.psicogest.dto;

public record PaymentCollectionResponseDTO(
        PaymentResponseDTO payment,
        PaymentAllocationResponseDTO allocation
) {
}
