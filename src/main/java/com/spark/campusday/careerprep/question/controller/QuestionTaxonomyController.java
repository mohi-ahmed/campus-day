package com.spark.campusday.careerprep.question.controller;

import com.spark.campusday.careerprep.question.dto.TaxonomyNodeRequest;
import com.spark.campusday.careerprep.question.dto.TaxonomyNodeResponse;
import com.spark.campusday.careerprep.question.service.QuestionTaxonomyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@ConditionalOnProperty(name = "app.career-prep.enabled", havingValue = "true")
@Validated
@RestController
@RequestMapping("/api/internal/career-prep/taxonomy")
@RequiredArgsConstructor
public class QuestionTaxonomyController {
    private final QuestionTaxonomyService service;

    @PostMapping("/nodes")
    public List<TaxonomyNodeResponse> upsert(
            @Valid @NotEmpty @Size(max = 2000)
            @RequestBody List<TaxonomyNodeRequest> requests) {
        return service.upsert(requests);
    }

    @GetMapping("/children")
    public List<TaxonomyNodeResponse> children(
            @RequestParam @NotBlank String parentCode) {
        return service.children(parentCode);
    }
}
