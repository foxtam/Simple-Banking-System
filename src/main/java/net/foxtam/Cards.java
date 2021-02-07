package net.foxtam;

import org.sqlite.SQLiteDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class Cards {
    private static final String CARDS_TABLE = "CARDS";
    private static final String ID = "id";
    private static final String NUMBER_COLUMN = "number";
    private static final String PIN_COLUMN = "pin";
    private static final String BALANCE_COLUMN = "balance";

    private final SQLiteDataSource dataSource;
    private int numberOfCards;

    private Cards(SQLiteDataSource dataSource) {
        this.dataSource = dataSource;
        connectAnd(Cards::createTable);
        numberOfCards = countCards();
    }

    private void connectAnd(Consumer<Statement> consumer) {
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                consumer.accept(statement);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createTable(Statement statement) {
        String query =
                String.format(
                        "CREATE TABLE IF NOT EXISTS %s(" +
                                "%s INTEGER PRIMARY KEY, " +
                                "%s TEXT NOT NULL, " +
                                "%s TEXT NOT NULL, " +
                                "%s INTEGER DEFAULT 0)",
                        CARDS_TABLE,
                        ID,
                        NUMBER_COLUMN,
                        PIN_COLUMN,
                        BALANCE_COLUMN);
        try {
            statement.executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int countCards() {
        AtomicInteger count = new AtomicInteger();
        connectAnd(countDbRows(count));
        return count.get();
    }

    private Consumer<Statement> countDbRows(AtomicInteger count) {
        return statement -> {
            String countQuery = "SELECT COUNT(1) as count FROM " + CARDS_TABLE;
            try (ResultSet resultSet = statement.executeQuery(countQuery)) {
                if (resultSet.next()) {
                    count.set(resultSet.getInt("count"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };
    }

    public static Cards loadFromDb(String dbFileName) {
        String url = "jdbc:sqlite:" + dbFileName;
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);
        return new Cards(dataSource);
    }

    public void add(Card card) {
        connectAnd(insertCard(card));
    }

    private Consumer<Statement> insertCard(Card card) {
        return statement -> {
            String query =
                    String.format(
                            "INSERT INTO %s VALUES (%d, %s, %s, %d);",
                            CARDS_TABLE,
                            ++numberOfCards,
                            card.getNumber(),
                            card.getPin(),
                            card.getBalance());
            try {
                statement.executeUpdate(query);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };
    }

    public Optional<Card> findCard(String number, String pin) {
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
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        return Optional.ofNullable(atomicReference.get());
    }
}
