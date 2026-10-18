package com.spectromancer.port;

import java.util.Map;

/** Dependency-free regression smoke tests for the first battle-engine port. */
public final class BattleEngineSmokeTest {
    public static void main(String[] args) {
        Map<Integer, CardDefinition> all = CardCatalog.all();
        check(all.size() == 223, "expected all 223 imported card records");
        check(all.get(1).attack == 4 && all.get(1).health == 16, "Goblin Berserker stats imported from Card.as");
        check(all.get(6).isSpell() && all.get(6).cost == 6, "Flame Wave metadata imported from Card.as");
        check(all.get(27).requiresTarget(), "Call to Thunder is a target spell");
        check(all.get(49).resource() == Element.SPECIAL, "Death cards use the special resource bucket");

        testSummoningAndTurns(all);
        testTargetedSpell(all);
        testIceGolemImmunity(all);
        testDynamicElementalAttack(all);
        testExpandedSpellMechanics(all);
        testGoblinBerserkerTurnEffect(all);
        testEmissaryReplacement(all);
        testDirectDamageAndVictory(all);
        testSurrender(all);

        System.out.println("PASS: catalog=" + all.size() + ", summon, resource cost, target spell, Ice Golem immunity, dynamic elemental attack, live spell previews, expanded spells, Goblin Berserker priority, Emissary replacement, blood spell cost, surrender, direct damage, victory and turn progression verified");
    }

    private static void testSurrender(Map<Integer, CardDefinition> all) {
        BattleEngine engine = new BattleEngine(all, 909L);
        engine.surrender();
        check(engine.isGameOver(), "surrender ends the match");
        check(engine.winner() == BattleEngine.AI, "opponent wins after surrender");
        check(engine.status().contains("сдались"), "surrender status is visible");
    }

    private static void testSummoningAndTurns(Map<Integer, CardDefinition> all) {
        BattleEngine engine = new BattleEngine(all, 123456L);
        check(engine.human().deck.size() == 20, "starter deck has 20 cards");
        check(engine.human().health == 60 && engine.opponent().health == 60, "players start at 60 health");
        int fireBefore = engine.human().getPower(Element.FIRE);
        engine.tapCard(1);
        engine.tapSlot(BattleEngine.HUMAN, 0);
        check(engine.human().board[0] != null && engine.human().board[0].card.id == 1, "summon into an empty lane");
        check(engine.human().usedCards.contains(1), "played card becomes unavailable");
        check(engine.human().getPower(Element.FIRE) == fireBefore - 1, "summoning pays its resource cost");
        check(engine.selectedCardId() == null, "selection clears after a successful summon");

        int beforeTurn = engine.humanTurn();
        engine.endHumanTurn();
        check(engine.humanTurn() == beforeTurn + 1 || engine.isGameOver(), "turn advances after end turn");
        check(engine.logLines().size() <= 9, "battle log is bounded");
    }

    private static void testTargetedSpell(Map<Integer, CardDefinition> all) {
        BattleEngine engine = new BattleEngine(all, 777L);
        engine.human().power.put(Element.AIR, 10);
        engine.opponent().board[0] = new CreatureState(all.get(1));
        engine.tapCard(27);
        check(engine.selectedCardId() != null && engine.selectedCardId() == 27, "targeted spell enters selection mode");
        engine.tapSlot(BattleEngine.AI, 0);
        check(engine.human().usedCards.contains(27), "target spell gets consumed after cast");
        check(engine.human().getPower(Element.AIR) == 7, "target spell pays resource cost");
        check(engine.opponent().board[0] != null && engine.opponent().board[0].health == 10, "target spell damages the selected creature");
        check(engine.opponent().health == 54, "Call to Thunder also damages the enemy hero");
    }

