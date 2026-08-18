package com.techoptima.algorithm.knapsack;

import com.techoptima.model.Application;
import com.techoptima.model.TransformationBudget;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Solves the enterprise application modernization problem
 * using the 0/1 Knapsack dynamic programming algorithm.
 *
 * Mapping:
 *
 * Application modernization cost -> weight
 * Application business benefit   -> value
 * Transformation budget         -> capacity
 *
 * Each application can be selected at most once.
 */
public final class ZeroOneKnapsackOptimizer {

    private static final int MONEY_SCALE = 2;
    private static final long MAX_DP_CAPACITY = 5_000_000L;

    private ZeroOneKnapsackOptimizer() {
    }

    public static KnapsackResult optimize(
            Collection<Application> applications,
            TransformationBudget budget) {

        validateInput(applications, budget);

        List<Application> applicationList =
                new ArrayList<>(applications);

        int capacity = toMinorUnits(budget.getBudgetAmount());

        if (capacity > MAX_DP_CAPACITY) {
            throw new IllegalArgumentException(
                    "Budget is too large for the current DP capacity limit"
            );
        }

        int applicationCount = applicationList.size();

        int[][] dp = new int[applicationCount + 1][capacity + 1];

        for (int i = 1; i <= applicationCount; i++) {

            Application application =
                    applicationList.get(i - 1);

            int cost =
                    toMinorUnits(application.getModernizationCost());

            int benefit =
                    application.getBusinessBenefit();

            int copyLimit = Math.min(cost, capacity + 1);
            System.arraycopy(dp[i - 1], 0, dp[i], 0, copyLimit);

            for (int currentCapacity = cost;
                 currentCapacity <= capacity;
                 currentCapacity++) {

                int excludeValue = dp[i - 1][currentCapacity];
                int includeValue =
                        dp[i - 1][currentCapacity - cost]
                                + benefit;

                dp[i][currentCapacity] = Math.max(excludeValue, includeValue);
            }
        }

        List<Application> selectedApplications =
                reconstructSelection(
                        applicationList,
                        dp,
                        capacity
                );

        BigDecimal totalCost =
                calculateTotalCost(selectedApplications);

        int totalBusinessBenefit =
                dp[applicationCount][capacity];

        return new KnapsackResult(
                selectedApplications,
                totalCost,
                totalBusinessBenefit
        );
    }

    private static List<Application> reconstructSelection(
            List<Application> applications,
            int[][] dp,
            int capacity) {

        List<Application> selected =
                new ArrayList<>();

        int currentCapacity = capacity;

        for (int i = applications.size(); i >= 1; i--) {

            if (dp[i][currentCapacity] !=
                    dp[i - 1][currentCapacity]) {

                Application application =
                        applications.get(i - 1);

                selected.add(application);

                int cost =
                        toMinorUnits(
                                application.getModernizationCost()
                        );

                currentCapacity -= cost;
            }
        }

        java.util.Collections.reverse(selected);

        return selected;
    }

    private static BigDecimal calculateTotalCost(
            List<Application> applications) {

        BigDecimal total =
                BigDecimal.ZERO;

        for (Application application : applications) {
            total = total.add(
                    application.getModernizationCost()
            );
        }

        return total;
    }

    private static int toMinorUnits(
            BigDecimal amount) {

        try {
            return amount
                    .setScale(
                            MONEY_SCALE,
                            RoundingMode.UNNECESSARY
                    )
                    .movePointRight(MONEY_SCALE)
                    .intValueExact();

        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Monetary amount must contain at most "
                            + MONEY_SCALE
                            + " decimal places and fit the DP capacity",
                    exception
            );
        }
    }

    private static void validateInput(
            Collection<Application> applications,
            TransformationBudget budget) {

        if (applications == null) {
            throw new IllegalArgumentException(
                    "applications cannot be null"
            );
        }

        if (budget == null) {
            throw new IllegalArgumentException(
                    "budget cannot be null"
            );
        }

        Set<Long> applicationIds =
                new HashSet<>();

        for (Application application : applications) {

            if (application == null) {
                throw new IllegalArgumentException(
                        "applications cannot contain null elements"
                );
            }

            if (!applicationIds.add(
                    application.getApplicationId())) {

                throw new IllegalArgumentException(
                        "Duplicate applicationId: "
                                + application.getApplicationId()
                );
            }

            toMinorUnits(
                    application.getModernizationCost()
            );
        }

        toMinorUnits(budget.getBudgetAmount());
    }
}
