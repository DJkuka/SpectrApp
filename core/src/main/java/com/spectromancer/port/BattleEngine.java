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
    private final List<Integer> configuredDeckIds;
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
        this(catalog, seed, CardCatalog.starterIds());
    }

    /** Explicit deck list supports incremental rule tests and future deck-selection UI. */
    public BattleEngine(Map<Integer, CardDefinition> catalog, long seed, List<Integer> deckIds) {
        this.catalog = catalog;
        this.configuredDeckIds = new ArrayList<>(deckIds);
        List<CardDefinition> starter = new ArrayList<>();
        for (int id : configuredDeckIds) {
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

    /** Context-sensitive explanation shown in the long-press card panel. */
    public String currentEffectHint(CardDefinition card, int ownerSide) {
        if (card == null) return "Нет данных об этой карте.";
        PlayerState owner = players[ownerSide == AI ? AI : HUMAN];
        PlayerState enemy = opponentOf(owner);
        switch (card.id) {
            case 1: return "В начале вашего хода: 2 урона своим существам слева и справа от гоблина.";
            case 2: return "При призыве: 5 урона каждому существу соперника.";
            case 6: return "Сейчас: до " + (9 + owner.spellDamageBonus) + " урона каждому существу соперника.";
            case 9: return "Сейчас: до " + (18 + owner.spellDamageBonus) + " урона выбранной цели и " + (10 + owner.spellDamageBonus) + " остальным существам соперника.";
            case 10: return "При призыве: 3 урона герою и существам соперника; прирост Огня +1. Атака после оплаты: " + Math.max(0, owner.getPower(Element.FIRE) - card.cost) + ".";
            case 11: return "Сейчас: " + (8 + owner.getPower(Element.FIRE) + owner.spellDamageBonus) + " урона каждому существу на поле и герою соперника. Сила Огня считается до оплаты.";
            case 13: return "При применении увеличивает текущую силу Огня, Воздуха и Земли на 1.";
            case 14: return "В начале каждого вашего хода наносит 2 урона вашему герою.";
            case 16: return "Получает 0 урона от заклинаний и способностей существ; обычные атаки по линии наносят урон.";
            case 20: return "Сейчас: до " + (15 + owner.spellDamageBonus) + " урона каждому существу на обеих сторонах; все силы соперника уменьшаются на 1.";
            case 22: return "При призыве лечит героя на 10 и увеличивает прирост Воды на 1. Атака после оплаты: " + Math.max(0, owner.getPower(Element.WATER) - card.cost) + ".";
            case 25: return "Пока существо на поле, урон заклинаний владельца увеличен на 1.";
            case 27: return "Сейчас: до " + (6 + owner.spellDamageBonus) + " урона выбранному существу и " + (6 + owner.spellDamageBonus) + " герою соперника.";
            case 30: return "Сейчас: " + (5 + owner.getPower(Element.AIR) + owner.spellDamageBonus) + " урона герою соперника. Сила Воздуха считается до оплаты.";
            case 35: return "При призыве наносит 8 урона герою соперника и увеличивает прирост Воздуха на 1. Атака после оплаты: " + Math.max(0, owner.getPower(Element.AIR) - card.cost) + ".";
            case 37: return "В начале каждого вашего хода восстанавливает герою 3 здоровья.";
            case 38: return "Лечит героя на 8 и восстанавливает выбранному вашему существу до 8 здоровья.";
            case 40: return "Сейчас восстанавливает герою " + (2 * owner.getPower(Element.EARTH)) + " здоровья. Сила Земли считается до оплаты.";
            case 42: return "Сейчас нанесёт герою соперника " + (topTwoAttacks(owner) + owner.spellDamageBonus) + " урона — сумма атаки двух сильнейших ваших существ.";
            case 45: return "Сейчас: до " + (25 + owner.spellDamageBonus) + " урона каждому существу на обеих сторонах поля.";
            case 49: return "Сейчас: до " + (3 + owner.spellDamageBonus) + " урона каждому существу соперника; ваши существа лечатся на 3.";
            case 50: return "Сейчас: до " + (12 + owner.spellDamageBonus) + " урона каждому существу на поле и " + (3 + owner.spellDamageBonus) + " герою соперника.";
            case 51: return "При призыве наносит существу в противоположном слоте урон в половину его текущего здоровья (с округлением вверх).";
            case 53: return "Жертвует выбранным вашим существом и наносит всем существам соперника урон по его текущему здоровью, но не более 32.";
            case 54: return "Каждый раз, когда погибает существо соперника, особая сила владельца увеличивается на 1.";
            case 32: return "Сейчас: до " + (9 + owner.spellDamageBonus) + " урона каждому существу соперника и его герою.";
            case 34: return "Уничтожает выбранное существо соперника, а не наносит обычный числовой урон.";
            case 55: return "Уничтожает подходящие цели среди существ на обеих сторонах и лечит героя на 2 за каждое уничтоженное существо.";
            default:
                return card.description == null || card.description.trim().isEmpty()
                        ? "Точное описание эффекта ещё не перенесено из оригинального кода."
                        : card.description.trim();
        }
    }

    private int topTwoAttacks(PlayerState player) {
        int first = 0, second = 0;
        for (CreatureState c : player.board) {
            if (c == null) continue;
            int a = Math.max(0, c.attack);
            if (a >= first) { second = first; first = a; }
            else if (a > second) second = a;
        }
        return first + second;
    }

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
                && (!card.isCreature() || (card.id == 52
                    ? canUseSpecialSummon(card, human()) : human().freeSlot() >= 0));
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
            if (card.id == 52) {
                if (human().board[slot] == null) {
                    status = "Эмиссар Дорлака заменяет существо. Коснитесь занятого слота на своей стороне.";
                    return;
                }
                summon(human(), opponent(), card, slot);
                selectedCardId = null;
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
        BattleEngine fresh = new BattleEngine(catalog, seed, configuredDeckIds);
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
        // Emissary of Dorlak replaces an existing friendly creature, so a free slot is not required.
        return card != null && card.id == 52 && owner.hasAnyCreatures();
    }

    private void beginTurn(PlayerState player, boolean grow) {
        player.turnNumber++;
        if (grow) {
            player.growPowers();
            addLog(player.name + " получает прирост силы.");
        }
        refreshDynamicAttacks(player);
        for (int i = 0; i < player.board.length; i++) {
            CreatureState creature = player.board[i];
            if (creature == null) continue;
            creature.canAttack = true;
            switch (creature.card.id) {
                case 1: // Original prioritizes the left neighbour, otherwise the right; never hits both.
                    if (i > 0 && player.board[i - 1] != null) {
                        damageCreature(player, i - 1, 2, false, true, true);
                    } else if (i + 1 < player.board.length && player.board[i + 1] != null) {
                        damageCreature(player, i + 1, 2, false, true, true);
                    }
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
                int projectedAttack = isElemental(card.id)
                        ? Math.max(0, owner.getPower(card.resource()) - card.cost)
                        : card.attack;
                double score = 4.0 + projectedAttack * 0.95 + card.health * 0.16 - card.cost * 0.28;
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
            else {
                int projectedAttack = isElemental(card.id)
                        ? Math.max(0, owner.getPower(card.resource()) - card.cost)
                        : card.attack;
                s += Math.min(10, projectedAttack) - Math.min(10, blocking.health) * 0.12;
            }
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
            case 20: return owner.countCreatures() + enemy.countCreatures() > 0 || hasAnyPower(enemy);
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
        refreshDynamicAttacks(owner);
        if (card.id == 52) killCreature(owner, slot, false);
        CreatureState creature = new CreatureState(card);
        if (isElemental(card.id)) creature.attack = owner.getPower(card.resource());
        owner.board[slot] = creature;
        applyCreatureAura(owner, creature, 1);
        refreshDynamicAttacks(owner);
        addLog(owner.name + " призывает " + card.name + " в слот " + (slot + 1) + ".");
        switch (card.id) {
            case 2:
                damageAllCreatures(enemy, 5, false, true);
                break;
            case 3: modifyGrowth(owner, creature, Element.FIRE, 1); break;
            case 10:
                enemy.health -= 3;
                damageAllCreatures(enemy, 3, false, true);
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
                    damageCreature(enemy, slot, damage, false, true, true);
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
        // The original Flash rules resolve spellAttack() before charging its resource cost.
        // Capture power here so scaling effects are based on the same pre-payment values.
        int firePowerBeforeCast = owner.getPower(Element.FIRE);
        int airPowerBeforeCast = owner.getPower(Element.AIR);
        int earthPowerBeforeCast = owner.getPower(Element.EARTH);
        owner.spend(card);
        refreshDynamicAttacks(owner);
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
                refreshDynamicAttacks(enemy);
                break;
            case 27:
                damageCreature(enemy, targetSlot, 6 + spellDamage, true, true);
                enemy.health -= 6 + spellDamage;
                break;
            case 30: enemy.health -= 5 + airPowerBeforeCast + spellDamage; break;
            case 38:
                healPlayer(owner, 8);
                healCreature(owner, targetSlot, 8);
                break;
            case 40: healPlayer(owner, earthPowerBeforeCast * 2); break;
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
                int damage = 8 + firePowerBeforeCast + spellDamage;
                enemy.health -= damage;
                damageAllCreatures(owner, damage, true);
                damageAllCreatures(enemy, damage, true);
                break;
            case 13: // Meditation: the original increases Fire, Air and Earth, not every resource.
                owner.addPower(Element.FIRE, 1);
                owner.addPower(Element.AIR, 1);
                owner.addPower(Element.EARTH, 1);
                addLog(owner.name + " увеличивает силу Огня, Воздуха и Земли на 1.");
                break;
            case 32: // Chain Lightning: base version hits enemy creatures and the hero for 9.
                int chainDamage = 9 + spellDamage;
                damageAllCreatures(enemy, chainDamage, true);
                enemy.health -= chainDamage;
                break;
            case 55: // Drain Souls: destroy creatures that the original spell can target; heal 2 per kill.
                int destroyed = 0;
                for (int i = 0; i < enemy.board.length; i++) {
                    CreatureState c = enemy.board[i];
                    if (c != null && !isInstantKillSpellImmune(c.card.id)) {
                        killCreature(enemy, i, true);
                        destroyed++;
                    }
                }
                for (int i = 0; i < owner.board.length; i++) {
                    CreatureState c = owner.board[i];
                    if (c != null && !isInstantKillSpellImmune(c.card.id)) {
                        killCreature(owner, i, true);
                        destroyed++;
                    }
                }
                healPlayer(owner, destroyed * 2);
                addLog("Drain Souls уничтожает существ: " + destroyed + ", лечение: " + (destroyed * 2) + ".");
                break;
            default:
                addLog("Эффект карты " + card.name + " ещё не перенесён.");
                break;
        }
        if ("blood".equalsIgnoreCase(card.category)) {
            owner.health -= card.cost * 2;
            addLog(owner.name + " теряет " + (card.cost * 2) + " здоровья за заклинание Крови.");
        }
        refreshDynamicAttacks(owner);
        refreshDynamicAttacks(enemy);
        cleanBoards();
        checkGameOver();
    }

    private boolean isElemental(int cardId) {
        return cardId == 10 || cardId == 22 || cardId == 35 || cardId == 46;
    }

    /** ImmuneToSpell() list used by the original Drain Souls implementation. */
    private boolean isInstantKillSpellImmune(int cardId) {
        return cardId == 138 || cardId == 140 || cardId == 178
                || cardId == 181 || cardId == 184 || cardId == 187;
    }

    /** The four classic elementals use their owner's current matching power as attack. */
    private void refreshDynamicAttacks(PlayerState player) {
        if (player == null) return;
        for (CreatureState creature : player.board) {
            if (creature != null && isElemental(creature.card.id)) {
                creature.attack = player.getPower(creature.card.resource());
            }
        }
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
                damageAllCreatures(enemy, creature.attack, false, true);
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
        damageAllCreatures(player, amount, spellDamage, false);
    }

    private void damageAllCreatures(PlayerState player, int amount, boolean spellDamage, boolean abilityDamage) {
        for (int i = 0; i < player.board.length; i++) {
            damageCreature(player, i, amount, spellDamage, abilityDamage, true);
        }
        cleanBoards();
    }

    private void damageCreature(PlayerState player, int slot, int amount, boolean spellDamage, boolean invokeDeathEffects) {
        damageCreature(player, slot, amount, spellDamage, false, invokeDeathEffects);
    }

    private void damageCreature(PlayerState player, int slot, int amount, boolean spellDamage,
                                boolean abilityDamage, boolean invokeDeathEffects) {
        if (slot < 0 || slot >= player.board.length || player.board[slot] == null || amount <= 0) return;
        CreatureState creature = player.board[slot];
        // In the original, Ice Golem and Steel Golem resist both spell damage and creature abilities,
        // but still take ordinary lane-attack damage.
        if ((spellDamage || abilityDamage) && (creature.card.id == 16 || creature.card.id == 221)) return;
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

    private boolean hasAnyPower(PlayerState player) {
        for (Element element : Element.values()) if (player.getPower(element) > 0) return true;
        return false;
    }

    private void reduceAllPower(PlayerState player, int amount) {
        for (Element element : Element.values()) player.addPower(element, -amount);
        refreshDynamicAttacks(player);
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