    private static void testIceGolemImmunity(Map<Integer, CardDefinition> all) {
        BattleEngine spellBattle = new BattleEngine(all, 88L);
        spellBattle.human().power.put(Element.AIR, 3);
        spellBattle.opponent().board[0] = new CreatureState(all.get(16));
        spellBattle.tapCard(27);
        spellBattle.tapSlot(BattleEngine.AI, 0);
        check(spellBattle.opponent().board[0] != null && spellBattle.opponent().board[0].health == 12,
                "Ice Golem ignores spell damage");
        check(spellBattle.opponent().health == 54, "Call to Thunder still damages hero when target is immune");

        BattleEngine abilityBattle = new BattleEngine(all, 89L);
        abilityBattle.human().power.put(Element.FIRE, 2);
        abilityBattle.opponent().board[0] = new CreatureState(all.get(16));
        abilityBattle.opponent().board[1] = new CreatureState(all.get(1));
        abilityBattle.tapCard(2);
        abilityBattle.tapSlot(BattleEngine.HUMAN, 0);
        check(abilityBattle.opponent().board[0] != null && abilityBattle.opponent().board[0].health == 12,
                "Ice Golem ignores creature ability damage from Wall of Fire");
        check(abilityBattle.opponent().board[1] != null && abilityBattle.opponent().board[1].health == 11,
                "Wall of Fire still damages unprotected creatures");
    }

    private static void testDynamicElementalAttack(Map<Integer, CardDefinition> all) {
        BattleEngine engine = new BattleEngine(all, 90L);
        engine.human().power.put(Element.FIRE, 13);
        engine.tapCard(10);
        engine.tapSlot(BattleEngine.HUMAN, 0);
        check(engine.human().board[0] != null && engine.human().board[0].attack == 3,
                "Fire Elemental attack equals remaining Fire power after paying its cost");

        engine.tapCard(1);
        engine.tapSlot(BattleEngine.HUMAN, 1);
        check(engine.human().board[0].attack == 2,
                "Elemental attack refreshes when its owner spends matching power");
    }

    private static void testExpandedSpellMechanics(Map<Integer, CardDefinition> all) {
        BattleEngine meditation = new BattleEngine(all, 15L, java.util.Arrays.asList(13));
        meditation.human().power.put(Element.WATER, 1);
        meditation.human().power.put(Element.FIRE, 2);
        meditation.human().power.put(Element.AIR, 3);
        meditation.human().power.put(Element.EARTH, 4);
        meditation.tapCard(13);
        check(meditation.human().getPower(Element.FIRE) == 3
                && meditation.human().getPower(Element.AIR) == 4
                && meditation.human().getPower(Element.EARTH) == 5,
                "Meditation increases Fire, Air and Earth by one");
        check(meditation.human().getPower(Element.WATER) == 0,
                "Meditation does not increase Water and pays its Water cost");

        BattleEngine armageddon = new BattleEngine(all, 16L, java.util.Arrays.asList(11));
        armageddon.human().power.put(Element.FIRE, 11);
        armageddon.opponent().health = 60;
        armageddon.human().board[0] = new CreatureState(all.get(1));
        armageddon.opponent().board[0] = new CreatureState(all.get(1));
        armageddon.tapCard(11);
        check(armageddon.opponent().health == 41,
                "Armageddon damage uses Fire power before payment (8 + 11)");
        check(armageddon.human().board[0] == null && armageddon.opponent().board[0] == null,
                "Armageddon damages creatures on both sides");

        BattleEngine rejuvenation = new BattleEngine(all, 17L, java.util.Arrays.asList(40));
        rejuvenation.human().health = 20;
        rejuvenation.human().power.put(Element.EARTH, 6);
        rejuvenation.tapCard(40);
        check(rejuvenation.human().health == 32,
                "Rejuvenation heals twice the pre-payment Earth power");
        check(rejuvenation.human().getPower(Element.EARTH) == 2,
                "Rejuvenation still pays its four Earth cost");

        BattleEngine chain = new BattleEngine(all, 18L, java.util.Arrays.asList(32));
        chain.human().power.put(Element.AIR, 8);
        chain.opponent().board[0] = new CreatureState(all.get(1));
        chain.tapCard(32);
        check(chain.opponent().health == 51, "Chain Lightning deals 9 damage to the enemy hero");
        check(chain.opponent().board[0] != null && chain.opponent().board[0].health == 7,
                "Chain Lightning deals 9 damage to each enemy creature");

        BattleEngine tornado = new BattleEngine(all, 19L, java.util.Arrays.asList(34));
        tornado.human().power.put(Element.AIR, 10);
        tornado.opponent().board[0] = new CreatureState(all.get(16));
        tornado.tapCard(34);
        tornado.tapSlot(BattleEngine.AI, 0);
        check(tornado.opponent().board[0] == null,
                "Tornado destroys the target rather than dealing ordinary spell damage");

        BattleEngine drain = new BattleEngine(all, 20L, java.util.Arrays.asList(55));
        drain.human().power.put(Element.SPECIAL, 7);
        drain.human().health = 30;
        drain.human().board[0] = new CreatureState(all.get(37));
        drain.opponent().board[0] = new CreatureState(all.get(1));
        drain.opponent().board[1] = new CreatureState(all.get(16));
        drain.tapCard(55);
        check(drain.human().health == 36, "Drain Souls heals two per creature destroyed on either side");
        check(drain.human().board[0] == null && drain.opponent().board[0] == null
                && drain.opponent().board[1] == null, "Drain Souls destroys targetable creatures on both sides");

        BattleEngine bloodSpell = new BattleEngine(all, 21L, java.util.Arrays.asList(197));
        bloodSpell.human().power.put(Element.SPECIAL, 1);
        bloodSpell.human().health = 20;
        bloodSpell.tapCard(197);
        check(bloodSpell.human().health == 18, "Blood-category spell costs twice its resource cost in health");

        BattleEngine preview = new BattleEngine(all, 22L);
        preview.human().power.put(Element.AIR, 15);
        check(preview.currentEffectHint(all.get(30), BattleEngine.HUMAN).contains("20 урона"),
                "long-press preview reports actual Lightning Bolt damage before cost");
    }

