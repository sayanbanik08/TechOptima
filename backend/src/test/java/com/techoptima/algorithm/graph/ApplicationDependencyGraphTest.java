package com.techoptima.algorithm.graph;

import com.techoptima.model.Application;
import com.techoptima.model.Criticality;
import com.techoptima.model.Department;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationDependencyGraphTest {

    private Application createApplication(
            long id,
            String name,
            List<Long> dependencies) {

        return new Application(
                id,
                name,
                BigDecimal.valueOf(20),
                80,
                Criticality.HIGH,
                Department.OPERATIONS,
                dependencies
        );
    }

    @Test
    void shouldCreateEmptyGraph() {
        ApplicationDependencyGraph graph =
                new ApplicationDependencyGraph();

        assertEquals(0, graph.size());
        assertEquals(0, graph.edgeCount());
    }

    @Test
    void shouldAddApplicationNode() {
        ApplicationDependencyGraph graph =
                new ApplicationDependencyGraph();

        graph.addApplication(1L);

        assertTrue(graph.containsApplication(1L));
        assertEquals(1, graph.size());
        assertEquals(0, graph.edgeCount());
    }

    @Test
    void shouldCreateDependencyInCorrectDirection() {
        ApplicationDependencyGraph graph =
                new ApplicationDependencyGraph();

        graph.addApplication(1L); // CRM
        graph.addApplication(2L); // Analytics

        graph.addDependency(2L, 1L);

        assertEquals(List.of(2L), graph.getDependents(1L));
        assertTrue(graph.getDependents(2L).isEmpty());
    }

    @Test
    void shouldPreventDuplicateEdges() {
        ApplicationDependencyGraph graph =
                new ApplicationDependencyGraph();

        graph.addApplication(1L);
        graph.addApplication(2L);

        graph.addDependency(2L, 1L);
        graph.addDependency(2L, 1L);

        assertEquals(1, graph.edgeCount());
        assertEquals(List.of(2L), graph.getDependents(1L));
    }

    @Test
    void shouldBuildGraphFromApplications() {
        Application crm =
                createApplication(1L, "CRM", List.of());

        Application analytics =
                createApplication(2L, "Analytics", List.of(1L));

        ApplicationDependencyGraph graph =
                DependencyGraphBuilder.build(
                        List.of(crm, analytics)
                );

        assertEquals(
                List.of(2L),
                graph.getDependents(1L)
        );

        assertEquals(2, graph.size());
        assertEquals(1, graph.edgeCount());
    }

    @Test
    void shouldSupportMultipleDependencies() {
        Application crm =
                createApplication(1L, "CRM", List.of());

        Application erp =
                createApplication(2L, "ERP", List.of());

        Application analytics =
                createApplication(
                        3L,
                        "Analytics",
                        List.of(1L, 2L)
                );

        ApplicationDependencyGraph graph =
                DependencyGraphBuilder.build(
                        List.of(crm, erp, analytics)
                );

        assertEquals(
                List.of(3L),
                graph.getDependents(1L)
        );

        assertEquals(
                List.of(3L),
                graph.getDependents(2L)
        );

        assertEquals(2, graph.edgeCount());
    }

    @Test
    void shouldPreserveUnknownDependencyForLaterValidation() {
        Application analytics =
                createApplication(
                        3L,
                        "Analytics",
                        List.of(99L)
                );

        ApplicationDependencyGraph graph =
                DependencyGraphBuilder.build(
                        List.of(analytics)
                );

        assertFalse(graph.containsApplication(99L));
        assertTrue(graph.getNodeIds().contains(99L));
        assertEquals(List.of(3L), graph.getDependents(99L));
    }

    @Test
    void shouldRejectNullApplicationCollection() {
        assertThrows(
                IllegalArgumentException.class,
                () -> DependencyGraphBuilder.build(null)
        );
    }

    @Test
    void shouldRejectNullApplicationElement() {
        List<Application> applications = new ArrayList<>();
        applications.add(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> DependencyGraphBuilder.build(applications)
        );
    }

    @Test
    void shouldRejectInvalidApplicationId() {
        ApplicationDependencyGraph graph =
                new ApplicationDependencyGraph();

        assertThrows(
                IllegalArgumentException.class,
                () -> graph.addApplication(0L)
        );
    }

    @Test
    void shouldExposeReadOnlyAdjacencyList() {
        Application crm =
                createApplication(1L, "CRM", List.of());

        Application analytics =
                createApplication(2L, "Analytics", List.of(1L));

        ApplicationDependencyGraph graph =
                DependencyGraphBuilder.build(
                        List.of(crm, analytics)
                );

        Map<Long, List<Long>> adjacency =
                graph.getAdjacencyList();

        assertThrows(
                UnsupportedOperationException.class,
                () -> adjacency.put(99L, List.of())
        );
    }
}