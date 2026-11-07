package llc.devlab.app.util;

import java.util.List;
import java.util.Set;

import llc.devlab.app.config.PaginationProperties;
import llc.devlab.app.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class PageableFactory {

    private final int maxPageSize;

    public PageableFactory(PaginationProperties paginationProperties) {
        this.maxPageSize = paginationProperties.maxPageSize();
    }

    public Pageable create(int page, int size, List<String> sortParams, Sort defaultSort, Set<String> allowedSortFields) {
        if (page < 0) {
            throw new BadRequestException("Page must be zero or greater.");
        }
        if (size <= 0) {
            throw new BadRequestException("Size must be greater than zero.");
        }
        if (size > maxPageSize) {
            throw new BadRequestException("Size exceeds the maximum page size of " + maxPageSize + ".");
        }

        Sort sort = sortParams == null || sortParams.isEmpty()
                ? defaultSort
                : parseSort(sortParams, allowedSortFields);

        return PageRequest.of(page, size, sort);
    }

    private Sort parseSort(List<String> sortParams, Set<String> allowedSortFields) {
        Sort combined = Sort.unsorted();
        for (int i = 0; i < sortParams.size(); i++) {
            String sortParam = sortParams.get(i);
            if (!sortParam.contains(",") && i + 1 < sortParams.size()
                    && isDirection(sortParams.get(i + 1))) {
                sortParam = sortParam + "," + sortParams.get(++i);
            }

            String[] parts = sortParam.split(",");
            if (parts.length == 0 || parts.length > 2) {
                throw new BadRequestException("Sort must follow field,direction.");
            }

            String property = parts[0].trim();
            if (!allowedSortFields.contains(property)) {
                throw new BadRequestException("Unsupported sort field: " + property);
            }

            Sort.Direction direction = parts.length == 2
                    ? parseDirection(parts[1].trim())
                    : Sort.Direction.ASC;

            combined = combined.and(Sort.by(direction, property));
        }
        return combined;
    }

    private boolean isDirection(String value) {
        return "asc".equalsIgnoreCase(value) || "desc".equalsIgnoreCase(value);
    }

    private Sort.Direction parseDirection(String direction) {
        try {
            return Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Unsupported sort direction: " + direction);
        }
    }
}
