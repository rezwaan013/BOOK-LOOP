package com.bookloop.dao;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Generic CRUD contract for DAO classes.
 * Demonstrates the use of a Java <b>interface</b> with generics:
 * every entity DAO exposes the same Create/Read/Delete vocabulary.
 *
 * @param <T> entity type
 * @param <K> primary-key type
 */
public interface CrudRepository<T, K> {

    /** Persists a new entity and returns its generated key. */
    int save(T entity) throws SQLException;

    /** Finds an entity by primary key. */
    Optional<T> findById(K id) throws SQLException;

    /** Deletes an entity by primary key. */
    void deleteById(K id) throws SQLException;
}
