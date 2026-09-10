package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.ReceivableResponseDTO;
import com.psicogest.psicogest.service.ReceivableService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 37. Controller para contas a receber
 * 
 * GET /receivables/{receivableId} - busca com saldos
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/receivables")
public class ReceivableController {

    private final ReceivableService receivableService;

    public ReceivableController(
            ReceivableService receivableService
    ) {
        this.receivableService = receivableService;
    }

    /**
     * 37, 38. Busca cobrança com saldos e status temporal
     * 
     * GET /api/v1/receivables/{receivableId}
     * 
     * Response:
     * {
     *   "id": "...",
     *   "patientId": 123,
     *   "clinicId": 456,
     *   "appointmentId": 789,
     *   "description": "Sessão de psicanálise",
     *   "grossAmount": 300.00,
     *   "discountAmount": 0.00,
     *   "netAmount": 300.00,
     *   "paidAmount": 200.00,
     *   "outstandingAmount": 100.00,
     *   "overdue": false,
     *   "status": "PARTIALLY_PAID",
     *   "dueDate": "2026-09-15",
     *   "createdAt": "2026-09-01T10:00:00Z"
     * }
     */
    @GetMapping("/{receivableId}")
    @ResponseStatus(HttpStatus.OK)
    public ReceivableResponseDTO findById(
            @PathVariable
            UUID receivableId
    ) {

        log.info(
                "GET /receivables/{}: consultando cobrança",
                receivableId
        );

        return receivableService.findById(
                receivableId
        );
    }
}
