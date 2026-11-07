package llc.devlab.app.util;

import java.util.List;
import java.util.function.Function;

import llc.devlab.app.model.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class PageResponseFactory {

    public <T, R> PageResponse<R> from(Page<T> page, Function<T, R> mapper) {
        List<R> items = page.getContent().stream().map(mapper).toList();
        return new PageResponse<>(
                items,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
