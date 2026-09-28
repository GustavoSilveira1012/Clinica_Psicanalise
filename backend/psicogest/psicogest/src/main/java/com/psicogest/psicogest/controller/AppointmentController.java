package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.appointment.*;
import com.psicogest.psicogest.service.AppointmentService;
import com.psicogest.psicogest.service.AppointmentSeriesOperationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(
        "/psychoanalysts/{psychoanalystId}/appointments"
)
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AppointmentSeriesOperationService seriesOperationService;

    public AppointmentController(
            AppointmentService appointmentService,
            AppointmentSeriesOperationService seriesOperationService
    ) {
        this.appointmentService =
                appointmentService;
        this.seriesOperationService =
                seriesOperationService;
    }

    @PostMapping
    @PreAuthorize("""
            @identityAuthorization.isPsychoanalystSelf(
                authentication,
                #psychoanalystId
            )
            """)
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponseDTO create(
            @PathVariable Long psychoanalystId,
            @Valid
            @RequestBody AppointmentCreateDTO dto
    ) {

        return appointmentService.create(
                psychoanalystId,
                dto
        );
    }

    @PostMapping("/recurring")
    @PreAuthorize("@identityAuthorization.isPsychoanalystSelf(authentication, #psychoanalystId)")
    @ResponseStatus(HttpStatus.CREATED)
    public List<AppointmentResponseDTO>
    createRecurring(
            @PathVariable Long psychoanalystId,
            @Valid
            @RequestBody RecurringAppointmentCreateDTO dto
    ) {

        return appointmentService
                .createWeeklyRecurring(
                        psychoanalystId,
                        dto
                );
    }

    @GetMapping
    @PreAuthorize("@identityAuthorization.isPsychoanalystSelf(authentication, #psychoanalystId)")
    public List<AppointmentResponseDTO> findAll(
            @PathVariable Long psychoanalystId
    ) {

        return appointmentService
                .findByPsychoanalyst(
                        psychoanalystId
                );
    }

    @GetMapping("/{appointmentId}")
    @PreAuthorize("""
            @clinicalAuthorizationService.canReadAppointment(
                authentication,
                #appointmentId
            )
            """)
    public AppointmentResponseDTO findById(
            @PathVariable Long psychoanalystId,
            @PathVariable Long appointmentId
    ) {

        return appointmentService.findById(
                psychoanalystId,
                appointmentId
        );
    }

    @PatchMapping("/{appointmentId}/cancel")
    @PreAuthorize("@identityAuthorization.isPsychoanalystSelf(authentication, #psychoanalystId)")
    public AppointmentResponseDTO cancel(
            @PathVariable Long psychoanalystId,
            @PathVariable Long appointmentId,
            @Valid
            @RequestBody AppointmentCancelDTO dto
    ) {

        return appointmentService.cancel(
                psychoanalystId,
                appointmentId,
                dto
        );
    }

@PatchMapping("/{appointmentId}/confirm")
@PreAuthorize("@identityAuthorization.isPsychoanalystSelf(authentication, #psychoanalystId)")
public AppointmentResponseDTO confirm(
        @PathVariable Long psychoanalystId,
        @PathVariable Long appointmentId
) {

    return appointmentService.confirm(
            psychoanalystId,
            appointmentId
    );
}

@PatchMapping("/{appointmentId}/complete")
@PreAuthorize("@identityAuthorization.isPsychoanalystSelf(authentication, #psychoanalystId)")
public AppointmentResponseDTO complete(
        @PathVariable Long psychoanalystId,
        @PathVariable Long appointmentId
) {

    return appointmentService.complete(
            psychoanalystId,
            appointmentId
    );
}

@PatchMapping("/{appointmentId}/no-show")
@PreAuthorize("@identityAuthorization.isPsychoanalystSelf(authentication, #psychoanalystId)")
public AppointmentResponseDTO noShow(
        @PathVariable Long psychoanalystId,
        @PathVariable Long appointmentId
) {

    return appointmentService.markNoShow(
            psychoanalystId,
            appointmentId
    );
}

@PatchMapping(
        "/{appointmentId}/series/cancel"
)
@PreAuthorize("@identityAuthorization.isPsychoanalystSelf(authentication, #psychoanalystId)")
public List<AppointmentResponseDTO>
cancelSeriesScope(
        @PathVariable Long psychoanalystId,
        @PathVariable Long appointmentId,
        @Valid
        @RequestBody RecurringAppointmentCancelDTO dto
) {

    return seriesOperationService.cancel(
            psychoanalystId,
            appointmentId,
            dto
    );
}

@PostMapping(
        "/{appointmentId}/series/reschedule"
)
@PreAuthorize("@identityAuthorization.isPsychoanalystSelf(authentication, #psychoanalystId)")
public List<AppointmentResponseDTO>
rescheduleSeriesScope(
        @PathVariable Long psychoanalystId,
        @PathVariable Long appointmentId,
        @Valid
        @RequestBody RecurringAppointmentRescheduleDTO dto
) {

    return seriesOperationService.reschedule(
            psychoanalystId,
            appointmentId,
            dto
    );
}

    @PostMapping("/{appointmentId}/reschedule")
    @PreAuthorize("@identityAuthorization.isPsychoanalystSelf(authentication, #psychoanalystId)")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponseDTO reschedule(
            @PathVariable Long psychoanalystId,
            @PathVariable Long appointmentId,
            @Valid
            @RequestBody AppointmentRescheduleDTO dto
    ) {

        return appointmentService.reschedule(
                psychoanalystId,
                appointmentId,
                dto
        );
    }
}
