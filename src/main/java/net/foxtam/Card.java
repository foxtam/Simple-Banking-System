package net.foxtam;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

public class Card {
    private static final int numberLength = 16;
    private static final int[] numberPrefix = {4, 0, 0, 0, 0, 0};
    private static final Random random = new Random();
    private final String number;
    private final String pin;
    private int balance;

    public Card(String number, String pin) {
        this.number = number;
        this.pin = pin;
        this.balance = 0;
    }

    public static Card generateRandom() {
        return new Card(
                getRandomNumber(),
                getRandomPIN());
    }

    private static String getRandomNumber() {
        int[] cardNumberDigits = createNotCheckedNumber();
        setCheckDigit(cardNumberDigits);
        return asString(cardNumberDigits);
    }

    private static String getRandomPIN() {
        return asString(getNRandomDigits(4));
    }

    private static int[] createNotCheckedNumber() {
        int[] cardNumberDigits = new int[numberLength];
        System.arraycopy(numberPrefix, 0, cardNumberDigits, 0, numberPrefix.length);
        int[] randomDigits = getNRandomDigits(numberLength - numberPrefix.length - 1);
        System.arraycopy(randomDigits, 0, cardNumberDigits, numberPrefix.length, randomDigits.length);
        return cardNumberDigits;
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

    private static int[] getNRandomDigits(int length) {
        int[] digits = new int[length];
        for (int i = 0; i < length; i++) {
            digits[i] = random.nextInt(10);
        }
        return digits;
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
        return IntStream.of(digits).sum();
    }

    public int getBalance() {
        return balance;
    }

    public String getNumber() {
        return number;
    }

    public String getPin() {
        return pin;
    }
}
