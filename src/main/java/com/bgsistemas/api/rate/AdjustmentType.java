package com.bgsistemas.api.rate;

// How a rule changes the price: price = BASE x FACTOR + ADDITION
public enum AdjustmentType {
    BASE,
    FACTOR,
    ADDITION
}
