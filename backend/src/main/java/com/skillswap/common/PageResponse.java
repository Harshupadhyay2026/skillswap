package com.skillswap.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Transport shape for paginated endpoints — keeps Spring's Page out of the API. */
public class PageResponse<T> {

    private List<T> items;
    private int page;
    private int size;
    private long totalItems;
    private int totalPages;
    private boolean hasNext;

    public PageResponse() {}

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        PageResponse<T> res = new PageResponse<>();
        res.setItems(page.getContent().stream().map(mapper).toList());
        res.setPage(page.getNumber());
        res.setSize(page.getSize());
        res.setTotalItems(page.getTotalElements());
        res.setTotalPages(page.getTotalPages());
        res.setHasNext(page.hasNext());
        return res;
    }

    public List<T> getItems() { return items; }
    public void setItems(List<T> items) { this.items = items; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public long getTotalItems() { return totalItems; }
    public void setTotalItems(long totalItems) { this.totalItems = totalItems; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public boolean isHasNext() { return hasNext; }
    public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }
}
