package llc.devlab.app.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import llc.devlab.app.exception.ConflictException;
import llc.devlab.app.exception.ResourceNotFoundException;
import llc.devlab.app.mapper.ProductMapper;
import llc.devlab.app.model.dto.PageResponse;
import llc.devlab.app.model.dto.ProductCreateRequest;
import llc.devlab.app.model.dto.ProductDto;
import llc.devlab.app.model.dto.ProductUpdateRequest;
import llc.devlab.app.model.entity.Product;
import llc.devlab.app.repository.ProductRepository;
import llc.devlab.app.util.PageResponseFactory;
import llc.devlab.app.util.PageableFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("sku", "name", "unitPrice", "availableStock", "createdAt");

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final PageableFactory pageableFactory;
    private final PageResponseFactory pageResponseFactory;

    @Transactional
    public ProductDto create(ProductCreateRequest request) {
        String normalizedSku = normalizeSku(request.sku());
        if (productRepository.existsBySkuIgnoreCase(normalizedSku)) {
            throw new ConflictException("Product SKU already exists.");
        }

        Product product = productMapper.toEntity(request);
        product.setSku(normalizedSku);
        return productMapper.toDto(productRepository.save(product));
    }

    @Transactional
    public ProductDto update(long productId, ProductUpdateRequest request) {
        Product product = findEntity(productId);
        productMapper.updateEntity(request, product);
        return productMapper.toDto(product);
    }

    @Transactional(readOnly = true)
    public ProductDto getById(long productId) {
        return productMapper.toDto(findEntity(productId));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductDto> list(int page, int size, List<String> sort) {
        Pageable pageable = pageableFactory.create(page, size, sort, Sort.by(Sort.Direction.ASC, "name"), ALLOWED_SORT_FIELDS);
        Page<Product> result = productRepository.findAll(pageable);
        return pageResponseFactory.from(result, productMapper::toDto);
    }

    private Product findEntity(long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + productId + " was not found."));
    }

    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }
}
