package com.idb.auth.common.dto.request;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApiPageRequest<D> {
    @JsonProperty("pageNumber")
    private int pageNumber = 0;

    @JsonProperty("pageSize")
    private int pageSize = 10;

    @JsonProperty("sortColumn")
    private String sortColumn;

    @JsonProperty("sortOrder")
    private Direction sortOrder;

    @NotNull(message = "Filter is required")
    @JsonProperty("filter")
    private D filter;

    @JsonIgnore
    public Pageable getPageable() {
        if (pageNumber < 0) {
            pageNumber = 0;
        }
        if (sortColumn != null && !sortColumn.isEmpty()) {
            return PageRequest.of(pageNumber, pageSize, sortOrder, sortColumn);
        }
        return PageRequest.of(pageNumber, pageSize);
    }
}
