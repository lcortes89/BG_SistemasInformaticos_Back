package com.bgsistemas.api.globals;

import java.util.ArrayList;
import java.util.List;

// BGSI-333: answer of a bulk action. processed = ids done; failed = ids not done and why
public record BulkResult(List<Long> processed, List<Failure> failed) {

    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String HAS_PRODUCTS = "HAS_PRODUCTS"; // a brand with products cannot be deleted
    public static final String OWN_ACCOUNT = "OWN_ACCOUNT"; // an admin cannot act on their own account
    public static final String WRONG_STATUS = "WRONG_STATUS"; // e.g. blocking an admin who is not active

    public record Failure(Long id, String reason) {
    }

    // Every requested id that is not in "done" failed because it does not exist
    public static BulkResult of(List<Long> requested, List<Long> done) {
        return of(requested, done, List.of());
    }

    // "refused": ids that exist but a rule did not allow (with their reason). The rest not done = NOT_FOUND
    public static BulkResult of(List<Long> requested, List<Long> done, List<Failure> refused) {
        List<Failure> failed = new ArrayList<>(refused);
        List<Long> refusedIds = refused.stream().map(Failure::id).toList();
        for (Long id : requested) {
            if (!done.contains(id) && !refusedIds.contains(id)) {
                failed.add(new Failure(id, NOT_FOUND));
            }
        }
        return new BulkResult(List.copyOf(done), List.copyOf(failed));
    }
}
