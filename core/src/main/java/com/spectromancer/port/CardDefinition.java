package com.spectromancer.port;

/** Immutable card metadata extracted from the user's ActionScript export. */
public final class CardDefinition {
    public final int id;
    public final String name;
    public final String className;
    public final String category;
    public final CardType type;
    public final int cost;
    public final int attack;
    public final int health;
    public final int bonus;
    public final String description;

    public CardDefinition(int id, String name, String className, String category,
                          CardType type, int cost, int attack, int health,
                          int bonus, String description) {
        this.id = id;
        this.name = name;
        this.className = className;
        this.category = category;
        this.type = type;
        this.cost = cost;
        this.attack = attack;
        this.health = health;
        this.bonus = bonus;
        this.description = description == null ? "" : description;
    }

    public Element resource() { return Element.fromCategory(category); }
    public boolean isCreature() { return type == CardType.CREATURE; }
    public boolean isSpell() { return type == CardType.SPELL; }
    public boolean requiresTarget() {
        return id == 9 || id == 27 || id == 34 || id == 38 || id == 53;
    }
}
