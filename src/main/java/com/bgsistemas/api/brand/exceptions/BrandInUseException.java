package com.bgsistemas.api.brand.exceptions;

import com.bgsistemas.api.globals.ResourceConflictException;

public class BrandInUseException extends ResourceConflictException {

    public BrandInUseException(String brandName, long serviceCount) {
        super("Brand " + brandName + " has " + serviceCount
                + " service(s). Delete them or move them to another brand first");
    }
}
