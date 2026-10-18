package com.spectromancer.port;

import java.util.EnumMap;

/** Mutable in-battle instance of a creature card. */
public final class CreatureState {
    public final CardDefinition card;
    public int attack;
    public int health;
    public final int maxHealth;
    public boolean canAttack;
    public int temporaryAttack;
    public final EnumMap<Element, Integer> growthContribution = new EnumMap<>(Element.class);
    public int spellDamageContribution;

    public CreatureState(CardDefinition card) {
        this.card = card;
        this.attack = card.attack;
        this.health = card.health;
        this.maxHealth = card.health;
        this.canAttack = card.id == 4; // Fire Drake attacks on the summoning turn in the original.
    }

    public String name() { return card.name; }
    public boolean isAlive() { return health > 0; }
}
