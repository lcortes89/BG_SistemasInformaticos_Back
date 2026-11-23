package com.bgsistemas.api.globals;

import java.util.ArrayList;
import java.util.List;

// BGSI-333: answer of a bulk action. processed = ids done; failed = ids not done and why
public record BulkResult(List<Long> processed, List<Failure> failed) {

    public static final String NOT_FOUND = "NOT_FOUND";

    public record Failure(Long id, String reason) {
    }

    // Every requested id that is not in "done" failed because it does not exist
    public static BulkResult of(List<Long> requested, List<Long> done) {
        List<Failure> failed = new ArrayList<>();
        for (Long id : requested) {
            if (!done.contains(id)) {
                failed.add(new Failure(id, NOT_FOUND));
            }
        }
        return new BulkResult(List.copyOf(done), List.copyOf(failed));
    }
}
