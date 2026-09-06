package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.user.UserCreateDTO;
import com.psicogest.psicogest.dto.user.UserResponseDTO;
import com.psicogest.psicogest.dto.user.UserUpdateDTO;
import com.psicogest.psicogest.service.UserService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponseDTO> findAll() {

        return userService.findAll();
    }

    @GetMapping("/{id}")
    public UserResponseDTO findById(
            @PathVariable Long id) {

        return userService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDTO create(
            @Valid @RequestBody UserCreateDTO dto) {

        return userService.create(dto);
    }

    @PutMapping("/{id}")
    public UserResponseDTO update(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateDTO dto) {

        return userService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id) {

        userService.delete(id);
    }
}