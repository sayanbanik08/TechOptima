package com.techoptima.algorithm.knapsack;

import com.techoptima.model.Application;
import com.techoptima.model.TransformationBudget;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Solves the enterprise application modernization problem
 * using globally optimal 0/1 Knapsack optimization with
 * transitive dependency closure constraints.
 *
 * For independent applications, uses dynamic programming.
 * For precedence-constrained applications, uses exact
 * Branch-and-Bound with dependency propagation and
 * fractional upper-bound pruning.
 *
 * Mapping:
 * Application modernization cost -> weight
 * Application business benefit   -> value
 * Transformation budget         -> capacity
 * Dependency closure            -> precedence constraints
 *
 * Each application can be selected at most once.
 *
 * Monetary values are dynamically scaled so that the DP
 * capacity never exceeds MAX_DP_CAPACITY, supporting
 * enterprise-scale budgets (₹100 Crore and beyond).
 */
public final class ZeroOneKnapsackOptimizer {

    private static final long MAX_DP_CAPACITY = 5_000_000L;

    private ZeroOneKnapsackOptimizer() {
    }

    public static KnapsackResult optimize(
            Collection<Application> applications,
            TransformationBudget budget) {

        validateInput(applications, budget);

        List<Application> applicationList =
                new ArrayList<>(applications);

        if (applicationList.isEmpty()) {
            return new KnapsackResult(
                    List.of(),
                    BigDecimal.ZERO,
                    0
            );
        }

        MonetaryScale scale = MonetaryScale.create(
                budget.getBudgetAmount(),
                applicationList
        );

        int capacity = scale.toCapacityUnits(
                budget.getBudgetAmount()
        );

        if (capacity < 0) {
            return new KnapsackResult(
                    List.of(),
                    BigDecimal.ZERO,
                    0
            );
        }

        boolean hasDependencies = false;
        for (Application application : applicationList) {
            if (!application.getDependencyApplicationIds().isEmpty()) {
                hasDependencies = true;
                break;
            }
        }

        KnapsackResult candidateResult;

        if (!hasDependencies) {
            candidateResult = optimizeStandardDp(
                    applicationList, capacity, scale
            );
        } else {
            candidateResult = optimizeWithDependencies(
                    applicationList, capacity, scale
            );
        }

        /*
         * Final safety net: validate the selected portfolio
         * against the original BigDecimal budget to guard
         * against any unforeseen discrepancy.
         */
        BigDecimal actualTotalCost =
                calculateTotalCost(
                        candidateResult.getSelectedApplications()
                );

        if (actualTotalCost.compareTo(
                budget.getBudgetAmount()) > 0) {
            throw new IllegalStateException(
                    "Scaled optimization result exceeded exact transformation budget: "
                            + actualTotalCost + " > " + budget.getBudgetAmount()
            );
        }

        /*
         * Final safety net: validate dependency closure on the selected portfolio.
         * Guarantees no application is returned without all its dependencies present.
         */
        Set<Long> selectedIds = new HashSet<>();
        for (Application app : candidateResult.getSelectedApplications()) {
            selectedIds.add(app.getApplicationId());
        }

        for (Application app : candidateResult.getSelectedApplications()) {
            for (Long depId : app.getDependencyApplicationIds()) {
                if (!selectedIds.contains(depId)) {
                    throw new IllegalStateException(
                            "Selected portfolio is missing dependency: "
                                    + depId + " required by application " + app.getApplicationId()
                    );
                }
            }
        }

        return candidateResult;
    }

