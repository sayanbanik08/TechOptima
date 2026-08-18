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

            System.out.printf("Dataset N=%-4d | Budget=$%-8s | Selected=%-3d | Time=%.3f ms | Mem Delta~%d KB%n",
                    n, budget.getBudgetAmount(), result.getSelectedApplications().size(), ms, memUsedKb);

            assertNotNull(result);
            assertTrue(result.getTotalCost().compareTo(budget.getBudgetAmount()) <= 0);
            assertTrue(ms < 1000.0, "Knapsack should execute within 1000ms");
        }
    }

    @Test
    void benchmarkTopologicalSortSmallMediumLarge() {
        System.out.println("\n=== TOPOLOGICAL SORT (KAHN'S) BENCHMARK ===");

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
            assertTrue(ms < 50.0, "Topological sort should execute within 50ms even for 2000 nodes");
        }
    }

    @Test
    void benchmarkDependencyValidatorSmallMediumLarge() {
        System.out.println("\n=== DEPENDENCY VALIDATOR BENCHMARK ===");

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
            assertTrue(ms < 20.0, "Dependency validator should execute within 20ms");
        }
    }

    @Test
    void benchmarkPriorityQueueOrdering() {
        System.out.println("\n=== PRIORITY QUEUE BENCHMARK ===");

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
            assertTrue(ms < 50.0, "PriorityQueue drain should execute within 50ms for 10000 items");
        }
    }

    @Test
    void verifyLargeNumbersAndPrecision() {
        System.out.println("\n=== LARGE NUMBERS & PRECISION TEST ===");

        // Test boundary decimal values
        BigDecimal exactBudget = new BigDecimal("9999999999.99");
        TransformationBudget budget = new TransformationBudget(exactBudget);
        assertEquals(exactBudget, budget.getBudgetAmount());

        // Test exact cent arithmetic
        Application a1 = new Application(1L, "A1", new BigDecimal("1234.56"), 90, Criticality.HIGH, Department.SALES, List.of());
        Application a2 = new Application(2L, "A2", new BigDecimal("6543.21"), 80, Criticality.LOW, Department.FINANCE, List.of());

        KnapsackResult res = ZeroOneKnapsackOptimizer.optimize(List.of(a1, a2), new TransformationBudget(new BigDecimal("10000.00")));
        assertEquals(new BigDecimal("7777.77"), res.getTotalCost());
        assertEquals(170, res.getTotalBusinessBenefit());

        System.out.println("Exact arithmetic and large budget verified without precision loss or overflow.");
    }
}
