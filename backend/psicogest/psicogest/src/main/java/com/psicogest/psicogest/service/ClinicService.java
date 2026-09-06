package com.psicogest.psicogest.service;

import com.psicogest.psicogest.dto.clinic.ClinicCreateDTO;
import com.psicogest.psicogest.dto.clinic.ClinicResponseDTO;
import com.psicogest.psicogest.exception.CnpjAlreadyExistsException;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.model.entity.Clinic;
import com.psicogest.psicogest.repository.ClinicRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClinicService {

    private final ClinicRepository clinicRepository;

    public ClinicService(ClinicRepository clinicRepository) {
        this.clinicRepository = clinicRepository;
    }

    public ClinicResponseDTO create(ClinicCreateDTO dto) {

        if (dto.cnpj() != null
                && !dto.cnpj().isBlank()
                && clinicRepository.existsByCnpj(dto.cnpj())) {

            throw new CnpjAlreadyExistsException(
                    "Já existe uma clínica cadastrada com este CNPJ");
        }

        Clinic clinic = Clinic.builder()
                .name(dto.name())
                .cnpj(dto.cnpj())
                .active(true)
                .build();

        Clinic savedClinic = clinicRepository.save(clinic);

        return toResponseDTO(savedClinic);
    }

    public List<ClinicResponseDTO> findAll() {

        return clinicRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public ClinicResponseDTO findById(Long id) {

        Clinic clinic = findClinicById(id);

        return toResponseDTO(clinic);
    }

    private Clinic findClinicById(Long id) {

        return clinicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Clínica não encontrada com o ID: " + id));
    }

    private ClinicResponseDTO toResponseDTO(Clinic clinic) {

        return new ClinicResponseDTO(
                clinic.getId(),
                clinic.getName(),
                clinic.getCnpj(),
                clinic.getActive());
    }
}