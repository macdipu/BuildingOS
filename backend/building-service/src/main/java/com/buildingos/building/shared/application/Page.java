package com.buildingos.building.shared.application;

import java.util.List;

public record Page<T>(List<T> items, int page, int size, long total) {
    public static final int MAX_SIZE = 100;

    public Page {
        items = List.copyOf(items);
    }

    public static void validate(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("page must be >= 0 and size 1-" + MAX_SIZE);
        }
    }
}
