package com.spectromancer.port;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Offline battle rules for the first native port milestone.
 * Numeric card definitions are imported from Card.initializeCard(); only effects listed in
 * applySpell/applySummon/start-of-turn handlers have been ported so far.
 */
public final class BattleEngine {
    public static final int HUMAN = 0;
    public static final int AI = 1;
    private static final int MAX_LOG_LINES = 9;
    private static final int MAX_AI_PLAYS_PER_TURN = 20;

    private final Map<Integer, CardDefinition> catalog;
    private final List<String> log = new ArrayList<>();
    private final Random aiRandom;
    private final PlayerState[] players = new PlayerState[2];
    private int activePlayer = HUMAN;
    private int humanTurn = 1;
    private Integer selectedCardId;
    private boolean gameOver;
    private int winner = -1;
    private String status = "Выберите карту и коснитесь свободного слота для призыва.";

    public BattleEngine() {
        this(CardCatalog.all(), 0x5EC7L);
    }

    public BattleEngine(Map<Integer, CardDefinition> catalog, long seed) {
        this.catalog = catalog;
        List<CardDefinition> starter = new ArrayList<>();
        for (int id : CardCatalog.starterIds()) {
            CardDefinition card = catalog.get(id);
            if (card != null) starter.add(card);
        }
        aiRandom = new Random(seed ^ 0xA17E5L);
        players[HUMAN] = new PlayerState("Маг", true, "Death", starter, 16, seed + 11);
        players[AI] = new PlayerState("Соперник", false, "Death", starter, 16, seed + 37);
        addLog("Бой начался. Стартовые параметры силы рассчитаны по Player.initPowersSingleDuel().");
        addLog("В этой версии перенесена основа боя; часть особых способностей ещё не реализована.");
    }

    public PlayerState player(int index) { return players[index]; }
    public PlayerState human() { return players[HUMAN]; }
    public PlayerState opponent() { return players[AI]; }
    public int activePlayer() { return activePlayer; }
    public int humanTurn() { return humanTurn; }
    public boolean isGameOver() { return gameOver; }
    public int winner() { return winner; }
    public Integer selectedCardId() { return selectedCardId; }
    public String status() { return status; }
    public List<String> logLines() { return Collections.unmodifiableList(new ArrayList<>(log)); }
    public CardDefinition cardForHuman(int id) { return human().findCard(id); }

    public boolean isTargetSpell(CardDefinition card) {
        return card != null && card.isSpell() && card.requiresTarget();
    }

    public boolean targetBelongsToHuman(CardDefinition card) {
        // Nature's Ritual and Blood Ritual target a friendly creature in the original code.
        return card != null && (card.id == 38 || card.id == 53);
    }

    public boolean isCardEnabledForHuman(CardDefinition card) {
        return !gameOver && activePlayer == HUMAN && card != null
                && human().isAvailable(card) && human().canAfford(card)
                && (!card.isCreature() || human().freeSlot() >= 0 || canUseSpecialSummon(card, human()));
    }

    public void tapCard(int cardId) {
        if (gameOver || activePlayer != HUMAN) return;
        CardDefinition card = human().findCard(cardId);
        if (!isCardEnabledForHuman(card)) {
            status = card == null ? "Карта не найдена." : !human().isAvailable(card)
                    ? "Эта карта уже использована." : !human().canAfford(card)
                    ? "Недостаточно силы для этой карты." : "Нет свободного слота для существа.";
            selectedCardId = null;
            return;
        }
        if (card.isSpell() && !card.requiresTarget()) {
            selectedCardId = null;
            if (hasSpellValue(card, human())) castSpell(human(), opponent(), card, -1, HUMAN);
            else status = "Сейчас эта карта не даст полезного эффекта.";
            return;
        }
        selectedCardId = cardId;
        if (card.isCreature()) status = "Коснитесь свободного слота на своей стороне поля.";
        else if (targetBelongsToHuman(card)) status = "Коснитесь своего существа — цели заклинания.";
        else status = "Коснитесь существа соперника — цели заклинания.";
    }

