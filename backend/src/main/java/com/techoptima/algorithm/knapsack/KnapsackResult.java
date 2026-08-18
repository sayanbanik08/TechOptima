package com.techoptima.algorithm.knapsack;

import com.techoptima.model.Application;

import java.math.BigDecimal;
import java.util.List;

public final class KnapsackResult {

    private final List<Application> selectedApplications;
    private final BigDecimal totalCost;
    private final int totalBusinessBenefit;

    public KnapsackResult(
            List<Application> selectedApplications,
            BigDecimal totalCost,
            int totalBusinessBenefit) {

        if (selectedApplications == null) {
            throw new IllegalArgumentException(
                    "selectedApplications cannot be null"
            );
        }

        if (totalCost == null) {
            throw new IllegalArgumentException(
                    "totalCost cannot be null"
            );
        }

        if (totalCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "totalCost cannot be negative"
            );
        }

        if (totalBusinessBenefit < 0) {
            throw new IllegalArgumentException(
                    "totalBusinessBenefit cannot be negative"
            );
        }

        this.selectedApplications =
                List.copyOf(selectedApplications);
        this.totalCost = totalCost;
        this.totalBusinessBenefit = totalBusinessBenefit;
    }

    public List<Application> getSelectedApplications() {
        return selectedApplications;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public int getTotalBusinessBenefit() {
        return totalBusinessBenefit;
    }

    @Override
    public String toString() {
        return "KnapsackResult{" +
                "selectedApplications=" + selectedApplications +
                ", totalCost=" + totalCost +
                ", totalBusinessBenefit=" + totalBusinessBenefit +
                '}';
    }
}