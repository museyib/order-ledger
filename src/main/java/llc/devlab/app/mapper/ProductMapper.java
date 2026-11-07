package llc.devlab.app.mapper;

import llc.devlab.app.model.dto.ProductCreateRequest;
import llc.devlab.app.model.dto.ProductDto;
import llc.devlab.app.model.dto.ProductUpdateRequest;
import llc.devlab.app.model.entity.Product;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface ProductMapper {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "sku", source = "sku")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "unitPrice", source = "unitPrice")
    @Mapping(target = "availableStock", source = "availableStock")
    Product toEntity(ProductCreateRequest request);

    ProductDto toDto(Product product);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sku", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "unitPrice", source = "unitPrice")
    @Mapping(target = "availableStock", source = "availableStock")
    void updateEntity(ProductUpdateRequest request, @MappingTarget Product product);
}
