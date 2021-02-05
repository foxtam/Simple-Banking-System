package net.foxtam;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Cards {
    private final List<Card> cards = new ArrayList<>();

    public Cards() {
    }

    public void add(Card card) {
        cards.add(card);
    }

    public Optional<Card> getCard(String number, String pin) {
        return cards.stream()
                .filter(card ->
                        card.getNumber().equals(number) && card.getPin().equals(pin))
                .findAny();
    }
}
