package fr.valoriatycoon.database;

import java.sql.Connection;

@FunctionalInterface
public interface SqlOperation<T> {
    T execute(Connection connection) throws Exception;
}
