package com.kardexa.catalog.infrastructure.adapter.in.web;

import com.kardexa.catalog.application.port.in.CreateCategoryUseCase;
import com.kardexa.catalog.application.port.in.CreateCategoryUseCase.CreateCategoryCommand;
import com.kardexa.catalog.application.port.in.ListCategoriesUseCase;
import com.kardexa.catalog.application.port.in.UpdateCategoryUseCase;
import com.kardexa.catalog.application.port.in.UpdateCategoryUseCase.UpdateCategoryCommand;
import com.kardexa.catalog.domain.model.Category;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** {@code /api/v1/categories} según el contrato {@code docs/api/openapi.yaml} (HU-05). */
@RestController
@RequestMapping("/categories")
class CategoryController {

    private final CreateCategoryUseCase createCategory;
    private final UpdateCategoryUseCase updateCategory;
    private final ListCategoriesUseCase listCategories;

    CategoryController(
            CreateCategoryUseCase createCategory,
            UpdateCategoryUseCase updateCategory,
            ListCategoriesUseCase listCategories) {
        this.createCategory = createCategory;
        this.updateCategory = updateCategory;
        this.listCategories = listCategories;
    }

    @GetMapping
    List<CategoryResponse> list() {
        return listCategories.listAll().stream().map(CategoryResponse::from).toList();
    }

    @PostMapping
    ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        Category created = createCategory.create(new CreateCategoryCommand(request.name()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(CategoryResponse.from(created));
    }

    @PutMapping("/{id}")
    CategoryResponse update(@PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
        return CategoryResponse.from(updateCategory.update(
                new UpdateCategoryCommand(id, request.name(), request.activeOrDefault())));
    }

    /** Esquema {@code CategoryRequest}: {@code active} es opcional y vale {@code true} por defecto. */
    record CategoryRequest(
            @NotBlank(message = "El nombre es obligatorio.")
            @Size(min = Category.NAME_MIN_LENGTH, max = Category.NAME_MAX_LENGTH,
                    message = "Debe tener entre {min} y {max} caracteres.")
            String name,
            Boolean active) {

        boolean activeOrDefault() {
            return active == null || active;
        }
    }

    /** Esquema {@code Category}. */
    record CategoryResponse(UUID id, String name, boolean active) {

        static CategoryResponse from(Category category) {
            return new CategoryResponse(category.id(), category.name(), category.isActive());
        }
    }
}
