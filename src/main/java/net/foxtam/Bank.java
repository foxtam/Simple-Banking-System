package net.foxtam;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Scanner;

public class Bank {

    private final Scanner scanner = new Scanner(System.in);
    private final Cards cards;
    private boolean showMainMenu = true;

    public Bank(String fileName) throws SQLException {
        this.cards = Cards.loadFromDb(fileName);
    }

    public static void main(String[] args) throws SQLException {
        String dbFileName = getValueBy(args, "-fileName");
        new Bank(dbFileName).run();
    }

    private static String getValueBy(String[] keys, String key) {
        int index = Arrays.asList(keys).indexOf(key);
        if (index == -1) {
            throw new IllegalArgumentException(key);
        }
        return keys[index + 1];
    }

    private void run() throws SQLException {
        while (showMainMenu) {
            showMenu();
            int answer = readUserAnswer();
            switch (answer) {
                case 0 -> showMainMenu = false;
                case 1 -> createAccount();
                case 2 -> logIntoAccount();
            }
        }
        System.out.println("Bye!");
    }

    private void showMenu() {
        System.out.println("1. Create an account\n" +
                "2. Log into account\n" +
                "0. Exit");
    }

    private int readUserAnswer() {
        return Integer.parseInt(scanner.nextLine());
    }

    private void createAccount() throws SQLException {
        Card card = Card.generateRandom();
        cards.add(card);

        System.out.println("Your card has been created");
        System.out.println("Your card number:");
        System.out.println(card.getNumber());
        System.out.println("Your card PIN:");
        System.out.println(card.getPin());
    }

    private void logIntoAccount() throws SQLException {
        System.out.println("Enter your card number:");
        String number = scanner.nextLine();

        System.out.println("Enter your PIN:");
        String pin = scanner.nextLine();

        Optional<Card> optionalCard = cards.findCard(number, pin);
        if (optionalCard.isPresent()) {
            System.out.println("You have successfully logged in!");
            accountInteraction(optionalCard.get());
        } else {
            System.out.println("Wrong card number or PIN!");
        }
    }

    private void accountInteraction(Card card) {
        while (true) {
            printAccountMenu();
            int answer = readUserAnswer();
            switch (answer) {
                case 0:
                    showMainMenu = false;
                    return;
                case 1:
                    System.out.println("Balance: " + card.getBalance());
                    break;
                case 2:
                    return;
            }
        }
    }

    private void printAccountMenu() {
        System.out.println("1. Balance\n" +
                "4. Close account\n" +
                "5. Log out\n" +
                "0. Exit");
    }
}