    public void tapSlot(int side, int slot) {
        if (gameOver || activePlayer != HUMAN || slot < 0 || slot >= PlayerState.BOARD_SIZE) return;
        if (selectedCardId == null) {
            CreatureState c = players[side].board[slot];
            status = c == null ? "Выберите карту." : c.card.name + " — атака " + c.attack + ", здоровье " + c.health + ".";
            return;
        }
        CardDefinition card = human().findCard(selectedCardId);
        if (card == null || !human().isAvailable(card) || !human().canAfford(card)) {
            selectedCardId = null;
            status = "Выбранная карта больше недоступна.";
            return;
        }
        if (card.isCreature()) {
            if (side != HUMAN) {
                status = "Существа можно призывать только на свою сторону поля.";
                return;
            }
            if (human().board[slot] != null) {
                status = "Этот слот занят. Выберите свободный слот.";
                return;
            }
            summon(human(), opponent(), card, slot);
            selectedCardId = null;
            return;
        }
        if (!card.requiresTarget()) {
            castSpell(human(), opponent(), card, -1, HUMAN);
            selectedCardId = null;
            return;
        }
        int expectedSide = targetBelongsToHuman(card) ? HUMAN : AI;
        if (side != expectedSide) {
            status = expectedSide == HUMAN ? "Это заклинание требует цель на своей стороне." : "Это заклинание требует цель на стороне соперника.";
            return;
        }
        if (players[side].board[slot] == null) {
            status = "В выбранном слоте нет существа.";
            return;
        }
        castSpell(human(), opponent(), card, slot, HUMAN);
        selectedCardId = null;
    }

    public void endHumanTurn() {
        if (gameOver || activePlayer != HUMAN) return;
        selectedCardId = null;
        addLog("Вы завершаете ход.");
        resolveAttacks(human(), opponent());
        if (checkGameOver()) return;

        activePlayer = AI;
        beginTurn(opponent(), true);
        if (checkGameOver()) return;
        runAiTurn();
        if (checkGameOver()) return;
        addLog("Соперник завершает ход.");
        resolveAttacks(opponent(), human());
        if (checkGameOver()) return;

        activePlayer = HUMAN;
        humanTurn++;
        beginTurn(human(), true);
        if (!gameOver) status = "Ваш ход " + humanTurn + ". Выберите доступную карту.";
    }

    public void restart() {
        long seed = System.nanoTime();
        BattleEngine fresh = new BattleEngine(catalog, seed);
        for (int i = 0; i < players.length; i++) players[i] = fresh.players[i];
        log.clear();
        log.addAll(fresh.log);
        activePlayer = HUMAN;
        humanTurn = 1;
        selectedCardId = null;
        gameOver = false;
        winner = -1;
        status = "Новый бой. Выберите карту и слот.";
    }

    private boolean canUseSpecialSummon(CardDefinition card, PlayerState owner) {
        // The original permits only certain card-specific summons over an occupied board.
        return card.id == 52 && owner.getPower(Element.SPECIAL) >= card.cost;
    }

    private void beginTurn(PlayerState player, boolean grow) {
        player.turnNumber++;
        if (grow) {
            player.growPowers();
            addLog(player.name + " получает прирост силы.");
        }
        for (int i = 0; i < player.board.length; i++) {
            CreatureState creature = player.board[i];
            if (creature == null) continue;
            creature.canAttack = true;
            switch (creature.card.id) {
                case 1: // Goblin Berserker deals damage to neighbouring friendly creatures.
                    damageCreature(player, i - 1, 2, false, false);
                    damageCreature(player, i + 1, 2, false, false);
                    break;
                case 14: player.health -= 2; addLog("Sea Sprite наносит 2 урона своему владельцу."); break;
                case 29: opponentOf(player).health -= 4; addLog("Wall of Lightnings наносит 4 урона противнику."); break;
                case 37: healPlayer(player, 3); break;
                case 44: healCreature(player, i, 4); break;
                case 47:
                    healPlayer(player, 3);
                    for (int j = 0; j < player.board.length; j++) healCreature(player, j, 3);
                    break;
                default: break;
            }
            if (creature.card.id == 8 && creature.attack < creature.card.attack + 10) creature.attack += 1;
        }
        cleanBoards();
        checkGameOver();
    }

