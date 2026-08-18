package com.techoptima.model;

import java.math.BigDecimal;

public class TransformationBudget {
    private final BigDecimal budgetAmount;

    public TransformationBudget(BigDecimal budgetAmount) {
        if (budgetAmount == null) {
            throw new IllegalArgumentException("budgetAmount cannot be null");
        }
        if (budgetAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("budgetAmount must be greater than or equal to 0");
        }
        this.budgetAmount = budgetAmount;
    }

    public BigDecimal getBudgetAmount() {
        return budgetAmount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransformationBudget that = (TransformationBudget) o;
        return budgetAmount.equals(that.budgetAmount);
    }

    @Override
    public int hashCode() {
        return budgetAmount.hashCode();
    }

    @Override
    public String toString() {
        return "TransformationBudget{" +
                "budgetAmount=" + budgetAmount +
                '}';
    }
}