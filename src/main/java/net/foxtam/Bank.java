package net.foxtam;

import org.sqlite.SQLiteDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

public class Bank {
    private static final String TABLE_NAME = "CARDS";
    private static final String ID_COLUMN = "id";
    private static final String NUMBER_COLUMN = "number";
    private static final String PIN_COLUMN = "pin";
    private static final String BALANCE_COLUMN = "balance";

    private static final int numberLength = 16;
    private static final int pinLength = 4;
    private static final int[] numberPrefix = {4, 0, 0, 0, 0, 0};
    private static final Random random = new Random();

    private final SQLiteDataSource dataSource;

    private Bank(SQLiteDataSource dataSource) throws SQLException {
        this.dataSource = dataSource;
        connectAnd(Bank::createTable);
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
                                "%s VARCHAR(16) NOT NULL, " +
                                "%s VARCHAR(4) NOT NULL, " +
                                "%s INTEGER DEFAULT 0)",
                        TABLE_NAME,
                        ID_COLUMN,
                        NUMBER_COLUMN,
                        PIN_COLUMN,
                        BALANCE_COLUMN);
        statement.executeUpdate(query);
    }

    public static boolean isCorrectCardNumber(String number) {
        if (number.length() != numberLength || hasNotDigit(number)) return false;
        return checkLuhn(number);
    }

    private static boolean hasNotDigit(String number) {
        for (char c : number.toCharArray()) {
            if (c < '0' || c > '9') return true;
        }
        return false;
    }

    private static boolean checkLuhn(String number) {
        int[] digits = toIntArray(number);
        setCheckDigit(digits);
        return asString(digits).equals(number);
    }

    private static int[] toIntArray(String number) {
        int[] array = new int[number.length()];
        for (int i = 0; i < number.length(); i++) {
            array[i] = number.charAt(i) - '0';
        }
        return array;
    }

    private static void setCheckDigit(int[] digits) {
        int[] copy = Arrays.copyOf(digits, numberLength);
        twiceEvens(copy);
        subtractNine(copy);
        int sum = sum(copy);
        int checkDigit = (10 - (sum % 10)) % 10;
        digits[numberLength - 1] = checkDigit;
    }

    private static String asString(int[] digits) {
        StringBuilder builder = new StringBuilder();
        for (int digit : digits) {
            builder.append(digit);
        }
        return builder.toString();
    }

    private static void twiceEvens(int[] digits) {
        for (int i = 0; i < digits.length; i += 2) {
            digits[i] *= 2;
        }
    }

    private static void subtractNine(int[] digits) {
        for (int i = 0; i < numberLength; i += 2) {
            if (digits[i] > 9) {
                digits[i] -= 9;
            }
        }
    }

    private static int sum(int[] digits) {
        return IntStream.of(digits).limit(numberLength - 1).sum();
    }

    public static Bank loadCardsFromDb(String dbFileName) throws SQLException {
        String url = "jdbc:sqlite:" + dbFileName;
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);
        return new Bank(dataSource);
    }

    public Card newCard() throws SQLException {
        String number = getRandomNumber();
        String pin = getRandomPIN();
        addCard(number, pin, 0);
        return findCard(number, pin).orElseThrow(IllegalStateException::new);
    }

    private static String getRandomNumber() {
        int[] cardNumberDigits = createNotCheckedNumber();
        setCheckDigit(cardNumberDigits);
        return asString(cardNumberDigits);
    }

    private static String getRandomPIN() {
        return asString(getNRandomDigits(pinLength));
    }

    private void addCard(String number, String pin, int balance) throws SQLException {
        connectAnd(insertCard(number, pin, balance));
    }

    public Optional<Card> findCard(String number, String pin) throws SQLException {
        AtomicReference<Card> cardReference = new AtomicReference<>();
        connectAnd(writeCard(number, pin, cardReference));
        return Optional.ofNullable(cardReference.get());
    }

    private static int[] createNotCheckedNumber() {
        int[] cardNumberDigits = new int[numberLength];
        System.arraycopy(numberPrefix, 0, cardNumberDigits, 0, numberPrefix.length);
        int[] randomDigits = getNRandomDigits(numberLength - numberPrefix.length - 1);
        System.arraycopy(randomDigits, 0, cardNumberDigits, numberPrefix.length, randomDigits.length);
        return cardNumberDigits;
    }

    private static int[] getNRandomDigits(int length) {
        int[] digits = new int[length];
        for (int i = 0; i < length; i++) {
            digits[i] = random.nextInt(10);
        }
        return digits;
    }

    private ConsumerThrowSQLException<Statement> insertCard(String number, String pin, int balance) {
        return statement -> {
            String query =
                    String.format(
                            "INSERT INTO %s (%s, %s, %s) VALUES ('%s', '%s', %d);",
                            TABLE_NAME,
                            NUMBER_COLUMN,
                            PIN_COLUMN,
                            BALANCE_COLUMN,
                            number,
                            pin,
                            balance);

            statement.executeUpdate(query);
        };
    }

    private ConsumerThrowSQLException<Statement> writeCard(String number, String pin, AtomicReference<Card> cardReference) {
        return statement -> {
            String query =
                    String.format(
                            "SELECT %s FROM %s WHERE %s = '%s' AND %s = '%s';",
                            ID_COLUMN,
                            TABLE_NAME,
                            NUMBER_COLUMN,
                            number,
                            PIN_COLUMN,
                            pin);

            try (ResultSet resultSet = statement.executeQuery(query)) {
                if (resultSet.next()) {
                    int id = resultSet.getInt(ID_COLUMN);
                    cardReference.set(new Card(id));
                }
            }
        };
    }

    private ConsumerThrowSQLException<Statement> countDbRows(AtomicInteger count) {
        return statement -> {
            String countQuery = "SELECT COUNT(1) as count FROM " + TABLE_NAME;
            try (ResultSet resultSet = statement.executeQuery(countQuery)) {
                if (resultSet.next()) {
                    count.set(resultSet.getInt("count"));
                }
            }
        };
    }

    public Optional<Card> findCard(String number) throws SQLException {
        AtomicReference<Card> cardReference = new AtomicReference<>();
        connectAnd(writeCard(number, cardReference));
        return Optional.ofNullable(cardReference.get());
    }

    private ConsumerThrowSQLException<Statement> writeCard(String number, AtomicReference<Card> cardReference) {
        return statement -> {
            String query =
                    String.format(
                            "SELECT %s FROM %s " +
                                    "WHERE %s = '%s'",
                            ID_COLUMN,
                            TABLE_NAME,
                            NUMBER_COLUMN,
                            number);

            try (ResultSet resultSet = statement.executeQuery(query)) {
                if (resultSet.next()) {
                    int id = resultSet.getInt(ID_COLUMN);
                    cardReference.set(new Card(id));
                }
            }
        };
    }

    public class Card {
        private final int id;

        private Card(int id) {
            this.id = id;
        }

        public String getNumber() throws SQLException {
            return getProperty(NUMBER_COLUMN);
        }

        private String getProperty(String columnName) throws SQLException {
            AtomicReference<String> value = new AtomicReference<>();
            connectAnd(writeProperty(columnName, value));
            return value.get();
        }

        private ConsumerThrowSQLException<Statement> writeProperty(String columnName, AtomicReference<String> value) {
            return statement -> {
                String query =
                        String.format(
                                "SELECT %s FROM %s WHERE %s = %d",
                                columnName,
                                TABLE_NAME,
                                ID_COLUMN,
                                id);

                try (ResultSet resultSet = statement.executeQuery(query)) {
                    if (resultSet.next()) {
                        value.set(resultSet.getString(columnName));
                    } else {
                        throw new IllegalStateException("Card(id=" + id + " not found.");
                    }
                }
            };
        }

        public String getPin() throws SQLException {
            return getProperty(PIN_COLUMN);
        }

        public void transferMoneyTo(Card targetCard, int moneyAmount) throws NotEnoughMoneyException, SQLException {
            if (getBalance() < moneyAmount) {
                throw new NotEnoughMoneyException("Not enough money");
            }
            addIncome(-moneyAmount);
            targetCard.addIncome(moneyAmount);
        }

        public int getBalance() throws SQLException {
            return Integer.parseInt(getProperty(BALANCE_COLUMN));
        }

        public void addIncome(int income) throws SQLException {
            connectAnd(statement -> {
                String query =
                        String.format(
                                "UPDATE %1$s SET %2$s = %2$s + %3$d WHERE %4$s = %5$d",
                                TABLE_NAME,
                                BALANCE_COLUMN,
                                income,
                                ID_COLUMN,
                                id);

                statement.executeUpdate(query);
            });
        }

        public void delete() throws SQLException {
            connectAnd(statement -> {
                String query =
                        String.format(
                                "DELETE FROM %s WHERE %s = %d",
                                TABLE_NAME,
                                ID_COLUMN,
                                id);

                statement.executeUpdate(query);
            });
        }
    }
}
