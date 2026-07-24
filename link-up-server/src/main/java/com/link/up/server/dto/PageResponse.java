package com.link.up.server.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PageResponse<T> {

    private final List<T> data;
    private final int page;
    private final int pageSize;
    private final int total;

    public PageResponse(
            List<T> data,
            int page,
            int pageSize,
            int total) {

        this.data =
                Collections.unmodifiableList(
                        new ArrayList<T>(data));
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
    }

    public List<T> getData() {
        return data;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotal() {
        return total;
    }
}