    private void runAiTurn() {
        status = "Соперник выбирает действие...";
        int plays = 0;
        while (!gameOver && plays < MAX_AI_PLAYS_PER_TURN) {
            AiChoice choice = chooseAiAction(opponent(), human());
            if (choice == null) break;
            if (choice.card.isCreature()) summon(opponent(), human(), choice.card, choice.slot);
            else castSpell(opponent(), human(), choice.card, choice.targetSlot, AI);
            plays++;
            if (checkGameOver()) return;
        }
        if (plays == 0) addLog("Соперник не нашёл выгодного доступного действия.");
        else addLog("Соперник сыграл карт: " + plays + ".");
        status = "Ход соперника завершён.";
    }

    private AiChoice chooseAiAction(PlayerState owner, PlayerState enemy) {
        AiChoice best = null;
        double bestScore = -1e9;
        for (CardDefinition card : owner.deck) {
            if (!owner.isAvailable(card) || !owner.canAfford(card)) continue;
            if (card.isCreature()) {
                int slot = chooseSummonSlot(owner, enemy, card);
                if (slot < 0) continue;
                double score = 4.0 + card.attack * 0.95 + card.health * 0.16 - card.cost * 0.28;
                if (card.id == 2) score += enemy.countCreatures() * 1.8;
                if (card.id == 10 || card.id == 22 || card.id == 35) score += 3.5;
                if (score > bestScore) { bestScore = score; best = new AiChoice(card, slot, -1); }
            } else {
                if (!hasSpellValue(card, owner)) continue;
                int target = card.requiresTarget() ? chooseSpellTarget(card, owner, enemy) : -1;
                if (card.requiresTarget() && target < 0) continue;
                double score = estimateSpellScore(card, owner, enemy, target);
                if (score > bestScore) { bestScore = score; best = new AiChoice(card, -1, target); }
            }
        }
        return bestScore >= 3.0 ? best : null;
    }

    private int chooseSummonSlot(PlayerState owner, PlayerState enemy, CardDefinition card) {
        if (card.id == 52) {
            for (int i = 0; i < owner.board.length; i++) if (owner.board[i] != null) return i;
            return -1;
        }
        int slot = -1;
        double score = -1e9;
        for (int i = 0; i < owner.board.length; i++) {
            if (owner.board[i] != null) continue;
            CreatureState blocking = enemy.board[i];
            double s = aiRandom.nextDouble();
            if (blocking == null) s += 2.0;
            else s += Math.min(10, card.attack) - Math.min(10, blocking.health) * 0.12;
            if (card.attack == 0 && blocking != null) s -= 1.0;
            if (s > score) { score = s; slot = i; }
        }
        return slot;
    }

    private int chooseSpellTarget(CardDefinition card, PlayerState owner, PlayerState enemy) {
        PlayerState targetPlayer = targetBelongsToHuman(card) ? owner : enemy;
        int target = -1;
        double score = -1e9;
        for (int i = 0; i < targetPlayer.board.length; i++) {
            CreatureState c = targetPlayer.board[i];
            if (c == null) continue;
            double candidate;
            if (card.id == 38) candidate = (c.maxHealth - c.health) + c.attack * 0.2;
            else if (card.id == 9) candidate = c.attack * 0.9 + (c.health <= 18 ? 10 : 0) + c.health * 0.1;
            else candidate = c.attack * 0.8 + c.health * 0.1;
            if (candidate > score) { score = candidate; target = i; }
        }
        if (card.id == 38 && score < 1) return -1;
        return target;
    }

