package com.techoptima.benchmark;

import com.techoptima.algorithm.knapsack.KnapsackResult;
import com.techoptima.algorithm.knapsack.ZeroOneKnapsackOptimizer;
import com.techoptima.algorithm.priority.ApplicationPriorityQueue;
import com.techoptima.algorithm.topology.TopologicalSortResult;
import com.techoptima.algorithm.topology.TopologicalSorter;
import com.techoptima.model.Application;
import com.techoptima.model.Criticality;
import com.techoptima.model.Department;
import com.techoptima.model.TransformationBudget;
import com.techoptima.validation.DependencyValidationResult;
import com.techoptima.validation.DependencyValidator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class PerformanceBenchmarkTest {

    private List<Application> generateDataset(int count, int maxDeps) {
        List<Application> list = new ArrayList<>(count);
        Criticality[] criticalities = Criticality.values();
        Department[] departments = Department.values();
        Random random = new Random(42); // Deterministic seed

        for (int i = 1; i <= count; i++) {
            List<Long> deps = new ArrayList<>();
            if (maxDeps > 0 && i > 1) {
                int numDeps = random.nextInt(Math.min(maxDeps + 1, i));
                for (int d = 0; d < numDeps; d++) {
                    long depId = 1 + random.nextInt(i - 1); // Strictly smaller ID -> DAG (acyclic)
                    if (!deps.contains(depId)) {
                        deps.add(depId);
                    }
                }
            }

            BigDecimal cost = BigDecimal.valueOf(10 + random.nextInt(90)).setScale(2);
            int benefit = 10 + random.nextInt(90);
            Criticality criticality = criticalities[random.nextInt(criticalities.length)];
            Department department = departments[random.nextInt(departments.length)];

            list.add(new Application((long) i, "App-" + i, cost, benefit, criticality, department, deps));
        }

        return list;
    }

    @Test
    void benchmarkKnapsackSmallMediumLarge() {
        System.out.println("\n=== 0/1 KNAPSACK PERFORMANCE BENCHMARK ===");
        
        int[] datasetSizes = {10, 50, 100};
        BigDecimal[] budgets = {
            new BigDecimal("200.00"),
            new BigDecimal("1000.00"),
            new BigDecimal("2000.00")
        };

        for (int k = 0; k < datasetSizes.length; k++) {
            int n = datasetSizes[k];
            TransformationBudget budget = new TransformationBudget(budgets[k]);
            List<Application> apps = generateDataset(n, 0);

            // Warmup
            ZeroOneKnapsackOptimizer.optimize(apps, budget);

            long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long start = System.nanoTime();
            KnapsackResult result = ZeroOneKnapsackOptimizer.optimize(apps, budget);
            long elapsed = System.nanoTime() - start;
            long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

            double ms = elapsed / 1_000_000.0;
            long memUsedKb = Math.max(0, (memoryAfter - memoryBefore) / 1024);

            System.out.printf("Dataset N=%-4d | Budget=₹%-8s | Selected=%-3d | Time=%.3f ms | Mem Delta~%d KB%n",
                    n, budget.getBudgetAmount(), result.getSelectedApplications().size(), ms, memUsedKb);

            assertNotNull(result);
            assertTrue(result.getTotalCost().compareTo(budget.getBudgetAmount()) <= 0);
        }
    }

    @Test
    void benchmarkTopologicalSortSmallMediumLarge() {
        System.out.println("\n=== TOPOLOGICAL SORT (KAHN'S) REGRESSION TEST ===");

        int[] datasetSizes = {10, 100, 500, 1000, 2000};

        for (int n : datasetSizes) {
            List<Application> apps = generateDataset(n, 4);

            // Warmup
            TopologicalSorter.sort(apps);

            long start = System.nanoTime();
            TopologicalSortResult result = TopologicalSorter.sort(apps);
            long elapsed = System.nanoTime() - start;

            double ms = elapsed / 1_000_000.0;

            System.out.printf("Dataset N=%-5d | Valid DAG=%-5s | Ordered=%-5d | Time=%.3f ms%n",
                    n, result.isValid(), result.getOrderedApplications().size(), ms);

            assertTrue(result.isValid());
            assertEquals(n, result.getOrderedApplications().size());
        }
    }

    @Test
    void benchmarkDependencyValidatorSmallMediumLarge() {
        System.out.println("\n=== DEPENDENCY VALIDATOR REGRESSION TEST ===");

        int[] datasetSizes = {10, 100, 1000, 5000};

        for (int n : datasetSizes) {
            List<Application> apps = generateDataset(n, 3);

            // Warmup
            DependencyValidator.validate(apps);

            long start = System.nanoTime();
            DependencyValidationResult result = DependencyValidator.validate(apps);
            long elapsed = System.nanoTime() - start;

            double ms = elapsed / 1_000_000.0;

            System.out.printf("Dataset N=%-5d | Valid=%-5s | Time=%.3f ms%n",
                    n, result.isValid(), ms);

            assertTrue(result.isValid());
        }
    }

    @Test
    void benchmarkPriorityQueueOrdering() {
        System.out.println("\n=== PRIORITY QUEUE REGRESSION TEST ===");

        int[] datasetSizes = {100, 1000, 5000, 10000};

        for (int n : datasetSizes) {
            List<Application> apps = generateDataset(n, 0);

            long start = System.nanoTime();
            ApplicationPriorityQueue queue = ApplicationPriorityQueue.from(apps);
            List<Application> ordered = queue.drainInPriorityOrder();
            long elapsed = System.nanoTime() - start;

            double ms = elapsed / 1_000_000.0;

            System.out.printf("Dataset N=%-6d | Drained=%-6d | Time=%.3f ms%n",
                    n, ordered.size(), ms);

            assertEquals(n, ordered.size());
        }
    }

    @Test
    void verifyLargeNumbersAndPrecision() {
        System.out.println("\n=== ENTERPRISE SCALE & PRECISION REGRESSION TEST ===");

        // Test ₹100 Crore enterprise budget
        BigDecimal enterpriseBudget = new BigDecimal("1000000000.00");
        TransformationBudget budget = new TransformationBudget(enterpriseBudget);
        assertEquals(enterpriseBudget, budget.getBudgetAmount());

        // Enterprise-scale applications
        Application a1 = new Application(1L, "ERP-Core",
                new BigDecimal("250000000.00"), 85,
                Criticality.HIGH, Department.SALES, List.of());
        Application a2 = new Application(2L, "CRM-Suite",
                new BigDecimal("350000000.00"), 90,
                Criticality.HIGH, Department.FINANCE, List.of());
        Application a3 = new Application(3L, "Analytics-Platform",
                new BigDecimal("500000000.00"), 95,
                Criticality.CRITICAL, Department.INFORMATION_TECHNOLOGY, List.of());

        long start = System.nanoTime();
        KnapsackResult res = ZeroOneKnapsackOptimizer.optimize(
                List.of(a1, a2, a3), budget
        );
        long elapsed = System.nanoTime() - start;
        double ms = elapsed / 1_000_000.0;

        assertTrue(res.getTotalCost().compareTo(budget.getBudgetAmount()) <= 0,
                "Total cost must not exceed budget");
        assertEquals(185, res.getTotalBusinessBenefit());
        assertEquals(new BigDecimal("850000000.00"), res.getTotalCost());

        System.out.printf("Enterprise ₹100Cr budget: Selected=%d | Benefit=%d | Cost=₹%s | Time=%.3f ms%n",
                res.getSelectedApplications().size(), res.getTotalBusinessBenefit(),
                res.getTotalCost(), ms);

        // Test exact decimal precision still works at small scale
        Application s1 = new Application(4L, "SmallApp",
                new BigDecimal("1234.56"), 90,
                Criticality.HIGH, Department.SALES, List.of());
        Application s2 = new Application(5L, "SmallApp2",
                new BigDecimal("6543.21"), 80,
                Criticality.LOW, Department.FINANCE, List.of());
        KnapsackResult smallRes = ZeroOneKnapsackOptimizer.optimize(
                List.of(s1, s2),
                new TransformationBudget(new BigDecimal("10000.00"))
        );
        assertEquals(new BigDecimal("7777.77"), smallRes.getTotalCost());
        assertEquals(170, smallRes.getTotalBusinessBenefit());

        System.out.println("Enterprise-scale and decimal precision verified.");
    }

    @Test
    void benchmarkDependencyKnapsackWithDAG() {
        System.out.println("\n=== DEPENDENCY-CONSTRAINED KNAPSACK REGRESSION TEST ===");

        int[] datasetSizes = {10, 25, 50};
        BigDecimal[] budgets = {
            new BigDecimal("200.00"),
            new BigDecimal("500.00"),
            new BigDecimal("1000.00")
        };

        for (int k = 0; k < datasetSizes.length; k++) {
            int n = datasetSizes[k];
            TransformationBudget budget = new TransformationBudget(budgets[k]);
            List<Application> apps = generateDataset(n, 2);

            // Warmup
            ZeroOneKnapsackOptimizer.optimize(apps, budget);

            long start = System.nanoTime();
            KnapsackResult result = ZeroOneKnapsackOptimizer.optimize(apps, budget);
            long elapsed = System.nanoTime() - start;

            double ms = elapsed / 1_000_000.0;

            System.out.printf("DAG Dataset N=%-4d | Budget=₹%-8s | Selected=%-3d | Benefit=%-4d | Time=%.3f ms%n",
                    n, budget.getBudgetAmount(), result.getSelectedApplications().size(),
                    result.getTotalBusinessBenefit(), ms);

            assertNotNull(result);
            assertTrue(result.getTotalCost().compareTo(budget.getBudgetAmount()) <= 0);

            // Verify dependency validity on the selected portfolio
            DependencyValidationResult validation = DependencyValidator.validate(result.getSelectedApplications());
            assertTrue(validation.isValid(), "Selected portfolio must strictly satisfy all dependency constraints");
        }
    }

    @Test
    void benchmarkCyclicAndDenseDependencies() {
        System.out.println("\n=== CYCLIC & DENSE DEPENDENCY REGRESSION TEST ===");

        // Create cyclic cluster A <-> B <-> C and dense DAG dependents
        List<Application> apps = new ArrayList<>();
        apps.add(new Application(1L, "App-1", new BigDecimal("50.00"), 60, Criticality.HIGH, Department.SALES, List.of(2L)));
        apps.add(new Application(2L, "App-2", new BigDecimal("50.00"), 70, Criticality.HIGH, Department.FINANCE, List.of(3L)));
        apps.add(new Application(3L, "App-3", new BigDecimal("50.00"), 80, Criticality.CRITICAL, Department.OPERATIONS, List.of(1L))); // Cycle 1-2-3
        apps.add(new Application(4L, "App-4", new BigDecimal("30.00"), 40, Criticality.MEDIUM, Department.INFORMATION_TECHNOLOGY, List.of(1L))); // Depends on cycle
        apps.add(new Application(5L, "App-5", new BigDecimal("40.00"), 90, Criticality.LOW, Department.OPERATIONS, List.of())); // Independent

        TransformationBudget budget = new TransformationBudget(new BigDecimal("200.00"));

        long start = System.nanoTime();
        KnapsackResult result = ZeroOneKnapsackOptimizer.optimize(apps, budget);
        long elapsed = System.nanoTime() - start;

        double ms = elapsed / 1_000_000.0;

        System.out.printf("Cyclic SCC Dataset N=5 | Budget=₹%-8s | Selected=%-3d | Benefit=%-4d | Time=%.3f ms%n",
                budget.getBudgetAmount(), result.getSelectedApplications().size(),
                result.getTotalBusinessBenefit(), ms);

        assertNotNull(result);
        assertTrue(result.getTotalCost().compareTo(budget.getBudgetAmount()) <= 0);

        // Optimal: Cycle {1, 2, 3} costs 150 (benefit 210) + App 5 costs 40 (benefit 90) = Total 190, Benefit 300
        assertEquals(300, result.getTotalBusinessBenefit());
        assertEquals(new BigDecimal("190.00"), result.getTotalCost());
        assertEquals(4, result.getSelectedApplications().size());
    }
}
