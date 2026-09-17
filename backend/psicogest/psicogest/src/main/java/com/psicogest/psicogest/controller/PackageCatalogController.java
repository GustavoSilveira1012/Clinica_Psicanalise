package com.psicogest.psicogest.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.psicogest.psicogest.dto.PackagePlanCatalogResponse;
import com.psicogest.psicogest.service.PackageCatalogService;

@RestController
@RequestMapping("/api/v1/package-plans")
@PreAuthorize("isAuthenticated()")
public class PackageCatalogController {

    private final PackageCatalogService service;

    public PackageCatalogController(PackageCatalogService service) {
        this.service = service;
    }

    @GetMapping
    public List<PackagePlanCatalogResponse> list() {
        return service.list();
    }
}
