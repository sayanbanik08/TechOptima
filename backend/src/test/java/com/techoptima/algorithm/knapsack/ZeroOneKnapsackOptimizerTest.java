package com.techoptima.algorithm.knapsack;

import com.techoptima.model.Application;
import com.techoptima.model.Criticality;
import com.techoptima.model.Department;
import com.techoptima.model.TransformationBudget;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

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

    private Application applicationWithDeps(
            long id,
            String name,
            String cost,
            int benefit,
            List<Long> dependencies) {

        return new Application(
                id,
                name,
                new BigDecimal(cost),
                benefit,
                Criticality.HIGH,
                Department.OPERATIONS,
                dependencies
        );
    }

    @Test
    void shouldSolveExactOpt01ReproducerWithDependencyConstraint() {
        // OPT-01 Exact Reproducer from specification
        Application a = applicationWithDeps(1L, "A", "4.00", 100, List.of(2L));
        Application b = applicationWithDeps(2L, "B", "4.00", 1, List.of());
        Application c = applicationWithDeps(3L, "C", "4.00", 99, List.of());

        KnapsackResult result = ZeroOneKnapsackOptimizer.optimize(
                List.of(a, b, c),
                new TransformationBudget(new BigDecimal("8.00"))
        );

        // Optimal portfolio must be A + B with benefit 101, NOT B + C (benefit 100) or infeasible A + C
        assertEquals(101, result.getTotalBusinessBenefit());
        assertEquals(new BigDecimal("8.00"), result.getTotalCost());
        assertEquals(2, result.getSelectedApplications().size());
        assertTrue(result.getSelectedApplications().contains(a));
        assertTrue(result.getSelectedApplications().contains(b));
        assertFalse(result.getSelectedApplications().contains(c));
    }

    @Test
    void shouldSolveDependencyChainCorrectly() {
        // Chain: A -> B -> C (A requires B, B requires C)
        Application a = applicationWithDeps(1L, "A", "2.00", 40, List.of(2L));
        Application b = applicationWithDeps(2L, "B", "2.00", 30, List.of(3L));
        Application c = applicationWithDeps(3L, "C", "2.00", 20, List.of());
        Application d = applicationWithDeps(4L, "D", "6.00", 85, List.of());

        // Budget 6.00: {A, B, C} gives benefit 90 vs {D} gives benefit 85
        KnapsackResult result = ZeroOneKnapsackOptimizer.optimize(
                List.of(a, b, c, d),
                new TransformationBudget(new BigDecimal("6.00"))
        );

        assertEquals(90, result.getTotalBusinessBenefit());
        assertEquals(new BigDecimal("6.00"), result.getTotalCost());
        assertEquals(3, result.getSelectedApplications().size());
        assertTrue(result.getSelectedApplications().contains(a));
        assertTrue(result.getSelectedApplications().contains(b));
        assertTrue(result.getSelectedApplications().contains(c));
    }

    @Test
    void shouldHandleCyclicDependenciesInKnapsack() {
        // Cycle: A requires B, B requires A
        Application a = applicationWithDeps(1L, "A", "3.00", 50, List.of(2L));
        Application b = applicationWithDeps(2L, "B", "3.00", 50, List.of(1L));
        Application c = applicationWithDeps(3L, "C", "4.00", 60, List.of());

        // Budget 6.00: {A, B} costs 6.00 and gives 100 vs {C} costs 4.00 and gives 60
        KnapsackResult result = ZeroOneKnapsackOptimizer.optimize(
                List.of(a, b, c),
                new TransformationBudget(new BigDecimal("6.00"))
        );

        assertEquals(100, result.getTotalBusinessBenefit());
        assertEquals(new BigDecimal("6.00"), result.getTotalCost());
        assertEquals(2, result.getSelectedApplications().size());
        assertTrue(result.getSelectedApplications().contains(a));
        assertTrue(result.getSelectedApplications().contains(b));
    }

    @Test
    void shouldPruneApplicationsWithUnsatisfiableDependencies() {
        // App A requires non-existent application 999L
        Application a = applicationWithDeps(1L, "A", "5.00", 100, List.of(999L));
        Application b = applicationWithDeps(2L, "B", "10.00", 50, List.of());

        KnapsackResult result = ZeroOneKnapsackOptimizer.optimize(
                List.of(a, b),
                new TransformationBudget(new BigDecimal("20.00"))
        );

        assertEquals(50, result.getTotalBusinessBenefit());
        assertEquals(new BigDecimal("10.00"), result.getTotalCost());
        assertEquals(List.of(b), result.getSelectedApplications());
    }

    @Test
    void shouldSelectZeroCostDependency() {
        Application a = applicationWithDeps(1L, "A", "10.00", 80, List.of(2L));
        Application b = applicationWithDeps(2L, "B", "0.00", 10, List.of());

        KnapsackResult result = ZeroOneKnapsackOptimizer.optimize(
                List.of(a, b),
                new TransformationBudget(new BigDecimal("10.00"))
        );

        assertEquals(90, result.getTotalBusinessBenefit());
        assertEquals(new BigDecimal("10.00"), result.getTotalCost());
        assertEquals(2, result.getSelectedApplications().size());
        assertTrue(result.getSelectedApplications().contains(a));
        assertTrue(result.getSelectedApplications().contains(b));
    }

    @Test
    void shouldOptimizeWithOneCroreBudget() {
        Application a = application(1L, "App A", "3000000.00", 80);
        Application b = application(2L, "App B", "5000000.00", 90);
        Application c = application(3L, "App C", "4000000.00", 70);

        KnapsackResult result = ZeroOneKnapsackOptimizer.optimize(
                List.of(a, b, c),
                new TransformationBudget(new BigDecimal("10000000.00"))
        );

        assertEquals(170, result.getTotalBusinessBenefit());
        assertEquals(new BigDecimal("8000000.00"), result.getTotalCost());
        assertEquals(2, result.getSelectedApplications().size());
    }

    @Test
    void shouldOptimizeWithHundredCroreBudget() {
        Application a = application(1L, "App A", "250000000.00", 85);
        Application b = application(2L, "App B", "350000000.00", 90);
        Application c = application(3L, "App C", "500000000.00", 95);

        KnapsackResult result = ZeroOneKnapsackOptimizer.optimize(
                List.of(a, b, c),
                new TransformationBudget(new BigDecimal("1000000000.00"))
        );

        assertEquals(185, result.getTotalBusinessBenefit());
        assertEquals(new BigDecimal("850000000.00"), result.getTotalCost());
        assertEquals(2, result.getSelectedApplications().size());
    }

    @Test
    void shouldOptimizeWithTenThousandCroreBudget() {
        Application a = application(1L, "App A", "40000000000.00", 80);
        Application b = application(2L, "App B", "70000000000.00", 95);

        KnapsackResult result = ZeroOneKnapsackOptimizer.optimize(
                List.of(a, b),
                new TransformationBudget(new BigDecimal("100000000000.00"))
        );

        assertEquals(95, result.getTotalBusinessBenefit());
        assertEquals(new BigDecimal("70000000000.00"), result.getTotalCost());
        assertEquals(List.of(b), result.getSelectedApplications());
    }

    @Test
    void shouldSelectApplicationWhenCostExactlyEqualsBudgetLargeScale() {
        Application a = application(1L, "App A", "500000000.00", 75);

        KnapsackResult result = ZeroOneKnapsackOptimizer.optimize(
                List.of(a),
                new TransformationBudget(new BigDecimal("500000000.00"))
        );

        assertEquals(75, result.getTotalBusinessBenefit());
        assertEquals(new BigDecimal("500000000.00"), result.getTotalCost());
        assertEquals(List.of(a), result.getSelectedApplications());
    }

    @Test
    void shouldNotSelectApplicationJustOverBudgetLargeScale() {
        Application a = application(1L, "App A", "500000000.01", 75);

        KnapsackResult result = ZeroOneKnapsackOptimizer.optimize(
                List.of(a),
                new TransformationBudget(new BigDecimal("500000000.00"))
        );

        assertEquals(0, result.getTotalBusinessBenefit());
        assertEquals(BigDecimal.ZERO, result.getTotalCost());
        assertTrue(result.getSelectedApplications().isEmpty());
    }

    @Test
    void shouldHandleScalingRoundingBoundary() {
        Application a = application(1L, "App A", "199.00", 10);
        Application b = application(2L, "App B", "200.00", 10);
        Application c = application(3L, "App C", "201.00", 10);

        KnapsackResult result = ZeroOneKnapsackOptimizer.optimize(
                List.of(a, b, c),
                new TransformationBudget(new BigDecimal("1000000000.00"))
        );

        assertEquals(30, result.getTotalBusinessBenefit());
        assertEquals(new BigDecimal("600.00"), result.getTotalCost());
        assertEquals(3, result.getSelectedApplications().size());
    }

    @Test
    void shouldPreservePrecisionAtSmallScale() {
        Application a = application(1L, "App A", "10.01", 30);
        Application b = application(2L, "App B", "10.02", 40);
        Application c = application(3L, "App C", "10.99", 50);

        KnapsackResult result = ZeroOneKnapsackOptimizer.optimize(
                List.of(a, b, c),
                new TransformationBudget(new BigDecimal("100.00"))
        );

        assertEquals(120, result.getTotalBusinessBenefit());
        assertEquals(new BigDecimal("31.02"), result.getTotalCost());
        assertEquals(3, result.getSelectedApplications().size());
    }

    @Test
    void shouldHandleZeroCostWithLargeBudget() {
        Application a = application(1L, "App A", "0.00", 50);
        Application b = application(2L, "App B", "1000000000.00", 80);

        KnapsackResult result = ZeroOneKnapsackOptimizer.optimize(
                List.of(a, b),
                new TransformationBudget(new BigDecimal("1000000000.00"))
        );

        assertEquals(130, result.getTotalBusinessBenefit());
        assertEquals(new BigDecimal("1000000000.00"), result.getTotalCost());
        assertEquals(2, result.getSelectedApplications().size());
    }

    @Test
    void shouldHandleSingleCostExceedingBudgetLargeScale() {
        Application a = application(1L, "App A", "1000000001.00", 95);

        KnapsackResult result = ZeroOneKnapsackOptimizer.optimize(
                List.of(a),
                new TransformationBudget(new BigDecimal("1000000000.00"))
        );

        assertEquals(0, result.getTotalBusinessBenefit());
        assertEquals(BigDecimal.ZERO, result.getTotalCost());
        assertTrue(result.getSelectedApplications().isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenAllAppsExceedLargeBudget() {
        Application a = application(1L, "App A", "10000000.01", 80);
        Application b = application(2L, "App B", "20000000.00", 90);
        Application c = application(3L, "App C", "15000000.00", 70);

        KnapsackResult result = ZeroOneKnapsackOptimizer.optimize(
                List.of(a, b, c),
                new TransformationBudget(new BigDecimal("10000000.00"))
        );

        assertEquals(0, result.getTotalBusinessBenefit());
        assertEquals(BigDecimal.ZERO, result.getTotalCost());
        assertTrue(result.getSelectedApplications().isEmpty());
    }

    @Test
    void shouldSolveDependencyWithLargeScale() {
        Application a = applicationWithDeps(1L, "App A", "300000000.00", 95, List.of(2L));
        Application b = application(2L, "App B", "200000000.00", 40);
        Application c = application(3L, "App C", "600000000.00", 70);

        KnapsackResult result = ZeroOneKnapsackOptimizer.optimize(
                List.of(a, b, c),
                new TransformationBudget(new BigDecimal("1000000000.00"))
        );

        assertEquals(135, result.getTotalBusinessBenefit());
        assertEquals(new BigDecimal("500000000.00"), result.getTotalCost());
        assertEquals(2, result.getSelectedApplications().size());
        assertTrue(result.getSelectedApplications().contains(a));
        assertTrue(result.getSelectedApplications().contains(b));
    }

    @Test
    void shouldRejectDependencyExceedingBudgetLargeScale() {
        Application a = applicationWithDeps(1L, "App A", "300000000.00", 95, List.of(2L));
        Application b = application(2L, "App B", "200000000.00", 40);

        KnapsackResult result = ZeroOneKnapsackOptimizer.optimize(
                List.of(a, b),
                new TransformationBudget(new BigDecimal("400000000.00"))
        );

        assertEquals(40, result.getTotalBusinessBenefit());
        assertEquals(new BigDecimal("200000000.00"), result.getTotalCost());
        assertEquals(1, result.getSelectedApplications().size());
        assertTrue(result.getSelectedApplications().contains(b));
    }

    @Test
    void shouldHandleZeroBudgetAndAllZeroCostApplications() {
        Application a = application(1L, "App A", "0.00", 50);
        Application b = application(2L, "App B", "0.00", 60);

        KnapsackResult result = ZeroOneKnapsackOptimizer.optimize(
                List.of(a, b),
                new TransformationBudget(BigDecimal.ZERO)
        );

        assertEquals(110, result.getTotalBusinessBenefit());
        assertEquals(new BigDecimal("0.00"), result.getTotalCost());
        assertEquals(2, result.getSelectedApplications().size());
    }

    @Test
    void shouldSelectWithinTightScaledBoundary() {
        // Budget ₹100 Crore = 1,000,000,000.00
        Application a = application(1L, "App A", "400000000.00", 80);
        Application b = application(2L, "App B", "600000000.00", 90);
        Application c = application(3L, "App C", "100.00", 5);

        KnapsackResult result = ZeroOneKnapsackOptimizer.optimize(
                List.of(a, b, c),
                new TransformationBudget(new BigDecimal("1000000000.00"))
        );

        // a + b = 1,000,000,000.00 (exact budget, benefit 170)
        // a + b + c = 1,000,000,100.00 > budget
        assertEquals(170, result.getTotalBusinessBenefit());
        assertEquals(new BigDecimal("1000000000.00"), result.getTotalCost());
        assertEquals(2, result.getSelectedApplications().size());
        assertTrue(result.getSelectedApplications().contains(a));
        assertTrue(result.getSelectedApplications().contains(b));
    }

    @Test
    void shouldOptimizeEnterpriseScaleWithManyApplicationsAndDependencies() {
        // ₹100 Crore budget with 20 enterprise applications and dependency relationships
        List<Application> apps = new java.util.ArrayList<>();
        BigDecimal totalAvailableBudget = new BigDecimal("1000000000.00");

        for (int i = 1; i <= 20; i++) {
            List<Long> deps = (i % 3 == 0 && i > 3) ? List.of((long) (i - 1)) : List.of();
            BigDecimal cost = new BigDecimal((50000000 + (i * 5000000)) + ".00");
            int benefit = 50 + (i * 2);
            apps.add(new Application((long) i, "Enterprise-App-" + i, cost, benefit,
                    Criticality.HIGH, Department.OPERATIONS, deps));
        }

        KnapsackResult result = ZeroOneKnapsackOptimizer.optimize(
                apps,
                new TransformationBudget(totalAvailableBudget)
        );

        assertNotNull(result);
        assertTrue(result.getTotalCost().compareTo(totalAvailableBudget) <= 0,
                "Total cost must not exceed ₹100 Crore");
        assertTrue(result.getTotalBusinessBenefit() > 0);
        assertTrue(result.getSelectedApplications().size() > 5);

        // Verify that every selected application with a dependency has its dependency in the selection
        Set<Long> selectedIds = result.getSelectedApplications().stream()
                .map(Application::getApplicationId)
                .collect(java.util.stream.Collectors.toSet());

        for (Application app : result.getSelectedApplications()) {
            for (Long depId : app.getDependencyApplicationIds()) {
                assertTrue(selectedIds.contains(depId),
                        "Dependency " + depId + " for app " + app.getApplicationId() + " must be selected");
            }
        }
    }

    @Test
    void shouldPruneChainedMissingDependency() {
        // A -> B -> C and B -> 999 (missing)
        Application a = applicationWithDeps(1L, "App A", "10.00", 90, List.of(2L));
        Application b = applicationWithDeps(2L, "App B", "10.00", 80, List.of(3L, 999L));
        Application c = application(3L, "App C", "10.00", 70);
        Application d = application(4L, "App D", "10.00", 50);

        // Budget 20.00: A and B are pruned due to missing 999L. C and D fit within budget.
        KnapsackResult result = ZeroOneKnapsackOptimizer.optimize(
                List.of(a, b, c, d),
                new TransformationBudget(new BigDecimal("20.00"))
        );

        assertEquals(120, result.getTotalBusinessBenefit());
        assertEquals(new BigDecimal("20.00"), result.getTotalCost());
        assertEquals(2, result.getSelectedApplications().size());
        assertTrue(result.getSelectedApplications().contains(c));
        assertTrue(result.getSelectedApplications().contains(d));
    }

    @Test
    void shouldHandleCycleWithDependent() {
        // Cycle: A <-> B (A requires B, B requires A), and C requires A
        Application a = applicationWithDeps(1L, "App A", "20.00", 40, List.of(2L));
        Application b = applicationWithDeps(2L, "App B", "20.00", 40, List.of(1L));
        Application c = applicationWithDeps(3L, "App C", "20.00", 90, List.of(1L));
        Application d = application(4L, "App D", "35.00", 85);

        // If Budget is 40.00:
        // {A, B} costs 40.00, benefit 80
        // {C} is impossible because it requires {A, B} = 60.00 > 40.00
        // {D} costs 35.00, benefit 85
        // Optimal is {D} with benefit 85
        KnapsackResult result = ZeroOneKnapsackOptimizer.optimize(
                List.of(a, b, c, d),
                new TransformationBudget(new BigDecimal("40.00"))
        );

        assertEquals(85, result.getTotalBusinessBenefit());
        assertEquals(new BigDecimal("35.00"), result.getTotalCost());
        assertEquals(List.of(d), result.getSelectedApplications());

        // If Budget is 60.00:
        // {A, B, C} costs 60.00, benefit 40+40+90 = 170 > {D} (85)
        KnapsackResult result60 = ZeroOneKnapsackOptimizer.optimize(
                List.of(a, b, c, d),
                new TransformationBudget(new BigDecimal("60.00"))
        );
        assertEquals(170, result60.getTotalBusinessBenefit());
        assertEquals(new BigDecimal("60.00"), result60.getTotalCost());
        assertEquals(3, result60.getSelectedApplications().size());
        assertTrue(result60.getSelectedApplications().contains(a));
        assertTrue(result60.getSelectedApplications().contains(b));
        assertTrue(result60.getSelectedApplications().contains(c));
    }

    @Test
    void shouldHandleSharedDependencyBranchChoice() {
        // A -> B, and C -> B
        Application a = applicationWithDeps(1L, "App A", "20.00", 80, List.of(2L));
        Application b = application(2L, "App B", "20.00", 30);
        Application c = applicationWithDeps(3L, "App C", "20.00", 90, List.of(2L));

        // Budget 40.00:
        // {A, B} = cost 40.00, benefit 110
        // {C, B} = cost 40.00, benefit 120
        // {A, C} = infeasible without B
        // {A, B, C} = cost 60.00 > 40.00
        KnapsackResult result = ZeroOneKnapsackOptimizer.optimize(
                List.of(a, b, c),
                new TransformationBudget(new BigDecimal("40.00"))
        );

        assertEquals(120, result.getTotalBusinessBenefit());
        assertEquals(new BigDecimal("40.00"), result.getTotalCost());
        assertEquals(2, result.getSelectedApplications().size());
        assertTrue(result.getSelectedApplications().contains(b));
        assertTrue(result.getSelectedApplications().contains(c));
    }

    @Test
    void shouldPruneCycleWithMissingDependency() {
        // Cycle: A <-> B (A requires B, B requires A), but B also requires non-existent 999L
        Application a = applicationWithDeps(1L, "App A", "20.00", 90, List.of(2L));
        Application b = applicationWithDeps(2L, "App B", "20.00", 90, List.of(1L, 999L));
        Application c = application(3L, "App C", "20.00", 50);

        // Budget 50.00: A and B form a cycle with a missing dep (999L), so both must be pruned.
        // Only independent App C should be selected.
        KnapsackResult result = ZeroOneKnapsackOptimizer.optimize(
                List.of(a, b, c),
                new TransformationBudget(new BigDecimal("50.00"))
        );

        assertEquals(50, result.getTotalBusinessBenefit());
        assertEquals(new BigDecimal("20.00"), result.getTotalCost());
        assertEquals(List.of(c), result.getSelectedApplications());
    }
}