package com.techoptima.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TransformationBudgetTest {

    @Test
    void shouldCreateValidBudget() {
        TransformationBudget budget =
                new TransformationBudget(BigDecimal.valueOf(100));

        assertEquals(BigDecimal.valueOf(100), budget.getBudgetAmount());
    }

    @Test
    void shouldAcceptZeroBudget() {
        TransformationBudget budget =
                new TransformationBudget(BigDecimal.ZERO);

        assertEquals(BigDecimal.ZERO, budget.getBudgetAmount());
    }

    @Test
    void shouldRejectNegativeBudget() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TransformationBudget(BigDecimal.valueOf(-1))
        );
    }

    @Test
    void shouldRejectNullBudget() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TransformationBudget(null)
        );
    }

    @Test
    void shouldSupportEqualityForSameBudgetAmount() {
        TransformationBudget first =
                new TransformationBudget(BigDecimal.valueOf(100));

        TransformationBudget second =
                new TransformationBudget(BigDecimal.valueOf(100));

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void shouldNotConsiderDifferentBudgetAmountsEqual() {
        TransformationBudget first =
                new TransformationBudget(BigDecimal.valueOf(100));

        TransformationBudget second =
                new TransformationBudget(BigDecimal.valueOf(200));

        assertNotEquals(first, second);
    }

    @Test
    void shouldReturnReadableToString() {
        TransformationBudget budget =
                new TransformationBudget(BigDecimal.valueOf(100));

        String result = budget.toString();

        assertTrue(result.contains("100"));
    }
}
