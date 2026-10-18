package com.spectromancer.port;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PlayerState {
    public static final int BOARD_SIZE = 6;
    public final String name;
    public final boolean human;
    public final String specialCategory;
    public int health = 60;
    public final EnumMap<Element, Integer> power = new EnumMap<>(Element.class);
    public final EnumMap<Element, Integer> growth = new EnumMap<>(Element.class);
    public final CreatureState[] board = new CreatureState[BOARD_SIZE];
    public final List<CardDefinition> deck = new ArrayList<>();
    public final Set<Integer> usedCards = new HashSet<>();
    public int spellDamageBonus;
    public int turnNumber;

    public PlayerState(String name, boolean human, String specialCategory, List<CardDefinition> deck, int powerTotal, long seed) {
        this.name = name;
        this.human = human;
        this.specialCategory = specialCategory;
        this.deck.addAll(deck);
        for (Element element : Element.values()) {
            power.put(element, 0);
            growth.put(element, 1);
        }
        initializePowersLikeOriginal(powerTotal, seed);
    }

    /** Port of Player.initPowersSingleDuel() for the standard single-duel setup. */
    private void initializePowersLikeOriginal(int powerTotal, long seed) {
        int special = powerTotal / 10;
        power.put(Element.SPECIAL, special);
        int remaining = powerTotal - special * 2;
        int initialElementPower = remaining / 5;
        for (Element e : new Element[]{Element.FIRE, Element.WATER, Element.AIR, Element.EARTH}) {
            power.put(e, initialElementPower);
        }
        remaining -= initialElementPower * 4;
        java.util.Random random = new java.util.Random(seed);
        Element[] elements = {Element.FIRE, Element.WATER, Element.AIR, Element.EARTH};
        while (remaining-- > 0) {
            Element e = elements[random.nextInt(elements.length)];
            power.put(e, power.get(e) + 1);
        }
    }

    public int freeSlot() {
        for (int i = 0; i < board.length; i++) if (board[i] == null) return i;
        return -1;
    }

    public boolean hasAnyCreatures() {
        for (CreatureState creature : board) if (creature != null) return true;
        return false;
    }

    public int countCreatures() {
        int count = 0;
        for (CreatureState creature : board) if (creature != null) count++;
        return count;
    }

    public int getPower(Element element) { return power.get(element); }
    public int getGrowth(Element element) { return growth.get(element); }
    public void addPower(Element element, int amount) {
        power.put(element, Math.max(0, power.get(element) + amount));
    }
    public void addGrowth(Element element, int amount) {
        growth.put(element, Math.max(0, growth.get(element) + amount));
    }
    public boolean canAfford(CardDefinition card) {
        return power.get(card.resource()) >= card.cost;
    }
    public void spend(CardDefinition card) {
        addPower(card.resource(), -card.cost);
        usedCards.add(card.id);
    }
    public CardDefinition findCard(int id) {
        for (CardDefinition card : deck) if (card.id == id) return card;
        return null;
    }
    public boolean isAvailable(CardDefinition card) {
        return card != null && !usedCards.contains(card.id);
    }
    public void growPowers() {
        for (Element element : Element.values()) addPower(element, growth.get(element));
    }
}
