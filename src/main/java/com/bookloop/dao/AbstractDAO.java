package com.bookloop.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Shared base class for all DAO classes.
 * Demonstrates an <b>abstract class</b>: it holds the shared SQLite
 * {@link Connection} and forces subclasses to implement row-mapping,
 * while providing concrete helper behaviour (connection access).
 *
 * @param <T> entity type produced by {@link #map(ResultSet)}
 */
public abstract class AbstractDAO<T> {

    /** Shared connection from {@link DatabaseManager} (single reusable connection). */
    protected final Connection con = DatabaseManager.getInstance().getConnection();

    /** Maps the current row of a {@link ResultSet} to an entity. */
    protected abstract T map(ResultSet rs) throws SQLException;
}
