package com.psicogest.psicogest.dto.user;

import com.psicogest.psicogest.model.enums.UserRole;

public class UserResponseDTO {

    public UserResponseDTO(Long id, String name, String email, UserRole role, Boolean active) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.active = active;
    }

    private Long id;
    private String name;
    private String email;
    private UserRole role;
    private Boolean active;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    public Boolean getActive() {
        return active;
    }
}
