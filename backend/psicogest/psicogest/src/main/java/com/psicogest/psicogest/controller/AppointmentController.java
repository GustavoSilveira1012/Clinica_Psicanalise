package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.appointment.*;
import com.psicogest.psicogest.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(
        "/psychoanalysts/{psychoanalystId}/appointments"
)
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(
            AppointmentService appointmentService
    ) {
        this.appointmentService =
                appointmentService;
    }

    @PostMapping
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
    public List<AppointmentResponseDTO> findAll(
            @PathVariable Long psychoanalystId
    ) {

        return appointmentService
                .findByPsychoanalyst(
                        psychoanalystId
                );
    }

    @GetMapping("/{appointmentId}")
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
public AppointmentResponseDTO noShow(
        @PathVariable Long psychoanalystId,
        @PathVariable Long appointmentId
) {

    return appointmentService.markNoShow(
            psychoanalystId,
            appointmentId
    );
}

    @PostMapping("/{appointmentId}/reschedule")
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