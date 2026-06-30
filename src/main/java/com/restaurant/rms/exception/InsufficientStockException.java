package com.restaurant.rms.exception;

import java.math.BigDecimal;

/**
 * Thrown when the inventory stock level is too low to fulfil an order.
 * Maps to HTTP 422 Unprocessable Entity — the request is well-formed but
 * cannot be processed given the current system state.
 *
 * <p>Usage example:</p>
 * <pre>{@code
 *   throw new InsufficientStockException("Tomato", required, available);
 *   // → "Insufficient stock for 'Tomato': required 2.500 kg but only 0.750 kg available"
 * }</pre>
 */
public class InsufficientStockException extends RuntimeException {

    private final String itemName;
    private final BigDecimal required;
    private final BigDecimal available;

    public InsufficientStockException(String itemName, BigDecimal required, BigDecimal available) {
        super(String.format(
                "Insufficient stock for '%s': required %s but only %s available",
                itemName, required.toPlainString(), available.toPlainString()));
        this.itemName  = itemName;
        this.required  = required;
        this.available = available;
    }

    public String getItemName()     { return itemName; }
    public BigDecimal getRequired() { return required; }
    public BigDecimal getAva