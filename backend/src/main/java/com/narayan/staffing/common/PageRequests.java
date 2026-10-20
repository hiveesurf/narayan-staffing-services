package com.narayan.staffing.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PageRequests {

    private PageRequests() {
    }

    public static Pageable of(Integer page, Integer size, String sort, String direction, Set<String> allowed, String fallback) {
        int pageNumber = page == null ? 0 : Math.max(page, 0);
        int pageSize = size == null ? 20 : Math.min(Math.max(size, 1), 100);
        String property = sort != null && allowed.contains(sort) ? sort : fallback;
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(pageNumber, pageSize, Sort.by(dir, property));
    }
}
