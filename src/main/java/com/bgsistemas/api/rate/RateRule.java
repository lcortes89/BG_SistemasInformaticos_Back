package com.bgsistemas.api.rate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

// One possible answer of the wizard and how it affects the price.
// E.g. (BUSINESS_TYPE, "RESTAURANT", 49.00, BASE) or (NUM_LOCATIONS, "TWO_THREE", 1.80, FACTOR)
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"category", "option_code"}))
public class RateRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RateCategory category;

    // Code shared with the frontend wizard (e.g. "RESTAURANT")
    @Column(name = "option_code", nullable = false)
    private String optionCode;

    // Text shown in the admin panel (e.g. "Restaurante")
    @Column(nullable = false)
    private String label;

    // "value" is a reserved word in some databases, so the column is called "amount"
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdjustmentType adjustmentType;

    public RateRule() {
    }

    public RateRule(RateCategory category, String optionCode, String label,
                    BigDecimal amount, AdjustmentType adjustmentType) {
        this.category = category;
        this.optionCode = optionCode;
        this.label = label;
        this.amount = amount;
        this.adjustmentType = adjustmentType;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public RateCategory getCategory() { return category; }
    public void setCategory(RateCategory category) { this.category = category; }
    public String getOptionCode() { return optionCode; }
    public void setOptionCode(String optionCode) { this.optionCode = optionCode; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public AdjustmentType getAdjustmentType() { return adjustmentType; }
    public void setAdjustmentType(AdjustmentType adjustmentType) { this.adjustmentType = adjustmentType; }
}