    private static void testGoblinBerserkerTurnEffect(Map<Integer, CardDefinition> all) {
        BattleEngine engine = new BattleEngine(all, 23L, java.util.Arrays.asList(1));
        engine.human().board[0] = new CreatureState(all.get(37));
        engine.human().board[1] = new CreatureState(all.get(1));
        engine.human().board[2] = new CreatureState(all.get(37));
        engine.opponent().usedCards.add(1); // Keep the AI from adding unrelated creatures.
        engine.endHumanTurn(); // Advances through the AI turn and resolves the next human start-of-turn trigger.
        check(engine.human().board[0] != null && engine.human().board[0].health == 10,
                "Goblin Berserker damages the left neighbour when both neighbours exist");
        check(engine.human().board[2] != null && engine.human().board[2].health == 12,
                "Goblin Berserker does not damage both neighbours in the same turn");
    }

    private static void testEmissaryReplacement(Map<Integer, CardDefinition> all) {
        BattleEngine engine = new BattleEngine(all, 24L, java.util.Arrays.asList(52));
        engine.human().power.put(Element.SPECIAL, 4);
        for (int i = 0; i < PlayerState.BOARD_SIZE; i++) {
            engine.human().board[i] = new CreatureState(all.get(i == 3 ? 1 : 37));
        }
        engine.tapCard(52);
        check(engine.selectedCardId() != null && engine.selectedCardId() == 52,
                "Emissary can be selected when the board is full");
        engine.tapSlot(BattleEngine.HUMAN, 3);
        check(engine.human().board[3] != null && engine.human().board[3].card.id == 52,
                "Emissary replaces a creature in an occupied friendly slot");
        check(engine.human().usedCards.contains(52)
                && engine.human().getPower(Element.SPECIAL) == 0,
                "Emissary replacement consumes the card and pays its resource cost");
    }

    private static void testDirectDamageAndVictory(Map<Integer, CardDefinition> all) {
        BattleEngine engine = new BattleEngine(all, 42L);
        engine.human().power.put(Element.AIR, 15);
        engine.opponent().health = 3;
        engine.tapCard(30);
        check(engine.isGameOver(), "lethal direct-damage spell ends the match");
        check(engine.winner() == BattleEngine.HUMAN, "winning side is recorded correctly");
        check(engine.opponent().health == -17, "Lightning Bolt uses Air power before spending 6 resource points");
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
