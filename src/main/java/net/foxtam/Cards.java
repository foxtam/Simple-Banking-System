package net.foxtam;

import org.sqlite.SQLiteDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Cards {
    private static final String CARDS_TABLE = "CARDS";
    private static final String ID_COLUMN = "id";
    private static final String NUMBER_COLUMN = "number";
    private static final String PIN_COLUMN = "pin";
    private static final String BALANCE_COLUMN = "balance";

    private final SQLiteDataSource dataSource;
    private int numberOfCards;

    private Cards(SQLiteDataSource dataSource) throws SQLException {
        this.dataSource = dataSource;
        connectAnd(Cards::createTable);
        numberOfCards = countCards();
    }

    private void connectAnd(ConsumerThrowSQLException<Statement> consumer) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                consumer.accept(statement);
            }
        }
    }

    private static void createTable(Statement statement) throws SQLException {
        String query =
                String.format(
                        "CREATE TABLE IF NOT EXISTS %s(" +
                                "%s INTEGER PRIMARY KEY, " +
                                "%s TEXT NOT NULL, " +
                                "%s TEXT NOT NULL, " +
                                "%s INTEGER DEFAULT 0)",
                        CARDS_TABLE,
                        ID_COLUMN,
                        NUMBER_COLUMN,
                        PIN_COLUMN,
                        BALANCE_COLUMN);
        statement.executeUpdate(query);
    }

    private int countCards() throws SQLException {
        AtomicInteger count = new AtomicInteger();
        connectAnd(countDbRows(count));
        return count.get();
    }

    private ConsumerThrowSQLException<Statement> countDbRows(AtomicInteger count) {
        return statement -> {
            String countQuery = "SELECT COUNT(1) as count FROM " + CARDS_TABLE;
            try (ResultSet resultSet = statement.executeQuery(countQuery)) {
                if (resultSet.next()) {
                    count.set(resultSet.getInt("count"));
                }
            }
        };
    }

    public static Cards loadFromDb(String dbFileName) throws SQLException {
        String url = "jdbc:sqlite:" + dbFileName;
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);
        return new Cards(dataSource);
    }

    public void add(Card card) throws SQLException {
        connectAnd(insertCard(card));
    }

    private ConsumerThrowSQLException<Statement> insertCard(Card card) {
        return statement -> {
            String query =
                    String.format(
                            "INSERT INTO %s VALUES (%d, %s, %s, %d);",
                            CARDS_TABLE,
                            ++numberOfCards,
                            card.getNumber(),
                            card.getPin(),
                            card.getBalance());

            statement.executeUpdate(query);
        };
    }

    public Optional<Card> findCard(String number, String pin) throws SQLException {
        AtomicReference<Card> atomicReference = new AtomicReference<>();

        connectAnd(statement -> {
            String query =
                    String.format(
                            "SELECT %s FROM %s " +
                                    "WHERE %s = %s AND %s = %s;",
                            BALANCE_COLUMN,
                            CARDS_TABLE,
                            NUMBER_COLUMN,
                            number,
                            PIN_COLUMN,
                            pin);

            try (ResultSet resultSet = statement.executeQuery(query)) {
                if (resultSet.next()) {
                    int balance = resultSet.getInt(BALANCE_COLUMN);
                    atomicReference.set(new Card(number, pin, balance));
                }
            }
        });

        return Optional.ofNullable(atomicReference.get());
    }
}