    private double estimateSpellScore(CardDefinition card, PlayerState owner, PlayerState enemy, int targetSlot) {
        switch (card.id) {
            case 6: return enemy.countCreatures() == 0 ? -1 : 6 + enemy.countCreatures() * 2.2 - card.cost * 0.25;
            case 9: return 10 + enemy.countCreatures() * 1.3 - card.cost * 0.25;
            case 20: return (owner.countCreatures() + enemy.countCreatures()) * 1.4 + enemy.countCreatures() * 1.8 - card.cost * 0.3;
            case 27: return 9 + (enemy.health < 15 ? 4 : 0);
            case 30: return 7 + (enemy.health < 15 ? 5 : 0);
            case 38: return targetSlot < 0 ? -1 : 6 + (owner.board[targetSlot].maxHealth - owner.board[targetSlot].health) * 0.2;
            case 42: return owner.countCreatures() < 1 ? -1 : 7 + owner.countCreatures();
            case 45: return enemy.countCreatures() == 0 ? -1 : 8 + enemy.countCreatures() * 2.4;
            case 49: return enemy.countCreatures() == 0 ? -1 : 5 + enemy.countCreatures();
            case 50: return 4 + enemy.countCreatures() * 1.6 - owner.countCreatures() * 1.2;
            default: return -1;
        }
    }

    private boolean hasSpellValue(CardDefinition card, PlayerState owner) {
        PlayerState enemy = opponentOf(owner);
        if (card.requiresTarget()) {
            PlayerState target = targetBelongsToHuman(card) ? owner : enemy;
            if (card.id == 38) {
                for (CreatureState creature : target.board) if (creature != null && creature.health < creature.maxHealth) return true;
                return false;
            }
            for (CreatureState creature : target.board) if (creature != null) return true;
            return false;
        }
        switch (card.id) {
            case 6: case 45: case 49: return enemy.countCreatures() > 0;
            case 20: return owner.countCreatures() + enemy.countCreatures() > 0;
            case 42: return owner.countCreatures() > 0;
            case 50: return enemy.health > 0;
            default: return true;
        }
    }

    private void summon(PlayerState owner, PlayerState enemy, CardDefinition card, int slot) {
        if (owner == null || card == null || slot < 0 || slot >= 6 || !owner.isAvailable(card) || !owner.canAfford(card)) return;
        if (card.id != 52 && owner.board[slot] != null) return;
        if (card.id == 52 && owner.board[slot] == null) return;
        owner.spend(card);
        if (card.id == 52) killCreature(owner, slot, false);
        CreatureState creature = new CreatureState(card);
        owner.board[slot] = creature;
        applyCreatureAura(owner, creature, 1);
        addLog(owner.name + " призывает " + card.name + " в слот " + (slot + 1) + ".");
        switch (card.id) {
            case 2:
                damageAllCreatures(enemy, 5, false);
                break;
            case 3: modifyGrowth(owner, creature, Element.FIRE, 1); break;
            case 10:
                enemy.health -= 3;
                damageAllCreatures(enemy, 3, false);
                modifyGrowth(owner, creature, Element.FIRE, 1);
                break;
            case 17: modifyGrowth(owner, creature, Element.AIR, 1); break;
            case 22:
                healPlayer(owner, 10);
                modifyGrowth(owner, creature, Element.WATER, 1);
                break;
            case 23:
                for (Element e : Element.values()) modifyGrowth(owner, creature, e, 1);
                break;
            case 28: healPlayer(owner, Math.min(10, owner.getPower(Element.EARTH))); break;
            case 35:
                enemy.health -= 8;
                modifyGrowth(owner, creature, Element.AIR, 1);
                break;
            case 41: modifyGrowth(owner, creature, Element.EARTH, 2); break;
            case 46: modifyGrowth(owner, creature, Element.EARTH, 1); break;
            case 51:
                if (enemy.board[slot] != null) {
                    int damage = (enemy.board[slot].health + 1) / 2;
                    damageCreature(enemy, slot, damage, false, true);
                }
                break;
            case 54: break; // Death power trigger is handled when enemy creatures die.
            default: break;
        }
        if (owner.health <= 0 || enemy.health <= 0) checkGameOver();
    }