    /**
     * Classic 0/1 Knapsack DP algorithm for independent applications (0 dependencies).
     * Time: O(N * W), Space: O(N * W)
     */
    private static KnapsackResult optimizeStandardDp(
            List<Application> applicationList,
            int capacity,
            MonetaryScale scale) {

        int applicationCount = applicationList.size();

        int[][] dp = new int[applicationCount + 1][capacity + 1];

        for (int i = 1; i <= applicationCount; i++) {

            Application application =
                    applicationList.get(i - 1);

            int cost = scale.toCostUnits(
                    application.getModernizationCost()
            );

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
                        capacity,
                        scale
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

    /**
     * Solves the Precedence-Constrained 0/1 Knapsack Problem (PCKP) globally optimally.
     * Evaluates dependency closure directly during selection, guaranteeing global optimality.
     */
    private static KnapsackResult optimizeWithDependencies(
            List<Application> applicationList,
            int capacity,
            MonetaryScale scale) {

        Map<Long, Application> applicationMap = new LinkedHashMap<>();
        for (Application app : applicationList) {
            applicationMap.put(app.getApplicationId(), app);
        }

        // 1. Identify applications whose dependency closures are satisfiable within budget
        Map<Long, Set<Long>> fullClosures = new HashMap<>();
        Map<Long, Long> closureCosts = new HashMap<>();
        Set<Long> satisfiableAppIds = new HashSet<>();

        for (Application app : applicationList) {
            long id = app.getApplicationId();
            Set<Long> closure = new HashSet<>();
            boolean valid = computeClosure(id, applicationMap, closure, new HashSet<>());
            if (valid) {
                long cost = 0L;
                for (Long memberId : closure) {
                    cost += scale.toCostUnits(
                            applicationMap.get(memberId).getModernizationCost()
                    );
                }
                if (cost <= capacity) {
                    fullClosures.put(id, closure);
                    closureCosts.put(id, cost);
                    satisfiableAppIds.add(id);
                }
            }
        }

        if (satisfiableAppIds.isEmpty()) {
            return new KnapsackResult(
                    List.of(),
                    BigDecimal.ZERO,
                    0
            );
        }

        // Filter applications to only satisfiable candidates
        List<Application> candidates = new ArrayList<>();
        for (Application app : applicationList) {
            if (satisfiableAppIds.contains(app.getApplicationId())) {
                candidates.add(app);
            }
        }

        // 2. Find Strongly Connected Components (SCCs) to handle cyclic dependencies
        List<List<Application>> sccs = findSCCs(candidates);

        // 3. Contract SCCs into compound components
        int numComponents = sccs.size();
        List<Component> components = new ArrayList<>(numComponents);
        Map<Long, Integer> appToComponentIndex = new HashMap<>();

        for (int i = 0; i < numComponents; i++) {
            List<Application> sccApps = sccs.get(i);
            long compCost = 0L;
            int compBenefit = 0;
            for (Application app : sccApps) {
                compCost += scale.toCostUnits(
                        app.getModernizationCost()
                );
                compBenefit += app.getBusinessBenefit();
                appToComponentIndex.put(app.getApplicationId(), i);
            }

            int boundedCost = compCost > capacity
                    ? (capacity + 1)
                    : (int) compCost;

            components.add(new Component(
                    i, sccApps, boundedCost, compBenefit
            ));
        }

        // Build component direct dependency graph
        Map<Integer, Set<Integer>> compDependencies = new HashMap<>();
        for (int i = 0; i < numComponents; i++) {
            compDependencies.put(i, new LinkedHashSet<>());
        }

        for (int i = 0; i < numComponents; i++) {
            Component comp = components.get(i);
            for (Application app : comp.apps) {
                for (Long depAppId : app.getDependencyApplicationIds()) {
                    Integer depCompIdx = appToComponentIndex.get(depAppId);
                    if (depCompIdx != null && depCompIdx != i) {
                        compDependencies.get(i).add(depCompIdx);
                    }
                }
            }
        }

        // 4. Compute topological ordering of components (dependencies precede dependents)
        List<Integer> topoOrder = topologicalSortComponents(numComponents, compDependencies);

        // Re-index components according to topological ordering
        int m = topoOrder.size();
        Component[] orderedComponents = new Component[m];
        int[] oldToNewIndex = new int[numComponents];

        for (int newIdx = 0; newIdx < m; newIdx++) {
            int oldIdx = topoOrder.get(newIdx);
            Component oldComp = components.get(oldIdx);
            orderedComponents[newIdx] = new Component(newIdx, oldComp.apps, oldComp.cost, oldComp.benefit);
            oldToNewIndex[oldIdx] = newIdx;
        }

        // Map direct dependencies to new topological indices (all dependencies have index < newIdx)
        int[][] directDeps = new int[m][];
        BitSet[] transitiveDeps = new BitSet[m];

        for (int newIdx = 0; newIdx < m; newIdx++) {
            int oldIdx = topoOrder.get(newIdx);
            Set<Integer> oldDeps = compDependencies.get(oldIdx);
            directDeps[newIdx] = new int[oldDeps.size()];
            int ptr = 0;
            for (int od : oldDeps) {
                directDeps[newIdx][ptr++] = oldToNewIndex[od];
            }

            BitSet trans = new BitSet(m);
            for (int d : directDeps[newIdx]) {
                trans.set(d);
                trans.or(transitiveDeps[d]);
            }
            transitiveDeps[newIdx] = trans;
        }

        // 5. Exact Branch & Bound with Dependency Propagation & Fractional Knapsack Upper Bounding
        BranchAndBoundSolver solver = new BranchAndBoundSolver(
                orderedComponents,
                directDeps,
                transitiveDeps,
                capacity
        );
        solver.solve();

        List<Application> selectedApplications = solver.getSelectedApplications();
        BigDecimal totalCost = calculateTotalCost(selectedApplications);
        int totalBusinessBenefit = solver.getBestBenefit();

        return new KnapsackResult(
                selectedApplications,
                totalCost,
                totalBusinessBenefit
        );
    }

    private static boolean computeClosure(
            long currentId,
            Map<Long, Application> applicationMap,
            Set<Long> closure,
            Set<Long> visiting) {

        if (closure.contains(currentId)) {
            return true;
        }
        if (visiting.contains(currentId)) {
            // Cycle detected in dependencies, valid if all are present
            return true;
        }
        Application app = applicationMap.get(currentId);
        if (app == null) {
            return false;
        }

        visiting.add(currentId);
        closure.add(currentId);

        for (Long depId : app.getDependencyApplicationIds()) {
            if (!computeClosure(depId, applicationMap, closure, visiting)) {
                return false;
            }
        }

        visiting.remove(currentId);
        return true;
    }

    private static List<List<Application>> findSCCs(List<Application> candidates) {
        int n = candidates.size();
        Map<Long, Integer> idToIndex = new HashMap<>();
        for (int i = 0; i < n; i++) {
            idToIndex.put(candidates.get(i).getApplicationId(), i);
        }

        List<List<Integer>> adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            adj.add(new ArrayList<>());
            for (Long depId : candidates.get(i).getDependencyApplicationIds()) {
                Integer depIdx = idToIndex.get(depId);
                if (depIdx != null) {
                    adj.get(i).add(depIdx);
                }
            }
        }

        TarjanSCC tarjan = new TarjanSCC(n, adj);
        List<List<Integer>> sccIndices = tarjan.getSCCs();

        List<List<Application>> result = new ArrayList<>();
        for (List<Integer> comp : sccIndices) {
            List<Application> compApps = new ArrayList<>();
            for (int idx : comp) {
                compApps.add(candidates.get(idx));
            }
            result.add(compApps);
        }
        return result;
    }

    private static List<Integer> topologicalSortComponents(
            int numComponents,
            Map<Integer, Set<Integer>> compDependencies) {

        // Edge direction: dependency -> dependent (Kahn's topological sort)
        int[] indegree = new int[numComponents];
        Map<Integer, List<Integer>> outgoing = new HashMap<>();
        for (int i = 0; i < numComponents; i++) {
            outgoing.put(i, new ArrayList<>());
        }

        for (int i = 0; i < numComponents; i++) {
            for (int dep : compDependencies.get(i)) {
                outgoing.get(dep).add(i);
                indegree[i]++;
            }
        }

        List<Integer> ready = new ArrayList<>();
        for (int i = 0; i < numComponents; i++) {
            if (indegree[i] == 0) {
                ready.add(i);
            }
        }

        List<Integer> order = new ArrayList<>(numComponents);
        int head = 0;
        while (head < ready.size()) {
            int current = ready.get(head++);
            order.add(current);
            for (int neighbor : outgoing.get(current)) {
                indegree[neighbor]--;
                if (indegree[neighbor] == 0) {
                    ready.add(neighbor);
                }
            }
        }

        if (order.size() != numComponents) {
            throw new IllegalStateException(
                    "Component dependency graph contains a cycle after SCC contraction"
            );
        }

        return order;
    }

    private static final class Component {
        final int id;
        final List<Application> apps;
        final int cost;
        final int benefit;

        Component(int id, List<Application> apps, int cost, int benefit) {
            this.id = id;
            this.apps = apps;
            this.cost = cost;
            this.benefit = benefit;
        }
    }

    private static final class BranchAndBoundSolver {
        private final Component[] components;
        private final int[][] directDeps;
        private final BitSet[] transitiveDeps;
        private final int[] densityOrder;
        private final int capacity;
        private final int m;

        private final boolean[] selected;
        private final BitSet excludedMask;
        private boolean[] bestSelection;
        private int bestBenefit;
        private long bestCost;

        BranchAndBoundSolver(
                Component[] components,
                int[][] directDeps,
                BitSet[] transitiveDeps,
                int capacity) {

            this.components = components;
            this.directDeps = directDeps;
            this.transitiveDeps = transitiveDeps;
            this.capacity = capacity;
            this.m = components.length;

            this.selected = new boolean[m];
            this.excludedMask = new BitSet(m);
            this.bestSelection = new boolean[m];
            this.bestBenefit = 0;
            this.bestCost = 0;

            // Pre-calculate density ordering once (descending by benefit/cost ratio)
            Integer[] order = new Integer[m];
            for (int i = 0; i < m; i++) {
                order[i] = i;
            }
            java.util.Arrays.sort(order, (a, b) -> {
                int costA = components[a].cost;
                int costB = components[b].cost;
                int benA = components[a].benefit;
                int benB = components[b].benefit;
                if (costA == 0 && costB == 0) return 0;
                if (costA == 0) return -1;
                if (costB == 0) return 1;
                double ratioA = (double) benA / costA;
                double ratioB = (double) benB / costB;
                return Double.compare(ratioB, ratioA);
            });
            this.densityOrder = new int[m];
            for (int i = 0; i < m; i++) {
                this.densityOrder[i] = order[i];
            }

            // Initialize best solution with a fast greedy topological pass
            initGreedy();
        }

        private void initGreedy() {
            boolean[] greedySel = new boolean[m];
            long currentCost = 0L;
            int currentBenefit = 0;

            for (int i = 0; i < m; i++) {
                boolean eligible = true;
                for (int dep : directDeps[i]) {
                    if (!greedySel[dep]) {
                        eligible = false;
                        break;
                    }
                }
                if (eligible && currentCost + components[i].cost <= capacity) {
                    greedySel[i] = true;
                    currentCost += components[i].cost;
                    currentBenefit += components[i].benefit;
                }
            }

            this.bestBenefit = currentBenefit;
            this.bestCost = currentCost;
            System.arraycopy(greedySel, 0, this.bestSelection, 0, m);
        }

        void solve() {
            search(0, 0L, 0);
        }

        private void search(int index, long currentCost, int currentBenefit) {
            if (index == m) {
                if (currentBenefit > bestBenefit
                        || (currentBenefit == bestBenefit && currentCost < bestCost)) {
                    bestBenefit = currentBenefit;
                    bestCost = currentCost;
                    System.arraycopy(selected, 0, bestSelection, 0, m);
                }
                return;
            }

            // Upper bound pruning
            long remainingCapacity = capacity - currentCost;
            double upperBound = computeFractionalUpperBound(index, remainingCapacity);
            if (currentBenefit + (int) Math.floor(upperBound) < bestBenefit) {
                return;
            }
            if (currentBenefit + (int) Math.floor(upperBound) == bestBenefit && currentCost >= bestCost) {
                return;
            }

            // Excluded-dependency check: if any transitive dependency of this component
            // was excluded on the current branch, this component cannot be included.
            boolean hasExcludedDependency = transitiveDeps[index].intersects(excludedMask);

            // Check eligibility of component `index`
            boolean eligible = !hasExcludedDependency;
            if (eligible) {
                for (int dep : directDeps[index]) {
                    if (!selected[dep]) {
                        eligible = false;
                        break;
                    }
                }
            }

            // Branch 1: Include component (if eligible, not blocked, and fits in remaining capacity)
            if (eligible && components[index].cost <= remainingCapacity) {
                selected[index] = true;
                search(
                        index + 1,
                        currentCost + components[index].cost,
                        currentBenefit + components[index].benefit
                );
                selected[index] = false;
            }

            // Branch 2: Exclude component
            excludedMask.set(index);
            search(index + 1, currentCost, currentBenefit);
            excludedMask.clear(index);
        }

        private double computeFractionalUpperBound(int fromIndex, long remCap) {
            if (remCap <= 0) {
                return 0.0;
            }

            double bound = 0.0;
            long cap = remCap;

            for (int j : densityOrder) {
                if (j < fromIndex) {
                    continue;
                }
                if (transitiveDeps[j].intersects(excludedMask)) {
                    continue;
                }

                int compCost = components[j].cost;
                int compBenefit = components[j].benefit;

                if (compCost == 0) {
                    bound += compBenefit;
                } else if (compCost <= cap) {
                    bound += compBenefit;
                    cap -= compCost;
                } else {
                    bound += ((double) cap / compCost) * compBenefit;
                    break;
                }
            }

            return bound;
        }

        int getBestBenefit() {
            return bestBenefit;
        }

        List<Application> getSelectedApplications() {
            List<Application> result = new ArrayList<>();
            for (int i = 0; i < m; i++) {
                if (bestSelection[i]) {
                    result.addAll(components[i].apps);
                }
            }
            return result;
        }
    }

    private static final class TarjanSCC {
        private final int n;
        private final List<List<Integer>> adj;
        private int timer;
        private final int[] dfn;
        private final int[] low;
        private final boolean[] inStack;
        private final List<Integer> stack;
        private final List<List<Integer>> sccs;

        TarjanSCC(int n, List<List<Integer>> adj) {
            this.n = n;
            this.adj = adj;
            this.dfn = new int[n];
            this.low = new int[n];
            this.inStack = new boolean[n];
            this.stack = new ArrayList<>();
            this.sccs = new ArrayList<>();

            for (int i = 0; i < n; i++) {
                dfn[i] = -1;
                low[i] = -1;
            }

            for (int i = 0; i < n; i++) {
                if (dfn[i] == -1) {
                    dfs(i);
                }
            }
        }

        private void dfs(int u) {
            dfn[u] = low[u] = ++timer;
            stack.add(u);
            inStack[u] = true;

            for (int v : adj.get(u)) {
                if (dfn[v] == -1) {
                    dfs(v);
                    low[u] = Math.min(low[u], low[v]);
                } else if (inStack[v]) {
                    low[u] = Math.min(low[u], dfn[v]);
                }
            }

            if (low[u] == dfn[u]) {
                List<Integer> scc = new ArrayList<>();
                while (true) {
                    int top = stack.remove(stack.size() - 1);
                    inStack[top] = false;
                    scc.add(top);
                    if (top == u) {
                        break;
                    }
                }
                sccs.add(scc);
            }
        }

        List<List<Integer>> getSCCs() {
            return sccs;
        }
    }

    private static List<Application> reconstructSelection(
            List<Application> applications,
            int[][] dp,
            int capacity,
            MonetaryScale scale) {

        List<Application> selected =
                new ArrayList<>();

        int currentCapacity = capacity;

        for (int i = applications.size(); i >= 1; i--) {

            if (dp[i][currentCapacity] !=
                    dp[i - 1][currentCapacity]) {

                Application application =
                        applications.get(i - 1);

                selected.add(application);

                int cost = scale.toCostUnits(
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

    /**
     * Dynamic monetary scaling policy.
     *
     * Computes a scale factor (the "natural monetary granularity")
     * from the budget and all application costs, ensuring that the
     * DP capacity never exceeds MAX_DP_CAPACITY.
     *
     * Uses conservative rounding:
     *   - Costs are scaled with ceiling: scaledCost = ceil(cost / scale)
     *   - Budget/capacity is scaled with floor: scaledBudget = floor(budget / scale)
     *
     * Mathematical Feasibility Guarantee:
     *   Since cost <= scaledCost * scale and scaledBudget * scale <= budget,
     *   any combination of items satisfying sum(scaledCost) <= scaledBudget
     *   strictly satisfies sum(cost) <= budget in original currency.
     *
     * A final exact BigDecimal check validates that the selected portfolio
     * strictly satisfies the budget constraint.
     */
    static final class MonetaryScale {

        private final BigDecimal scaleUnit;

        private MonetaryScale(BigDecimal scaleUnit) {
            this.scaleUnit = scaleUnit;
        }

        /**
         * Creates a MonetaryScale from the budget and all
         * application costs.
         *
         * Algorithm:
         * 1. Determine the maximum decimal scale across all
         *    monetary values.
         * 2. Convert all values to unscaled integers by
         *    multiplying by 10^maxScale.
         * 3. Compute the GCD of all unscaled integers to find
         *    the natural monetary granularity.
         * 4. If the resulting DP capacity (budget / granularity)
         *    exceeds MAX_DP_CAPACITY, increase the granularity.
         */
        static MonetaryScale create(
                BigDecimal budget,
                List<Application> applications) {

            // Step 1: Find the maximum decimal scale
            int maxScale = budget.scale();
            for (Application app : applications) {
                int appScale = app.getModernizationCost().scale();
                if (appScale > maxScale) {
                    maxScale = appScale;
                }
            }

            // Ensure at least scale 0
            if (maxScale < 0) {
                maxScale = 0;
            }

            // Step 2: Convert all values to unscaled integers
            BigInteger budgetUnscaled = budget
                    .setScale(maxScale, RoundingMode.UNNECESSARY)
                    .unscaledValue();

            List<BigInteger> costValues = new ArrayList<>();
            for (Application app : applications) {
                BigDecimal cost = app.getModernizationCost();
                BigInteger costUnscaled = cost
                        .setScale(maxScale, RoundingMode.UNNECESSARY)
                        .unscaledValue();
                if (costUnscaled.signum() > 0) {
                    costValues.add(costUnscaled);
                }
            }

            // Step 3: Compute GCD of all non-zero values
            BigInteger gcd = budgetUnscaled;
            for (BigInteger costVal : costValues) {
                gcd = gcd.signum() == 0 ? costVal : gcd.gcd(costVal);
            }

            // Edge case: if budget and all costs are zero, use default scale of 1
            if (gcd.signum() == 0) {
                return new MonetaryScale(BigDecimal.ONE);
            }

            // gcd is now in units of 10^(-maxScale)
            // The natural granularity in original currency:
            // granularity = gcd / 10^maxScale
            BigDecimal naturalGranularity = new BigDecimal(gcd, maxScale);

            // Step 4: Check if budget / granularity fits in MAX_DP_CAPACITY
            BigDecimal scaledCapacity = budget.divide(
                    naturalGranularity, 0, RoundingMode.FLOOR
            );

            if (scaledCapacity.compareTo(
                    BigDecimal.valueOf(MAX_DP_CAPACITY)) <= 0) {
                return new MonetaryScale(naturalGranularity);
            }

            // Need to increase the scale unit so that
            // floor(budget / scaleUnit) <= MAX_DP_CAPACITY
            BigDecimal maxCapBd = BigDecimal.valueOf(MAX_DP_CAPACITY);
            BigDecimal minScaleUnit = budget.divide(
                    maxCapBd, maxScale, RoundingMode.CEILING
            );

            // Round up to the nearest multiple of naturalGranularity
            BigDecimal multiple = minScaleUnit.divide(
                    naturalGranularity, 0, RoundingMode.CEILING
            );
            BigDecimal adjustedScaleUnit =
                    multiple.multiply(naturalGranularity);

            // Final verification
            BigDecimal finalCapacity = budget.divide(
                    adjustedScaleUnit, 0, RoundingMode.FLOOR
            );

            if (finalCapacity.compareTo(
                    BigDecimal.valueOf(MAX_DP_CAPACITY)) > 0) {
                adjustedScaleUnit = budget.divide(
                        maxCapBd, maxScale, RoundingMode.CEILING
                );
                finalCapacity = budget.divide(
                        adjustedScaleUnit, 0, RoundingMode.FLOOR
                );
                if (finalCapacity.compareTo(
                        BigDecimal.valueOf(MAX_DP_CAPACITY)) > 0) {
                    throw new IllegalStateException(
                            "Unable to construct a valid monetary scale within DP capacity limit"
                    );
                }
            }

            return new MonetaryScale(adjustedScaleUnit);
        }

        /**
         * Converts a monetary cost to scaled integer units
         * using ceiling rounding (costs are never underestimated).
         *
         * Costs larger than MAX_DP_CAPACITY are bounded to
         * (MAX_DP_CAPACITY + 1) to safely prevent selection without
         * causing integer overflow.
         */
        int toCostUnits(BigDecimal amount) {
            if (amount.signum() == 0) {
                return 0;
            }
            BigDecimal scaled = amount.divide(
                    scaleUnit, 0, RoundingMode.CEILING
            );
            long longValue = scaled.longValue();
            if (longValue > MAX_DP_CAPACITY) {
                return (int) (MAX_DP_CAPACITY + 1);
            }
            return (int) longValue;
        }

        /**
         * Converts a monetary budget to scaled capacity units
         * using floor rounding (capacity is never overestimated).
         */
        int toCapacityUnits(BigDecimal amount) {
            if (amount.signum() == 0) {
                return 0;
            }
            BigDecimal scaled = amount.divide(
                    scaleUnit, 0, RoundingMode.FLOOR
            );
            long longValue = scaled.longValueExact();
            if (longValue > MAX_DP_CAPACITY) {
                throw new IllegalArgumentException(
                        "Scaled capacity " + longValue
                                + " exceeds DP capacity limit "
                                + MAX_DP_CAPACITY
                );
            }
            return (int) longValue;
        }

        /**
         * Returns the scale unit for testing/diagnostics.
         */
        BigDecimal getScaleUnit() {
            return scaleUnit;
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
        }
    }
}
