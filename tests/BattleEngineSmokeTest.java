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
        testDirectDamageAndVictory(all);

        System.out.println("PASS: catalog=" + all.size() + ", summon, resource cost, target spell, direct damage, victory and turn progression verified");
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

    private static void testDirectDamageAndVictory(Map<Integer, CardDefinition> all) {
        BattleEngine engine = new BattleEngine(all, 42L);
        engine.human().power.put(Element.AIR, 15);
        engine.opponent().health = 3;
        engine.tapCard(30);
        check(engine.isGameOver(), "lethal direct-damage spell ends the match");
        check(engine.winner() == BattleEngine.HUMAN, "winning side is recorded correctly");
        check(engine.opponent().health <= 0, "direct damage reduces opponent health to zero or below");
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
