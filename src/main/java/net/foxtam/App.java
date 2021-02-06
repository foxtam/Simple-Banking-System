package net.foxtam;

import java.util.Optional;
import java.util.Scanner;

public class App {

    private final Scanner scanner = new Scanner(System.in);
    private final Cards cards = new Cards();
    private boolean showMainMenu = true;

    public static void main(String[] args) {
        new App().run();
    }

    private void run() {
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

    private void createAccount() {
        Card card = Card.generateRandom();
        cards.add(card);

        System.out.println("Your card has been created");
        System.out.println("Your card number:");
        System.out.println(card.getNumber());
        System.out.println("Your card PIN:");
        System.out.println(card.getPin());
    }

    private void logIntoAccount() {
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
                "2. Log out\n" +
                "0. Exit");
    }
}
