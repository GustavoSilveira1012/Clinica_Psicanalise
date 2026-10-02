package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.service.PublicPlanCatalogService;
import com.psicogest.psicogest.service.PublicPlanCatalogService.PublicPlansResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/plans")
public class PublicPlanCatalogController {
    private final PublicPlanCatalogService catalog;

    public PublicPlanCatalogController(PublicPlanCatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public ResponseEntity<PublicPlansResponse> plans() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(catalog.publicPlans());
    }
}
