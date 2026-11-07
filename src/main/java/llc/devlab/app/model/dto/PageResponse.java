package llc.devlab.app.model.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
