package com.idb.auth.common.dto.response;

import static com.idb.auth.common.constant.OperationStatus.ERROR;
import static com.idb.auth.common.constant.OperationStatus.SUCCESS;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.idb.auth.common.constant.OperationStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiPageResponse<D> extends ApiResponse<List<D>> {
    @JsonProperty("pageNumber")
    private int pageNumber;
    @JsonProperty("pageSize")
    private int pageSize;
    @JsonProperty("totalElements")
    private long totalElements;
    @JsonProperty("totalPages")
    private int totalPages;

    public static <S, T> ApiPageResponse<T> fromPage(Page<S> page, Function<S, T> mapper) {
        return fromPage(SUCCESS, null, page, mapper);
    }

    public static <T> ApiPageResponse<T> fromPage(OperationStatus status, String message) {
        ApiPageResponse<T> response = new ApiPageResponse<>();
        response.setStatus(status == null ? ERROR : status);
        response.setMessage(message);
        response.setData(List.of());
        response.setPageNumber(-1);
        response.setPageSize(-1);
        response.setTotalElements(-1);
        response.setTotalPages(-1);
        return response;
    }

    @SuppressWarnings("unchecked")
    public static <S, T> ApiPageResponse<T> fromPage(OperationStatus status, String message, Page<S> page,
            Function<S, T> mapper) {
        if (page == null) {
            return ApiPageResponse.fromPage(status, message);
        }
        Stream<S> stream = page.getContent().stream();
        Stream<T> mappedStream = mapper != null ? stream.map(mapper) : stream.map(s -> (T) s);
        List<T> content = mappedStream.filter(Objects::nonNull).collect(Collectors.toList());

        ApiPageResponse<T> response = new ApiPageResponse<>();
        response.setStatus(status);
        response.setMessage(message);
        response.setData(content);
        response.setPageNumber(page.getNumber());
        response.setPageSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        return response;
    }
}
