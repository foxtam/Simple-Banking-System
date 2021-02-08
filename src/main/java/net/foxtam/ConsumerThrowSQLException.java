package net.foxtam;

import java.sql.SQLException;

@FunctionalInterface
public interface ConsumerThrowSQLException<T> {
    void accept(T t) throws SQLException;
}
