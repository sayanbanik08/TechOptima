package com.techoptima.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationTest {

    private Application createValidApplication() {
        return new Application(
                1L,
                "CRM",
                BigDecimal.valueOf(20),
                80,
                Criticality.HIGH,
                Department.SALES,
                List.of()
        );
    }

    @Test
    void shouldCreateValidApplication() {
        Application application = createValidApplication();

        assertEquals(1L, application.getApplicationId());
        assertEquals("CRM", application.getApplicationName());
        assertEquals(BigDecimal.valueOf(20), application.getModernizationCost());
        assertEquals(80, application.getBusinessBenefit());
        assertEquals(Criticality.HIGH, application.getCriticality());
        assertEquals(Department.SALES, application.getDepartment());
        assertTrue(application.getDependencyApplicationIds().isEmpty());
    }

    @Test
    void shouldRejectZeroApplicationId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Application(
                        0L, "CRM", BigDecimal.valueOf(20), 80,
                        Criticality.HIGH, Department.SALES, List.of()
                )
        );
    }

    @Test
    void shouldRejectNegativeApplicationId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Application(
                        -1L, "CRM", BigDecimal.valueOf(20), 80,
                        Criticality.HIGH, Department.SALES, List.of()
                )
        );
    }

    @Test
    void shouldRejectNullApplicationName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Application(
                        1L, null, BigDecimal.valueOf(20), 80,
                        Criticality.HIGH, Department.SALES, List.of()
                )
        );
    }

    @Test
    void shouldRejectBlankApplicationName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Application(
                        1L, "   ", BigDecimal.valueOf(20), 80,
                        Criticality.HIGH, Department.SALES, List.of()
                )
        );
    }

    @Test
    void shouldRejectNullModernizationCost() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Application(
                        1L, "CRM", null, 80,
                        Criticality.HIGH, Department.SALES, List.of()
                )
        );
    }

    @Test
    void shouldRejectNegativeModernizationCost() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Application(
                        1L, "CRM", BigDecimal.valueOf(-1), 80,
                        Criticality.HIGH, Department.SALES, List.of()
                )
        );
    }

    @Test
    void shouldAcceptZeroBusinessBenefit() {
        Application application = new Application(
                1L, "CRM", BigDecimal.valueOf(20), 0,
                Criticality.HIGH, Department.SALES, List.of()
        );

        assertEquals(0, application.getBusinessBenefit());
    }

    @Test
    void shouldAcceptMaximumBusinessBenefit() {
        Application application = new Application(
                1L, "CRM", BigDecimal.valueOf(20), 100,
                Criticality.HIGH, Department.SALES, List.of()
        );

        assertEquals(100, application.getBusinessBenefit());
    }

    @Test
    void shouldRejectNegativeBusinessBenefit() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Application(
                        1L, "CRM", BigDecimal.valueOf(20), -1,
                        Criticality.HIGH, Department.SALES, List.of()
                )
        );
    }

    @Test
    void shouldRejectBusinessBenefitAbove100() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Application(
                        1L, "CRM", BigDecimal.valueOf(20), 101,
                        Criticality.HIGH, Department.SALES, List.of()
                )
        );
    }

    @Test
    void shouldRejectNullCriticality() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Application(
                        1L, "CRM", BigDecimal.valueOf(20), 80,
                        null, Department.SALES, List.of()
                )
        );
    }

    @Test
    void shouldRejectNullDepartment() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Application(
                        1L, "CRM", BigDecimal.valueOf(20), 80,
                        Criticality.HIGH, null, List.of()
                )
        );
    }

    @Test
    void shouldRejectNullDependencyList() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Application(
                        1L, "CRM", BigDecimal.valueOf(20), 80,
                        Criticality.HIGH, Department.SALES, null
                )
        );
    }

    @Test
    void shouldDefensivelyCopyDependencyList() {
        List<Long> dependencies = new ArrayList<>();
        dependencies.add(2L);

        Application application = new Application(
                1L, "Analytics", BigDecimal.valueOf(25), 90,
                Criticality.HIGH, Department.OPERATIONS, dependencies
        );

        dependencies.add(3L);

        assertEquals(List.of(2L), application.getDependencyApplicationIds());
    }

    @Test
    void shouldUseApplicationIdForEquality() {
        Application first = new Application(
                1L, "CRM", BigDecimal.valueOf(20), 80,
                Criticality.HIGH, Department.SALES, List.of()
        );

        Application second = new Application(
                1L, "Different Name", BigDecimal.valueOf(50), 40,
                Criticality.LOW, Department.FINANCE, List.of(5L)
        );

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void shouldNotConsiderDifferentApplicationIdsEqual() {
        Application first = createValidApplication();

        Application second = new Application(
                2L, "CRM", BigDecimal.valueOf(20), 80,
                Criticality.HIGH, Department.SALES, List.of()
        );

        assertNotEquals(first, second);
    }

    @Test
    void shouldReturnReadableToString() {
        Application application = createValidApplication();

        String result = application.toString();

        assertTrue(result.contains("CRM"));
        assertTrue(result.contains("80"));
        assertTrue(result.contains("HIGH"));
        assertTrue(result.contains("SALES"));
    }
}
