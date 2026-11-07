package llc.devlab.app.controller;

import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import llc.devlab.app.model.dto.PageResponse;
import llc.devlab.app.model.dto.ProductCreateRequest;
import llc.devlab.app.model.dto.ProductDto;
import llc.devlab.app.model.dto.ProductUpdateRequest;
import llc.devlab.app.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ProductDto> create(@Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    ProductDto update(@PathVariable long productId, @Valid @RequestBody ProductUpdateRequest request) {
        return productService.update(productId, request);
    }

    @GetMapping("/{productId}")
    ProductDto getById(@PathVariable long productId) {
        return productService.getById(productId);
    }

    @GetMapping
    PageResponse<ProductDto> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> sort
    ) {
        return productService.list(page, size, sort);
    }
}
