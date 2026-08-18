package com.techoptima;

import com.techoptima.model.Application;
import com.techoptima.service.OptimizationResult;
import com.techoptima.service.PortfolioOptimizationService;

public class TechOptimaApplication {

    public static void main(String[] args) {

        System.out.println("==============================================");
        System.out.println("TECHOPTIMA");
        System.out.println("Enterprise Technology Transformation System");
        System.out.println("==============================================");

        try {
            PortfolioOptimizationService service =
                    new PortfolioOptimizationService();

            OptimizationResult result =
                    service.optimize();

            System.out.println();
            System.out.println("Budget: "
                    + result.getBudget().getBudgetAmount());

            System.out.println();
            System.out.println("Knapsack Selection:");

            for (Application application :
                    result.getKnapsackResult()
                            .getSelectedApplications()) {

                System.out.println(
                        "  " +
                        application.getApplicationName() +
                        " | Cost: " +
                        application.getModernizationCost() +
                        " | Benefit: " +
                        application.getBusinessBenefit()
                );
            }

            System.out.println();
            System.out.println(
                    "Total Cost: "
                    + result.getKnapsackResult()
                            .getTotalCost()
            );

            System.out.println(
                    "Total Benefit: "
                    + result.getKnapsackResult()
                            .getTotalBusinessBenefit()
            );

            System.out.println();
            System.out.println(
                    "Dependency Check: "
                            + (
                            result.getDependencyValidationResult()
                                    .isValid()
                                    ? "PASSED"
                                    : "FAILED"
                    )
            );

            if (!result.getDependencyValidationResult()
                    .isValid()) {

                System.out.println();
                System.out.println(
                        "Missing Dependencies:"
                );

                System.out.println(
                        result.getDependencyValidationResult()
                                .getMissingDependenciesByApplication()
                );

                return;
            }

            System.out.println();
            System.out.println(
                    "Final Upgrade Order:"
            );

            for (int i = 0;
                 i < result.getTopologicalSortResult()
                         .getOrderedApplications()
                         .size();
                 i++) {

                Application application =
                        result.getTopologicalSortResult()
                                .getOrderedApplications()
                                .get(i);

                System.out.println(
                        "  " + (i + 1) + ". "
                                + application.getApplicationName()
                );
            }

            System.out.println();

            if (result.isSuccessful()) {
                System.out.println(
                        "FINAL RECOMMENDATION: READY"
                );
            } else {
                System.out.println(
                        "FINAL RECOMMENDATION: NOT AVAILABLE"
                );
            }

        } catch (Exception exception) {

            System.err.println();
            System.err.println(
                    "TechOptima execution failed:"
            );

            System.err.println(
                    exception.getMessage()
            );
        }
    }
}