    private void castSpell(PlayerState owner, PlayerState enemy, CardDefinition card, int targetSlot, int ownerSide) {
        if (owner == null || card == null || !card.isSpell() || !owner.isAvailable(card) || !owner.canAfford(card)) return;
        if (card.requiresTarget() && (targetSlot < 0 || targetSlot >= 6)) return;
        owner.spend(card);
        int spellDamage = owner.spellDamageBonus;
        addLog(owner.name + " применяет " + card.name + ".");
        switch (card.id) {
            case 6: damageAllCreatures(enemy, 9 + spellDamage, true); break;
            case 9:
                if (enemy.board[targetSlot] != null) {
                    damageCreature(enemy, targetSlot, 18 + spellDamage, true, true);
                    for (int i = 0; i < enemy.board.length; i++) if (i != targetSlot) damageCreature(enemy, i, 10 + spellDamage, true, true);
                }
                break;
            case 20:
                damageAllCreatures(owner, 15 + spellDamage, true);
                damageAllCreatures(enemy, 15 + spellDamage, true);
                reduceAllPower(enemy, 1);
                break;
            case 27:
                damageCreature(enemy, targetSlot, 6 + spellDamage, true, true);
                enemy.health -= 6 + spellDamage;
                break;
            case 30: enemy.health -= 5 + owner.getPower(Element.AIR) + spellDamage; break;
            case 38:
                healPlayer(owner, 8);
                healCreature(owner, targetSlot, 8);
                break;
            case 40: healPlayer(owner, owner.getPower(Element.EARTH) * 2); break;
            case 42:
                List<Integer> attacks = new ArrayList<>();
                for (CreatureState c : owner.board) if (c != null) attacks.add(c.attack);
                attacks.sort(Comparator.reverseOrder());
                int total = 0;
                if (!attacks.isEmpty()) total += attacks.get(0);
                if (attacks.size() > 1) total += attacks.get(1);
                enemy.health -= total + spellDamage;
                break;
            case 45:
                damageAllCreatures(owner, 25 + spellDamage, true);
                damageAllCreatures(enemy, 25 + spellDamage, true);
                break;
            case 49:
                damageAllCreatures(enemy, 3 + spellDamage, true);
                for (int i = 0; i < owner.board.length; i++) healCreature(owner, i, 3);
                break;
            case 50:
                damageAllCreatures(owner, 12 + spellDamage, true);
                damageAllCreatures(enemy, 12 + spellDamage, true);
                enemy.health -= 3 + spellDamage;
                break;
            case 53:
                if (owner.board[targetSlot] != null) {
                    int damage = Math.min(32, owner.board[targetSlot].health);
                    killCreature(owner, targetSlot, false);
                    damageAllCreatures(enemy, damage + spellDamage, true);
                }
                break;
            case 34:
                if (enemy.board[targetSlot] != null) killCreature(enemy, targetSlot, true);
                break;
            case 11:
                int damage = 8 + owner.getPower(Element.FIRE) + spellDamage;
                enemy.health -= damage;
                damageAllCreatures(owner, damage, true);
                damageAllCreatures(enemy, damage, true);
                break;
            default:
                addLog("Эффект карты " + card.name + " ещё не перенесён.");
                break;
        }
        cleanBoards();
        checkGameOver();
    }

    private void applyCreatureAura(PlayerState owner, CreatureState creature, int sign) {
        if (creature.card.id == 25) {
            owner.spellDamageBonus += sign;
            creature.spellDamageContribution = sign;
        }
    }

    private void modifyGrowth(PlayerState owner, CreatureState creature, Element element, int amount) {
        owner.addGrowth(element, amount);
        creature.growthContribution.put(element, creature.growthContribution.containsKey(element)
                ? creature.growthContribution.get(element) + amount : amount);
    }

    private void resolveAttacks(PlayerState owner, PlayerState enemy) {
        for (int i = 0; i < owner.board.length; i++) {
            CreatureState creature = owner.board[i];
            if (creature == null || !creature.canAttack || creature.attack <= 0) continue;
            creature.canAttack = false;
            if (creature.card.id == 39) {
                enemy.health -= creature.attack;
                damageAllCreatures(enemy, creature.attack, false);
                addLog(creature.name() + " атакует всех существ соперника и самого соперника.");
                continue;
            }
            if (enemy.board[i] == null) {
                enemy.health -= creature.attack;
                addLog(creature.name() + " наносит " + creature.attack + " урона герою соперника.");
                if (creature.card.id == 56) owner.addPower(Element.SPECIAL, 2);
            } else {
                String targetName = enemy.board[i].name();
                damageCreature(enemy, i, creature.attack, false, true);
                addLog(creature.name() + " наносит " + creature.attack + " урона существу " + targetName + ".");
            }
            cleanBoards();
            if (checkGameOver()) return;
        }
    }

