package com.techoptima.validation;

import com.techoptima.model.Application;
import com.techoptima.model.Criticality;
import com.techoptima.model.Department;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DependencyValidatorTest {

    private Application application(
            long id,
            String name,
            List<Long> dependencies) {

        return new Application(
                id,
                name,
                new BigDecimal("20.00"),
                80,
                Criticality.HIGH,
                Department.OPERATIONS,
                dependencies
        );
    }

    @Test
    void shouldValidateWhenAllDependenciesAreSelected() {

        Application crm =
                application(
                        1L,
                        "CRM",
                        List.of()
                );

        Application analytics =
                application(
                        2L,
                        "Analytics",
                        List.of(1L)
                );

        DependencyValidationResult result =
                DependencyValidator.validate(
                        List.of(crm, analytics)
                );

        assertTrue(result.isValid());
        assertTrue(
                result.getMissingDependencyApplicationIds()
                        .isEmpty()
        );
        assertTrue(
                result.getMissingDependenciesByApplication()
                        .isEmpty()
        );
    }

    @Test
    void shouldRejectWhenDependencyIsNotSelected() {

        Application analytics =
                application(
                        2L,
                        "Analytics",
                        List.of(1L)
                );

        DependencyValidationResult result =
                DependencyValidator.validate(
                        List.of(analytics)
                );

        assertFalse(result.isValid());

        assertEquals(
                List.of(1L),
                result.getMissingDependencyApplicationIds()
        );

        assertEquals(
                List.of(1L),
                result.getMissingDependenciesByApplication()
                        .get(2L)
        );
    }

    @Test
    void shouldDetectMultipleMissingDependencies() {

        Application reporting =
                application(
                        3L,
                        "Reporting",
                        List.of(1L, 2L)
                );

        DependencyValidationResult result =
                DependencyValidator.validate(
                        List.of(reporting)
                );

        assertFalse(result.isValid());

        assertEquals(
                List.of(1L, 2L),
                result.getMissingDependencyApplicationIds()
        );

        assertEquals(
                List.of(1L, 2L),
                result.getMissingDependenciesByApplication()
                        .get(3L)
        );
    }

    @Test
    void shouldValidateApplicationWithNoDependencies() {

        Application crm =
                application(
                        1L,
                        "CRM",
                        List.of()
                );

        DependencyValidationResult result =
                DependencyValidator.validate(
                        List.of(crm)
                );

        assertTrue(result.isValid());
    }

    @Test
    void shouldAcceptEmptySelection() {

        DependencyValidationResult result =
                DependencyValidator.validate(
                        List.of()
                );

        assertTrue(result.isValid());
        assertTrue(
                result.getMissingDependencyApplicationIds()
                        .isEmpty()
        );
    }

    @Test
    void shouldRejectNullInput() {

        assertThrows(
                IllegalArgumentException.class,
                () -> DependencyValidator.validate(null)
        );
    }

    @Test
    void shouldRejectNullApplication() {

        List<Application> applications =
                new ArrayList<>();

        applications.add(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> DependencyValidator.validate(
                        applications
                )
        );
    }

    @Test
    void shouldRejectDuplicateApplicationIds() {

        Application first =
                application(
                        1L,
                        "CRM",
                        List.of()
                );

        Application second =
                application(
                        1L,
                        "Different CRM",
                        List.of()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> DependencyValidator.validate(
                        List.of(first, second)
                )
        );
    }

    @Test
    void shouldDetectMissingDependencyEvenWhenOtherDependenciesExist() {

        Application crm =
                application(
                        1L,
                        "CRM",
                        List.of()
                );

        Application analytics =
                application(
                        2L,
                        "Analytics",
                        List.of(1L)
                );

        Application reporting =
                application(
                        3L,
                        "Reporting",
                        List.of(2L, 99L)
                );

        DependencyValidationResult result =
                DependencyValidator.validate(
                        List.of(
                                crm,
                                analytics,
                                reporting
                        )
                );

        assertFalse(result.isValid());

        assertEquals(
                List.of(99L),
                result.getMissingDependencyApplicationIds()
        );

        assertEquals(
                List.of(99L),
                result.getMissingDependenciesByApplication()
                        .get(3L)
        );
    }
}