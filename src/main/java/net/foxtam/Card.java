package net.foxtam;

import java.util.Random;

public class Card {
    private static Random random = new Random();
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
        return "400000" + getNRandomDigits(10);
    }

    private static String getRandomPIN() {
        return getNRandomDigits(4);
    }

    private static String getNRandomDigits(int length) {
        StringBuilder number = new StringBuilder();
        for (int i = 0; i < length; i++) {
            number.append(random.nextInt(10));
        }
        return number.toString();
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