    private void damageAllCreatures(PlayerState player, int amount, boolean spellDamage) {
        for (int i = 0; i < player.board.length; i++) damageCreature(player, i, amount, spellDamage, true);
        cleanBoards();
    }

    private void damageCreature(PlayerState player, int slot, int amount, boolean spellDamage, boolean invokeDeathEffects) {
        if (slot < 0 || slot >= player.board.length || player.board[slot] == null || amount <= 0) return;
        CreatureState creature = player.board[slot];
        if (spellDamage && (creature.card.id == 16 || creature.card.id == 221)) return;
        if (creature.card.id == 19) amount = Math.max(0, amount - 5);
        creature.health -= amount;
        if (creature.health <= 0 && invokeDeathEffects) killCreature(player, slot, true);
    }

    private void killCreature(PlayerState owner, int slot, boolean countEnemyDeath) {
        if (slot < 0 || slot >= owner.board.length) return;
        CreatureState dead = owner.board[slot];
        if (dead == null) return;
        owner.board[slot] = null;
        for (Map.Entry<Element, Integer> entry : dead.growthContribution.entrySet()) owner.addGrowth(entry.getKey(), -entry.getValue());
        owner.spellDamageBonus = Math.max(0, owner.spellDamageBonus - dead.spellDamageContribution);
        if (countEnemyDeath) {
            PlayerState killer = opponentOf(owner);
            for (CreatureState c : killer.board) {
                if (c != null && c.card.id == 54) killer.addPower(Element.SPECIAL, 1);
            }
        }
        addLog(dead.name() + " погибает.");
    }

    private void cleanBoards() {
        for (PlayerState player : players) {
            for (int i = 0; i < player.board.length; i++) {
                if (player.board[i] != null && player.board[i].health <= 0) killCreature(player, i, true);
            }
        }
    }

    private void reduceAllPower(PlayerState player, int amount) {
        for (Element element : Element.values()) player.addPower(element, -amount);
    }

    private void healPlayer(PlayerState player, int amount) {
        if (amount <= 0) return;
        player.health = Math.min(60, player.health + amount);
    }

    private void healCreature(PlayerState player, int slot, int amount) {
        if (slot < 0 || slot >= player.board.length || player.board[slot] == null || amount <= 0) return;
        CreatureState c = player.board[slot];
        c.health = Math.min(c.maxHealth, c.health + amount);
    }

    private PlayerState opponentOf(PlayerState player) { return player == players[HUMAN] ? players[AI] : players[HUMAN]; }

    private boolean checkGameOver() {
        if (gameOver) return true;
        boolean humanDead = human().health <= 0;
        boolean aiDead = opponent().health <= 0;
        if (!humanDead && !aiDead) return false;
        gameOver = true;
        selectedCardId = null;
        if (humanDead && aiDead) {
            winner = -1;
            status = "Ничья!";
            addLog("Обе стороны потеряли все здоровье — ничья.");
        } else if (aiDead) {
            winner = HUMAN;
            status = "Победа! Соперник повержен.";
            addLog("Победа! Соперник повержен.");
        } else {
            winner = AI;
            status = "Поражение. Попробуйте другую тактику.";
            addLog("Вы проиграли этот бой.");
        }
        return true;
    }

    private void addLog(String text) {
        log.add(text);
        while (log.size() > MAX_LOG_LINES) log.remove(0);
    }

    private static final class AiChoice {
        final CardDefinition card;
        final int slot;
        final int targetSlot;
        AiChoice(CardDefinition card, int slot, int targetSlot) {
            this.card = card; this.slot = slot; this.targetSlot = targetSlot;
        }
    }
}
