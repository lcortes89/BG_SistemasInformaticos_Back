package com.bgsistemas.api.rate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.math.BigDecimal;

// One answer of a question and how it changes the price (BGSI-233).
// E.g. "Restaurante" -> 49.00 BASE, "2 o 3 locales" -> 1.80 FACTOR, "Terraza" -> 10.00 ADDITION
@Entity
public class RateOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private RateQuestion question;

    // An emoji shown on the card of the wizard, e.g. "🍽️"
    @Column(length = 16)
    private String icon;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(length = 255)
    private String description;

    // "value" is a reserved word in some databases, so the column is called "amount"
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdjustmentType adjustmentType;

    @Column(nullable = false)
    private int position;

    public RateOption() {
    }

    public RateOption(String icon, String label, String description, BigDecimal amount, AdjustmentType adjustmentType) {
        this.icon = icon;
        this.label = label;
        this.description = description;
        this.amount = amount;
        this.adjustmentType = adjustmentType;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public RateQuestion getQuestion() { return question; }
    public void setQuestion(RateQuestion question) { this.question = question; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public AdjustmentType getAdjustmentType() { return adjustmentType; }
    public void setAdjustmentType(AdjustmentType adjustmentType) { this.adjustmentType = adjustmentType; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}
