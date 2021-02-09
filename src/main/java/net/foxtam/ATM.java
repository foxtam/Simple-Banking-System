package net.foxtam;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Scanner;

public class ATM {

    private final Scanner scanner = new Scanner(System.in);
    private final Bank bank;
    private boolean showMainMenu = true;

    public ATM(String fileName) throws SQLException {
        this.bank = Bank.loadCardsFromDb(fileName);
    }

    public static void main(String[] args) throws SQLException {
        String dbFileName = getValueBy(args, "-fileName");
        new ATM(dbFileName).run();
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
        while (true) {
            try {
                return Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Incorrect input. Try again:");
            }
        }
    }

    private void createAccount() throws SQLException {
        Bank.Card card = bank.newCard();

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

        Optional<Bank.Card> optionalCard = bank.findCard(number, pin);
        if (optionalCard.isPresent()) {
            System.out.println("You have successfully logged in!");
            accountInteraction(optionalCard.get());
        } else {
            System.out.println("Wrong card number or PIN!");
        }
    }

    private void accountInteraction(Bank.Card card) throws SQLException {
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
                    addIncome(card);
                    break;
                case 3:
                    doTransfer(card);
                    break;
                case 4:
                    closeAccount(card);
                case 5:
                    return;
            }
        }
    }

    private void printAccountMenu() {
        System.out.println("1. Balance\n" +
                "2. Add income\n" +
                "3. Do transfer\n" +
                "4. Close account\n" +
                "5. Log out\n" +
                "0. Exit");
    }

    private void addIncome(Bank.Card card) throws SQLException {
        System.out.println("Enter income:");
        int income = Integer.parseInt(scanner.nextLine());
        card.addIncome(income);
        System.out.println("Income was added!");
    }

    private void doTransfer(Bank.Card card) throws SQLException {
        System.out.println("Transfer\nEnter card number:");
        String targetNumber = scanner.nextLine().trim();
        if (targetNumber.equals(card.getNumber())) {
            System.out.println("You can't transfer money to the same account!");
        } else if (!Bank.isCorrectCardNumber(targetNumber)) {
            System.out.println("Probably you made mistake in the card number. Please try again!");
        } else {
            Optional<Bank.Card> targetCard = bank.findCard(targetNumber);
            if (targetCard.isEmpty()) {
                System.out.println("Such a card does not exist.");
            } else {
                transfer(card, targetCard.get());
            }
        }
    }

    private void closeAccount(Bank.Card card) throws SQLException {
        card.delete();
    }

    private void transfer(Bank.Card card, Bank.Card targetCard) throws SQLException {
        System.out.println("Enter how much money you want to transfer:");
        int moneyAmount = Integer.parseInt(scanner.nextLine());
        try {
            card.transferMoneyTo(targetCard, moneyAmount);
        } catch (NotEnoughMoneyException e) {
            System.out.println("Not enough money!");
        }
    }
}
