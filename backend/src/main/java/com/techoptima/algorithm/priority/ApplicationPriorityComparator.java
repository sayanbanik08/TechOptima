package com.techoptima.algorithm.priority;

import com.techoptima.model.Application;
import com.techoptima.model.Criticality;

import java.util.Comparator;

public final class ApplicationPriorityComparator
        implements Comparator<Application> {

    public static final ApplicationPriorityComparator INSTANCE =
            new ApplicationPriorityComparator();

    @Override
    public int compare(
            Application first,
            Application second) {

        if (first == second) {
            return 0;
        }

        if (first == null) {
            return 1;
        }

        if (second == null) {
            return -1;
        }

        int criticalityComparison =
                Integer.compare(
                        criticalityRank(second.getCriticality()),
                        criticalityRank(first.getCriticality())
                );

        if (criticalityComparison != 0) {
            return criticalityComparison;
        }

        int benefitComparison =
                Integer.compare(
                        second.getBusinessBenefit(),
                        first.getBusinessBenefit()
                );

        if (benefitComparison != 0) {
            return benefitComparison;
        }

        int costComparison =
                first.getModernizationCost()
                        .compareTo(
                                second.getModernizationCost()
                        );

        if (costComparison != 0) {
            return costComparison;
        }

        return Long.compare(
                first.getApplicationId(),
                second.getApplicationId()
        );
    }

    private int criticalityRank(
            Criticality criticality) {

        if (criticality == null) {
            return -1;
        }

        switch (criticality) {
            case CRITICAL:
                return 4;
            case HIGH:
                return 3;
            case MEDIUM:
                return 2;
            case LOW:
                return 1;
            default:
                return -1;
        }
    }
}