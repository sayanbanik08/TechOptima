package com.techoptima.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class Application {

    private final long applicationId;
    private final String applicationName;
    private final BigDecimal modernizationCost;
    private final int businessBenefit;
    private final Criticality criticality;
    private final Department department;
    private final List<Long> dependencyApplicationIds;

    public Application(
            long applicationId,
            String applicationName,
            BigDecimal modernizationCost,
            int businessBenefit,
            Criticality criticality,
            Department department,
            List<Long> dependencyApplicationIds) {

        if (applicationId <= 0) {
            throw new IllegalArgumentException("applicationId must be greater than 0");
        }

        if (applicationName == null || applicationName.isBlank()) {
            throw new IllegalArgumentException("applicationName cannot be null or blank");
        }

        if (modernizationCost == null) {
            throw new IllegalArgumentException("modernizationCost cannot be null");
        }

        if (modernizationCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "modernizationCost must be greater than or equal to 0");
        }

        if (businessBenefit < 0 || businessBenefit > 100) {
            throw new IllegalArgumentException(
                    "businessBenefit must be between 0 and 100");
        }

        if (criticality == null) {
            throw new IllegalArgumentException("criticality cannot be null");
        }

        if (department == null) {
            throw new IllegalArgumentException("department cannot be null");
        }

        if (dependencyApplicationIds == null) {
            throw new IllegalArgumentException(
                    "dependencyApplicationIds cannot be null");
        }

        this.applicationId = applicationId;
        this.applicationName = applicationName;
        this.modernizationCost = modernizationCost;
        this.businessBenefit = businessBenefit;
        this.criticality = criticality;
        this.department = department;
        this.dependencyApplicationIds = List.copyOf(dependencyApplicationIds);
    }

    public long getApplicationId() {
        return applicationId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public BigDecimal getModernizationCost() {
        return modernizationCost;
    }

    public int getBusinessBenefit() {
        return businessBenefit;
    }

    public Criticality getCriticality() {
        return criticality;
    }

    public Department getDepartment() {
        return department;
    }

    public List<Long> getDependencyApplicationIds() {
        return dependencyApplicationIds;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof Application)) {
            return false;
        }

        Application other = (Application) object;
        return applicationId == other.applicationId;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(applicationId);
    }

    @Override
    public String toString() {
        return "Application{" +
                "applicationId=" + applicationId +
                ", applicationName='" + applicationName + '\'' +
                ", modernizationCost=" + modernizationCost +
                ", businessBenefit=" + businessBenefit +
                ", criticality=" + criticality +
                ", department=" + department +
                ", dependencyApplicationIds=" + dependencyApplicationIds +
                '}';
    }
}