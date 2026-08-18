package com.techoptima.algorithm.knapsack;

import com.techoptima.model.Application;
import com.techoptima.model.Criticality;
import com.techoptima.model.Department;
import com.techoptima.model.TransformationBudget;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ZeroOneKnapsackOptimizerTest {

    private Application application(
            long id,
            String name,
            String cost,
            int benefit) {

        return new Application(
                id,
                name,
                new BigDecimal(cost),
                benefit,
                Criticality.HIGH,
                Department.OPERATIONS,
                List.of()
        );
    }

    @Test
    void shouldSelectOptimalCombinationWithinBudget() {

        Application a =
                application(1, "CRM", "20.00", 80);

        Application b =
                application(2, "ERP", "40.00", 100);

        Application c =
                application(3, "Analytics", "25.00", 90);

        Application d =
                application(4, "Billing", "18.00", 60);

        KnapsackResult result =
                ZeroOneKnapsackOptimizer.optimize(
                        List.of(a, b, c, d),
                        new TransformationBudget(
                                new BigDecimal("60.00")
                        )
                );

        assertEquals(180,
                result.getTotalBusinessBenefit());

        assertEquals(
                new BigDecimal("60.00"),
                result.getTotalCost()
        );

        assertEquals(
                List.of(a, b),
                result.getSelectedApplications()
        );
    }

    @Test
    void shouldNeverExceedBudget() {

        List<Application> applications = List.of(
                application(1, "CRM", "30.00", 70),
                application(2, "ERP", "40.00", 90),
                application(3, "Analytics", "20.00", 50)
        );

        TransformationBudget budget =
                new TransformationBudget(
                        new BigDecimal("50.00")
                );

        KnapsackResult result =
                ZeroOneKnapsackOptimizer.optimize(
                        applications,
                        budget
                );

        assertTrue(
                result.getTotalCost()
                        .compareTo(budget.getBudgetAmount()) <= 0
        );
    }

    @Test
    void shouldNotSelectApplicationMoreThanOnce() {

        Application application =
                application(1, "CRM", "20.00", 80);

        KnapsackResult result =
                ZeroOneKnapsackOptimizer.optimize(
                        List.of(application),
                        new TransformationBudget(
                                new BigDecimal("100.00")
                        )
                );

        assertEquals(1,
                result.getSelectedApplications().size());

        assertEquals(
                80,
                result.getTotalBusinessBenefit()
        );
    }

    @Test
    void shouldReturnEmptySelectionForZeroBudget() {

        List<Application> applications = List.of(
                application(1, "CRM", "20.00", 80),
                application(2, "ERP", "30.00", 100)
        );

        KnapsackResult result =
                ZeroOneKnapsackOptimizer.optimize(
                        applications,
                        new TransformationBudget(
                                BigDecimal.ZERO
                        )
                );

        assertTrue(
                result.getSelectedApplications().isEmpty()
        );

        assertEquals(
                BigDecimal.ZERO,
                result.getTotalCost()
        );

        assertEquals(
                0,
                result.getTotalBusinessBenefit()
        );
    }

    @Test
    void shouldHandleEmptyApplicationList() {

        KnapsackResult result =
                ZeroOneKnapsackOptimizer.optimize(
                        List.of(),
                        new TransformationBudget(
                                new BigDecimal("100.00")
                        )
                );

        assertTrue(
                result.getSelectedApplications().isEmpty()
        );

        assertEquals(
                BigDecimal.ZERO,
                result.getTotalCost()
        );

        assertEquals(
                0,
                result.getTotalBusinessBenefit()
        );
    }

    @Test
    void shouldRejectNullApplicationCollection() {

        assertThrows(
                IllegalArgumentException.class,
                () -> ZeroOneKnapsackOptimizer.optimize(
                        null,
                        new TransformationBudget(
                                new BigDecimal("100.00")
                        )
                )
        );
    }

    @Test
    void shouldRejectNullBudget() {

        assertThrows(
                IllegalArgumentException.class,
                () -> ZeroOneKnapsackOptimizer.optimize(
                        List.of(
                                application(
                                        1,
                                        "CRM",
                                        "20.00",
                                        80
                                )
                        ),
                        null
                )
        );
    }

    @Test
    void shouldRejectDuplicateApplicationIds() {

        Application first =
                application(1, "CRM", "20.00", 80);

        Application second =
                application(1, "ERP", "30.00", 100);

        assertThrows(
                IllegalArgumentException.class,
                () -> ZeroOneKnapsackOptimizer.optimize(
                        List.of(first, second),
                        new TransformationBudget(
                                new BigDecimal("100.00")
                        )
                )
        );
    }

    @Test
    void shouldHandleDecimalCostsExactly() {

        Application a =
                application(1, "CRM", "10.25", 50);

        Application b =
                application(2, "ERP", "10.75", 60);

        KnapsackResult result =
                ZeroOneKnapsackOptimizer.optimize(
                        List.of(a, b),
                        new TransformationBudget(
                                new BigDecimal("21.00")
                        )
                );

        assertEquals(
                new BigDecimal("21.00"),
                result.getTotalCost()
        );

        assertEquals(
                110,
                result.getTotalBusinessBenefit()
        );
    }

    @Test
    void shouldReturnEmptySelectionWhenBudgetIsSmallerThanEveryItem() {

        List<Application> applications = List.of(
                application(1, "CRM", "20.00", 80),
                application(2, "ERP", "30.00", 100),
                application(3, "Analytics", "25.00", 90)
        );

        KnapsackResult result =
                ZeroOneKnapsackOptimizer.optimize(
                        applications,
                        new TransformationBudget(
                                new BigDecimal("10.00")
                        )
                );

        assertTrue(
                result.getSelectedApplications().isEmpty()
        );

        assertEquals(
                BigDecimal.ZERO,
                result.getTotalCost()
        );

        assertEquals(
                0,
                result.getTotalBusinessBenefit()
        );
    }

    @Test
    void shouldSelectAllApplicationsWhenBudgetExceedsTotalCost() {

        Application a =
                application(1, "CRM", "20.00", 80);

        Application b =
                application(2, "ERP", "30.00", 100);

        Application c =
                application(3, "Analytics", "25.00", 90);

        KnapsackResult result =
                ZeroOneKnapsackOptimizer.optimize(
                        List.of(a, b, c),
                        new TransformationBudget(
                                new BigDecimal("500.00")
                        )
                );

        assertEquals(3,
                result.getSelectedApplications().size());

        assertEquals(
                new BigDecimal("75.00"),
                result.getTotalCost()
        );

        assertEquals(
                270,
                result.getTotalBusinessBenefit()
        );
    }

    @Test
    void shouldSelectItemWhenCostExactlyEqualsBudget() {

        Application single =
                application(1, "CRM", "50.00", 80);

        KnapsackResult result =
                ZeroOneKnapsackOptimizer.optimize(
                        List.of(single),
                        new TransformationBudget(
                                new BigDecimal("50.00")
                        )
                );

        assertEquals(1,
                result.getSelectedApplications().size());

        assertEquals(
                new BigDecimal("50.00"),
                result.getTotalCost()
        );

        assertEquals(
                80,
                result.getTotalBusinessBenefit()
        );
    }

    @Test
    void shouldSelectZeroCostApplication() {

        Application zeroCost =
                application(1, "Free Tool", "0.00", 50);

        Application normal =
                application(2, "CRM", "20.00", 80);

        KnapsackResult result =
                ZeroOneKnapsackOptimizer.optimize(
                        List.of(zeroCost, normal),
                        new TransformationBudget(
                                new BigDecimal("20.00")
                        )
                );

        assertEquals(2,
                result.getSelectedApplications().size());

        assertEquals(
                new BigDecimal("20.00"),
                result.getTotalCost()
        );

        assertEquals(
                130,
                result.getTotalBusinessBenefit()
        );
    }
}