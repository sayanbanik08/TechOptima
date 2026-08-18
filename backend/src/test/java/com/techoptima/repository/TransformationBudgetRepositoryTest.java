package com.techoptima.repository;

import com.techoptima.database.DatabaseConnection;
import com.techoptima.model.TransformationBudget;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.junit.jupiter.api.Assertions.*;

class TransformationBudgetRepositoryTest {

    private final TransformationBudgetRepository repository =
            new TransformationBudgetRepository();

    private TransformationBudget originalBudget;

    @BeforeEach
    void cleanBefore() throws Exception {
        originalBudget = repository.findLatest();
        cleanup();
    }

    @AfterEach
    void cleanAfter() throws Exception {
        cleanup();
        if (originalBudget != null) {
            repository.replaceCurrent(originalBudget);
        }
    }

    @Test
    void shouldSaveAndFindLatestBudget()
            throws Exception {

        TransformationBudget budget =
                new TransformationBudget(
                        new BigDecimal("60.00")
                );

        repository.save(budget);

        TransformationBudget loaded =
                repository.findLatest();

        assertNotNull(loaded);

        assertEquals(
                new BigDecimal("60.00"),
                loaded.getBudgetAmount()
        );
    }

    @Test
    void shouldReturnLatestInsertedBudget()
            throws Exception {

        repository.save(
                new TransformationBudget(
                        new BigDecimal("40.00")
                )
        );

        repository.save(
                new TransformationBudget(
                        new BigDecimal("75.50")
                )
        );

        TransformationBudget loaded =
                repository.findLatest();

        assertNotNull(loaded);

        assertEquals(
                new BigDecimal("75.50"),
                loaded.getBudgetAmount()
        );
    }

    @Test
    void shouldReplaceCurrentBudget()
            throws Exception {

        repository.save(
                new TransformationBudget(
                        new BigDecimal("40.00")
                )
        );

        repository.replaceCurrent(
                new TransformationBudget(
                        new BigDecimal("100.00")
                )
        );

        TransformationBudget loaded =
                repository.findLatest();

        assertNotNull(loaded);

        assertEquals(
                new BigDecimal("100.00"),
                loaded.getBudgetAmount()
        );

        assertEquals(
                1,
                countBudgets()
        );
    }

    @Test
    void shouldReturnNullWhenNoBudgetExists()
            throws Exception {

        assertNull(
                repository.findLatest()
        );
    }

    @Test
    void shouldRejectNullBudgetOnSave() {

        assertThrows(
                IllegalArgumentException.class,
                () -> repository.save(null)
        );
    }

    @Test
    void shouldRejectNullBudgetOnReplace() {

        assertThrows(
                IllegalArgumentException.class,
                () -> repository.replaceCurrent(null)
        );
    }

    private int countBudgets() throws Exception {

        String sql =
                "SELECT COUNT(*) AS total " +
                "FROM transformation_budget";

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             var resultSet =
                     statement.executeQuery()) {

            assertTrue(resultSet.next());

            return resultSet.getInt("total");
        }
    }

    private void cleanup() throws Exception {

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(
                             "DELETE FROM transformation_budget"
                     )) {

            statement.executeUpdate();
        }
    }
}