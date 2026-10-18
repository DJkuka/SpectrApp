package com.spectromancer.port;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Artwork pass for the native Spectromancer port.
 * Uses original portrait-card art and card frames exported from the supplied SWF;
 * game rules remain in BattleEngine.
 */
public final class BattleScreen extends ScreenAdapter {
    private static final float W = 1280f, H = 720f;

    private static final float BOARD_X = 48f;
    private static final float BOARD_STEP = 207f;
    private static final float FRAME_W = 128f;
    private static final float CREATURE_H = 140f;
    private static final float ENEMY_Y = 490f;
    private static final float HUMAN_Y = 270f;

    // Show seven near-board-size cards at a time. Pages retain the complete deck while
    // giving the actual card artwork the room it has on the battlefield.
    private static final int HAND_PAGE_SIZE = 7;
    private static final float DECK_X = 48f;
    private static final float DECK_Y0 = 8f;
    private static final float DECK_W = 128f;
    private static final float DECK_H = 142f;
    private static final float DECK_STEP = 176f;
    private static final float PAGE_BTN_Y = 55f;
    private static final float PAGE_BTN_W = 35f;
    private static final float PAGE_BTN_H = 42f;
    private static final float PAGE_PREV_X = 5f;
    private static final float PAGE_NEXT_X = 1240f;

    private static final float END_X = 1083f, END_Y = 424f, END_W = 165f, END_H = 32f;
    private static final float MENU_X = 20f, MENU_Y = 688f, MENU_W = 102f, MENU_H = 24f;

    private final SpectromancerGame game;
    private final BattleEngine engine;
    private final FitViewport viewport;
    private final OrthographicCamera camera;
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = SystemFontFactory.create(15);
    private final BitmapFont smallFont = SystemFontFactory.create(11);
    private final BitmapFont tinyFont = SystemFontFactory.create(9);
    private final BitmapFont titleFont = SystemFontFactory.create(19);
    private final BitmapFont tooltipFont = SystemFontFactory.create(15);
    private final Vector2 point = new Vector2();
    private final Vector2 dragPoint = new Vector2();
    private final Map<Integer, Texture> cardArt = new HashMap<>();

    private final Texture background;
    private final Texture detailPanel;
    private final Texture parchment;
    private final Texture button;
    private final Texture playerPortrait;
    private final Texture opponentPortrait;
    private final Texture creatureBigFrame;
    private final Texture spellBigFrame;
    private final Texture creatureSmallFrame;
    private final Texture spellSmallFrame;
    private final Texture pixel;

    private Integer heldCardId;
    private boolean dragging;
    private boolean pressedEndTurn;
    private boolean pressedMenu;
    private boolean pressedPreviousPage;
    private boolean pressedNextPage;
    private int handPage;
    private boolean pointerDown;
    private boolean pointerMoved;
    private boolean longPressTriggered;
    private boolean dismissTooltipGesture;
    private float holdSeconds;
    private float touchStartX, touchStartY;
    private CardDefinition tooltipCard;
    private CreatureState tooltipCreature;
    private int tooltipSide = -1;
    private int tooltipSlot = -1;

    private final Color gold = Color.valueOf("e8ca81");
    private final Color parchmentText = Color.valueOf("f2e7d1");
    private final Color darkInk = Color.valueOf("3d2b20");
    private final Color muted = Color.valueOf("b6ad9b");
    private final Color green = Color.valueOf("83d88b");
    private final Color red = Color.valueOf("e19b84");

    public BattleScreen(SpectromancerGame game) {
        this.game = game;
        this.engine = new BattleEngine();
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(W, H, camera);
        this.background = texture("gfx/battle_bg.jpg");
        this.detailPanel = texture("gfx/detail_panel.png");
        this.parchment = texture("gfx/parchment.png");
        this.button = texture("gfx/button.png");
        this.playerPortrait = texture("gfx/portraits/player.jpg");
        this.opponentPortrait = texture("gfx/portraits/opponent.jpg");
        this.creatureBigFrame = texture("gfx/frames/creature_big.png");
        this.spellBigFrame = texture("gfx/frames/spell_big.png");
        this.creatureSmallFrame = texture("gfx/frames/creature_small.png");
        this.spellSmallFrame = texture("gfx/frames/spell_small.png");
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        this.pixel = new Texture(pixmap);
        pixmap.dispose();
        this.font.setColor(parchmentText);
        this.smallFont.setColor(parchmentText);
        this.titleFont.setColor(parchmentText);
    }

    private static Texture texture(String path) {
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    private Texture artFor(int cardId) {
        Texture cached = cardArt.get(cardId);
        if (cached != null) return cached;
        String name = String.format(Locale.ROOT, "gfx/cards/card_%03d.jpg", cardId);
        if (!Gdx.files.internal(name).exists()) return null;
        Texture loaded = texture(name);
        cardArt.put(cardId, loaded);
        return loaded;
    }

    @Override public void show() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override public boolean touchDown(int screenX, int screenY, int pointer, int buttonCode) {
                point.set(screenX, screenY);
                viewport.unproject(point);

                // A visible help card is dismissed by the next tap; that tap is not also
                // treated as a play/target action, which prevents accidental card casts.
                if (tooltipCard != null) {
                    clearTooltip();
                    dismissTooltipGesture = true;
                    pointerDown = false;
                    clearDrag();
                    return true;
                }
                dismissTooltipGesture = false;
                pointerDown = true;
                pointerMoved = false;
                longPressTriggered = false;
                holdSeconds = 0f;
                dragPoint.set(point);
                touchStartX = point.x;
                touchStartY = point.y;
                dragging = false;
                pressedEndTurn = inside(point.x, point.y, END_X, END_Y, END_W, END_H);
                pressedMenu = inside(point.x, point.y, MENU_X, MENU_Y - 15f, MENU_W, MENU_H + 15f);
                pressedPreviousPage = handPage > 0 && inside(point.x, point.y, PAGE_PREV_X, PAGE_BTN_Y, PAGE_BTN_W, PAGE_BTN_H);
                pressedNextPage = handPage < handPageCount() - 1 && inside(point.x, point.y, PAGE_NEXT_X, PAGE_BTN_Y, PAGE_BTN_W, PAGE_BTN_H);
                heldCardId = cardAt(point.x, point.y);
                // Board actions are deferred until touch-up so a long press can show help
                // without accidentally casting a selected targeted spell.
                return true;
            }

            @Override public boolean touchDragged(int screenX, int screenY, int pointer) {
                point.set(screenX, screenY);
                viewport.unproject(point);
                dragPoint.set(point);
                if (point.dst2(touchStartX, touchStartY) > 144f) {
                    pointerMoved = true;
                    if (heldCardId != null) dragging = true;
                }
                return true;
            }

            @Override public boolean touchUp(int screenX, int screenY, int pointer, int buttonCode) {
                point.set(screenX, screenY);
                viewport.unproject(point);
                pointerDown = false;
                if (dismissTooltipGesture) {
                    dismissTooltipGesture = false;
                    clearDrag();
                    return true;
                }
                if (pressedPreviousPage && inside(point.x, point.y, PAGE_PREV_X, PAGE_BTN_Y, PAGE_BTN_W, PAGE_BTN_H)) {
                    handPage = Math.max(0, handPage - 1);
                    clearDrag();
                    return true;
                }
                if (pressedNextPage && inside(point.x, point.y, PAGE_NEXT_X, PAGE_BTN_Y, PAGE_BTN_W, PAGE_BTN_H)) {
                    handPage = Math.min(handPageCount() - 1, handPage + 1);
                    clearDrag();
                    return true;
                }
                if (pressedMenu && inside(point.x, point.y, MENU_X, MENU_Y - 15f, MENU_W, MENU_H + 15f)) {
                    game.setScreen(new TitleScreen(game));
                    dispose();
                    return true;
                }
                if (pressedEndTurn && inside(point.x, point.y, END_X, END_Y, END_W, END_H)) {
                    if (engine.isGameOver()) engine.restart(); else engine.endHumanTurn();
                    clearDrag();
                    return true;
                }
                if (longPressTriggered) {
                    // The help panel remains visible after the finger is released.
                    clearDrag();
                    longPressTriggered = false;
                    return true;
                }
                if (heldCardId == null && pointerMoved
                        && insideY(touchStartY, DECK_Y0, DECK_Y0 + DECK_H)
                        && insideY(point.y, DECK_Y0, DECK_Y0 + DECK_H)
                        && Math.abs(point.x - touchStartX) > 90f) {
                    if (point.x < touchStartX) handPage = Math.min(handPageCount() - 1, handPage + 1);
                    else handPage = Math.max(0, handPage - 1);
                    clearDrag();
                    return true;
                }
                if (heldCardId != null) {
                    int cardId = heldCardId;
                    boolean wasDrag = dragging;
                    if (wasDrag) {
                        engine.tapCard(cardId);
                        tapSlotAt(point.x, point.y);
                    } else {
                        engine.tapCard(cardId);
                    }
                } else if (!pointerMoved) {
                    handleBoardTap(point.x, point.y);
                }
                clearDrag();
                return true;
            }
        });
    }

    private void clearDrag() {
        heldCardId = null;
        dragging = false;
        pressedEndTurn = false;
        pressedMenu = false;
        pressedPreviousPage = false;
        pressedNextPage = false;
        pointerDown = false;
        pointerMoved = false;
        holdSeconds = 0f;
    }

    private void clearTooltip() {
        tooltipCard = null;
        tooltipCreature = null;
        tooltipSide = -1;
        tooltipSlot = -1;
    }

    private void checkForLongPress(float delta) {
        if (!pointerDown || pointerMoved || dragging || longPressTriggered || tooltipCard != null) return;
        if (pressedEndTurn || pressedMenu || pressedPreviousPage || pressedNextPage) return;
        holdSeconds += Math.min(Math.max(delta, 0f), 0.1f);
        if (holdSeconds < 0.48f) return;

        if (heldCardId != null) {
            tooltipCard = engine.cardForHuman(heldCardId);
            tooltipCreature = null;
            tooltipSide = -1;
            tooltipSlot = -1;
        } else {
            int slot = slotAt(touchStartX);
            int side = -1;
            if (insideY(touchStartY, ENEMY_Y - 10f, ENEMY_Y + CREATURE_H + 10f)) side = BattleEngine.AI;
            else if (insideY(touchStartY, HUMAN_Y - 10f, HUMAN_Y + CREATURE_H + 10f)) side = BattleEngine.HUMAN;
            if (slot >= 0 && side >= 0) {
                CreatureState creature = engine.player(side).board[slot];
                if (creature != null) {
                    tooltipCreature = creature;
                    tooltipCard = creature.card;
                    tooltipSide = side;
                    tooltipSlot = slot;
                }
            }
        }
        if (tooltipCard != null) longPressTriggered = true;
        else holdSeconds = 0f;
    }

    private int handPageCount() {
        int size = engine.human().deck.size();
        return Math.max(1, (size + HAND_PAGE_SIZE - 1) / HAND_PAGE_SIZE);
    }

    private Integer cardAt(float x, float y) {
        if (!insideY(y, DECK_Y0, DECK_Y0 + DECK_H)) return null;
        if (x < DECK_X) return null;
        int col = (int) ((x - DECK_X) / DECK_STEP);
        if (col < 0 || col >= HAND_PAGE_SIZE) return null;
        float left = DECK_X + col * DECK_STEP;
        if (x > left + DECK_W) return null;
        int index = handPage * HAND_PAGE_SIZE + col;
        if (index >= engine.human().deck.size()) return null;
        return engine.human().deck.get(index).id;
    }

    private void handleBoardTap(float x, float y) {
        int slot = slotAt(x);
        if (slot < 0) return;
        if (insideY(y, HUMAN_Y - 10f, HUMAN_Y + CREATURE_H + 10f)) {
            engine.tapSlot(BattleEngine.HUMAN, slot);
        } else if (insideY(y, ENEMY_Y - 10f, ENEMY_Y + CREATURE_H + 10f)) {
            engine.tapSlot(BattleEngine.AI, slot);
        }
    }

    private void tapSlotAt(float x, float y) {
        int slot = slotAt(x);
        if (slot < 0) return;
        if (insideY(y, HUMAN_Y - 10f, HUMAN_Y + CREATURE_H + 10f)) {
            engine.tapSlot(BattleEngine.HUMAN, slot);
        } else if (insideY(y, ENEMY_Y - 10f, ENEMY_Y + CREATURE_H + 10f)) {
            engine.tapSlot(BattleEngine.AI, slot);
        }
    }

    private int slotAt(float x) {
        if (x < BOARD_X) return -1;
        int slot = (int) ((x - BOARD_X) / BOARD_STEP);
        if (slot < 0 || slot >= PlayerState.BOARD_SIZE) return -1;
        float left = BOARD_X + slot * BOARD_STEP;
        return x <= left + 160f ? slot : -1;
    }

    @Override public void render(float delta) {
        checkForLongPress(delta);
        Gdx.gl.glClearColor(0.055f, 0.06f, 0.07f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        shapes.setProjectionMatrix(camera.combined);

        batch.begin();
        batch.setColor(Color.WHITE);
        batch.draw(background, 0, 0, W, H);
        batch.end();

        drawPanelsAndSlots();
        drawBattleCards();
        drawPlayerSummary(engine.opponent(), false);
        drawPlayerSummary(engine.human(), true);
        drawStatusPanel();
        drawDeck();
        drawButtons();
        drawDraggingCard();
        if (tooltipCard != null) drawCardTooltip();
    }

    private void drawPanelsAndSlots() {
        Integer selectedId = engine.selectedCardId();
        CardDefinition selected = selectedId == null ? null : engine.cardForHuman(selectedId);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        // Dark translucent beds keep text and statistics readable over the source landscape.
        shapes.setColor(0.08f, 0.07f, 0.06f, 0.72f);
        shapes.rect(18, 652, 1244, 54);
        shapes.rect(18, 418, 1244, 48);
        shapes.rect(18, 166, 1244, 86);
        shapes.rect(18, 462, 1244, 27);

        for (int side = 0; side < 2; side++) {
            float y = side == BattleEngine.AI ? ENEMY_Y : HUMAN_Y;
            PlayerState player = side == BattleEngine.AI ? engine.opponent() : engine.human();
            for (int i = 0; i < PlayerState.BOARD_SIZE; i++) {
                float x = BOARD_X + i * BOARD_STEP - 8;
                boolean targetable = false;
                if (selected != null) {
                    if (selected.isCreature()) targetable = side == BattleEngine.HUMAN && player.board[i] == null;
                    else if (selected.requiresTarget()) {
                        boolean friendly = engine.targetBelongsToHuman(selected);
                        targetable = (friendly ? side == BattleEngine.HUMAN : side == BattleEngine.AI) && player.board[i] != null;
                    }
                }
                shapes.setColor(targetable ? new Color(0.75f, 0.55f, 0.18f, 0.22f) : new Color(0.08f, 0.09f, 0.09f, 0.36f));
                shapes.rect(x, y - 4, 158, 146);
                shapes.setColor(targetable ? gold : new Color(0.79f, 0.68f, 0.46f, 0.58f));
                shapes.rect(x, y - 4, 158, 1.4f);
                shapes.rect(x, y + 140.6f, 158, 1.4f);
                shapes.rect(x, y - 4, 1.4f, 146);
                shapes.rect(x + 156.6f, y - 4, 1.4f, 146);
            }
        }
        shapes.end();
    }

    private void drawBattleCards() {
        batch.begin();
        batch.setColor(Color.WHITE);
        drawSide(engine.opponent(), ENEMY_Y);
        drawSide(engine.human(), HUMAN_Y);
        batch.end();
    }

    private void drawSide(PlayerState player, float y) {
        for (int i = 0; i < PlayerState.BOARD_SIZE; i++) {
            float x = BOARD_X + i * BOARD_STEP + 7f;
            CreatureState creature = player.board[i];
            if (creature == null) {
                smallFont.setColor(new Color(0.89f, 0.82f, 0.66f, 0.72f));
                smallFont.draw(batch, "ПУСТО", x + 5f, y + 71f, FRAME_W - 12f, Align.center, false);
                continue;
            }
            CardDefinition card = creature.card;
            Texture frame = card.isCreature() ? creatureBigFrame : spellBigFrame;
            float fh = frame == creatureBigFrame ? CREATURE_H : FRAME_W * spellBigFrame.getHeight() / spellBigFrame.getWidth();
            Texture art = artFor(card.id);
            // Art first, original SWF frame on top. Transparent center preserves the illustration.
            if (art != null) {
                float artSize = 103f;
                batch.draw(art, x + 12.5f, y + 25f, artSize, artSize);
            }
            batch.draw(frame, x, y, FRAME_W, fh);
            smallFont.setColor(Color.valueOf("18264f"));
            smallFont.draw(batch, shorten(card.name, 18), x + 5f, y + fh - 8f, FRAME_W - 10f, Align.center, false);
            smallFont.setColor(Color.valueOf("263c91"));
            smallFont.draw(batch, String.valueOf(card.cost), x + FRAME_W - 22f, y + fh - 30f);
            if (card.isCreature()) {
                smallFont.setColor(Color.valueOf("8d3939"));
                smallFont.draw(batch, String.valueOf(creature.attack), x + 8f, y + 10f);
                smallFont.setColor(Color.valueOf("31723a"));
                smallFont.draw(batch, String.valueOf(creature.health), x + FRAME_W - 22f, y + 10f);
                if (!creature.canAttack) {
                    // Keep the SpriteBatch active: a small translucent overlay marks a creature
                    // that has already acted without switching renderers mid-batch.
                    batch.setColor(0.06f, 0.09f, 0.12f, 0.22f);
                    batch.draw(pixel, x + 11f, y + 24f, FRAME_W - 22f, FRAME_W - 22f);
                    batch.setColor(Color.WHITE);
                }
            }
        }
    }

    private void drawPlayerSummary(PlayerState player, boolean human) {
        float barY = human ? 418f : 652f;
        Texture portrait = human ? playerPortrait : opponentPortrait;
        batch.begin();
        batch.setColor(Color.WHITE);
        batch.draw(portrait, 28f, barY + 4f, human ? 38f : 44f, human ? 38f : 44f);
        batch.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(gold);
        shapes.rect(26f, barY + 2f, human ? 42f : 48f, human ? 42f : 48f);
        shapes.end();

        batch.begin();
        font.setColor(human ? Color.valueOf("ffe6ad") : Color.valueOf("f0ddbf"));
        font.draw(batch, (human ? "ВЫ" : "СОПЕРНИК") + " · " + player.health + " ♥", 78f, barY + 31f);
        smallFont.setColor(Color.valueOf("e6d2ad"));
        smallFont.draw(batch, human ? "ВАШ ХОД" : "МАГ КОМПЬЮТЕРА", 78f, barY + 15f);
        batch.end();

        float resourceStart = human ? 322f : 365f;
        float spacing = human ? 130f : 139f;
        Element[] order = {Element.FIRE, Element.WATER, Element.AIR, Element.EARTH, Element.SPECIAL};
        for (int i = 0; i < order.length; i++) {
            Element e = order[i];
            float x = resourceStart + i * spacing;
            Color c = categoryColor(categoryFor(e));
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(c.cpy().mul(0.54f, 0.54f, 0.54f, 1f));
            shapes.circle(x + 8f, barY + (human ? 23f : 27f), human ? 7f : 8f, 24);
            shapes.setColor(c);
            shapes.circle(x + 6f, barY + (human ? 25f : 29f), human ? 3.6f : 4f, 18);
            shapes.end();
            batch.begin();
            smallFont.setColor(c);
            String shortName = shortCategory(categoryFor(e));
            smallFont.draw(batch, shortName + " " + player.getPower(e) + "  +" + player.getGrowth(e), x + 20f, barY + (human ? 27f : 31f));
            batch.end();
        }
    }

    private void drawStatusPanel() {
        // Title and turn marker on the upper panel.
        batch.begin();
        titleFont.setColor(Color.valueOf("f1dcae"));
        titleFont.draw(batch, "SPECTROMANCER", 510f, 697f, 360f, Align.center, false);
        tinyFont.setColor(Color.valueOf("e7d5b4"));
        tinyFont.draw(batch, "ДОЛГОЕ НАЖАТИЕ — ОПИСАНИЕ КАРТЫ", 885f, 680f, 340f, Align.right, false);
        smallFont.setColor(Color.valueOf("e7d5b4"));
        smallFont.draw(batch, "ХОД " + engine.humanTurn(), 1155f, 697f, 76f, Align.right, false);
        batch.end();

        // This horizontal strip is cut from the original paper texture without distorting the page.
        batch.begin();
        batch.setColor(new Color(1f, 1f, 1f, 0.94f));
        batch.draw(detailPanel, 28f, 171f, 740f, 78f);
        batch.setColor(Color.WHITE);
        batch.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(new Color(0.73f, 0.56f, 0.31f, 0.95f));
        shapes.rect(28f, 171f, 740f, 78f);
        shapes.end();

        batch.begin();
        CardDefinition selected = engine.selectedCardId() == null ? null : engine.cardForHuman(engine.selectedCardId());
        if (selected != null) {
            titleFont.setColor(Color.valueOf("4d321c"));
            titleFont.draw(batch, selected.name, 48f, 231f, 700f, Align.left, false);
            smallFont.setColor(Color.valueOf("503922"));
            String detail = selected.description == null || selected.description.trim().isEmpty()
                    ? "" : selected.description;
            smallFont.draw(batch, detail, 48f, 211f, 700f, Align.left, true);
        } else {
            titleFont.setColor(Color.valueOf("4d321c"));
            titleFont.draw(batch, "ЖУРНАЛ БОЯ", 48f, 231f);
            smallFont.setColor(Color.valueOf("503922"));
            List<String> lines = engine.logLines();
            String line = lines.isEmpty() ? engine.status() : lines.get(lines.size() - 1);
            smallFont.draw(batch, line, 48f, 211f, 700f, Align.left, true);
        }
        smallFont.setColor(Color.valueOf("f4e7ca"));
        smallFont.draw(batch, engine.status(), 790f, 227f, 450f, Align.left, true);
        smallFont.setColor(Color.valueOf("e2d2b0"));
        List<String> logs = engine.logLines();
        if (!logs.isEmpty()) {
            String recent = logs.get(logs.size() - 1);
            smallFont.draw(batch, recent, 790f, 197f, 450f, Align.left, true);
        }
        batch.end();
    }

    private void drawDeck() {
        List<CardDefinition> deck = engine.human().deck;
        int startIndex = handPage * HAND_PAGE_SIZE;
        int endIndex = Math.min(deck.size(), startIndex + HAND_PAGE_SIZE);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int index = startIndex; index < endIndex; index++) {
            CardDefinition card = deck.get(index);
            int col = index - startIndex;
            float x = DECK_X + col * DECK_STEP;
            float y = DECK_Y0;
            boolean used = engine.human().usedCards.contains(card.id);
            boolean enabled = engine.isCardEnabledForHuman(card);
            boolean selected = card.id == (engine.selectedCardId() == null ? -1 : engine.selectedCardId());
            Color border = selected ? gold : categoryColor(card.category).cpy();
            float frameH = card.isCreature() ? CREATURE_H : DECK_W * spellBigFrame.getHeight() / spellBigFrame.getWidth();
            shapes.setColor(new Color(0.06f, 0.055f, 0.05f, used ? 0.62f : 0.91f));
            shapes.rect(x, y, DECK_W, frameH);
            shapes.setColor(border.cpy().mul(enabled || selected ? 1f : 0.48f, enabled || selected ? 1f : 0.48f, enabled || selected ? 1f : 0.48f, 1f));
            shapes.rect(x, y, 1.5f, frameH);
            shapes.rect(x + DECK_W - 1.5f, y, 1.5f, frameH);
            shapes.rect(x, y, DECK_W, 1.5f);
            shapes.rect(x, y + frameH - 1.5f, DECK_W, 1.5f);
        }
        shapes.end();

        batch.begin();
        batch.setColor(Color.WHITE);
        for (int index = startIndex; index < endIndex; index++) {
            CardDefinition card = deck.get(index);
            int col = index - startIndex;
            float x = DECK_X + col * DECK_STEP;
            float y = DECK_Y0;
            boolean used = engine.human().usedCards.contains(card.id);
            Texture art = artFor(card.id);
            Texture frame = card.isCreature() ? creatureBigFrame : spellBigFrame;
            float frameH = card.isCreature() ? CREATURE_H : DECK_W * spellBigFrame.getHeight() / spellBigFrame.getWidth();

            // The hand now uses the same large illustration and ornate frame scale as the board.
            if (art != null) {
                batch.setColor(used ? new Color(0.48f, 0.48f, 0.48f, 0.94f) : Color.WHITE);
                batch.draw(art, x + 12.5f, y + 25f, 103f, 103f);
            }
            batch.setColor(Color.WHITE);
            batch.draw(frame, x, y, DECK_W, frameH);

            smallFont.setColor(used ? muted : Color.valueOf("f1dfb8"));
            smallFont.draw(batch, shorten(card.name, 19), x + 5f, y + frameH - 8f, DECK_W - 10f, Align.center, false);
            smallFont.setColor(Color.valueOf("263c91"));
            smallFont.draw(batch, String.valueOf(card.cost), x + DECK_W - 22f, y + frameH - 30f);
            if (card.isCreature()) {
                smallFont.setColor(used ? muted : Color.valueOf("8d3939"));
                String attackText = (card.id == 10 || card.id == 22 || card.id == 35 || card.id == 46)
                        ? "?" : String.valueOf(card.attack);
                smallFont.draw(batch, attackText, x + 8f, y + 10f);
                smallFont.setColor(used ? muted : Color.valueOf("31723a"));
                smallFont.draw(batch, String.valueOf(card.health), x + DECK_W - 22f, y + 10f);
            } else {
                tinyFont.setColor(used ? muted : Color.valueOf("e4d8bc"));
                tinyFont.draw(batch, shortCategory(card.category), x + 8f, y + 10f);
            }
            if (used) {
                batch.setColor(0.12f, 0.12f, 0.12f, 0.28f);
                batch.draw(pixel, x + 2f, y + 2f, DECK_W - 4f, frameH - 4f);
                batch.setColor(Color.WHITE);
            }
        }
        batch.end();

        // Page controls keep all 20 cards reachable without shrinking the artwork again.
        batch.begin();
        batch.setColor(Color.WHITE);
        if (handPage > 0) {
            batch.draw(button, PAGE_PREV_X, PAGE_BTN_Y, PAGE_BTN_W, PAGE_BTN_H);
            titleFont.setColor(darkInk);
            titleFont.draw(batch, "‹", PAGE_PREV_X, PAGE_BTN_Y + 31f, PAGE_BTN_W, Align.center, false);
        }
        if (handPage < handPageCount() - 1) {
            batch.draw(button, PAGE_NEXT_X, PAGE_BTN_Y, PAGE_BTN_W, PAGE_BTN_H);
            titleFont.setColor(darkInk);
            titleFont.draw(batch, "›", PAGE_NEXT_X, PAGE_BTN_Y + 31f, PAGE_BTN_W, Align.center, false);
        }
        int visibleStart = startIndex + 1;
        int visibleEnd = endIndex;
        tinyFont.setColor(Color.valueOf("ecd9af"));
        tinyFont.draw(batch, "КАРТЫ " + visibleStart + "–" + visibleEnd + " ИЗ " + deck.size() + "   ·   СТРАНИЦА " + (handPage + 1) + "/" + handPageCount(),
                350f, 160f, 580f, Align.center, false);
        batch.end();
    }

    private void drawCardTooltip() {
        final float px = 285f, py = 92f, pw = 710f, ph = 536f;
        CardDefinition card = tooltipCard;
        if (card == null) return;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.73f);
        shapes.rect(0f, 0f, W, H);
        shapes.end();

        batch.begin();
        batch.setColor(Color.WHITE);
        batch.draw(parchment, px, py, pw, ph);
        batch.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(gold);
        shapes.rect(px, py, pw, ph);
        shapes.rect(px + 7f, py + 7f, pw - 14f, ph - 14f);
        shapes.end();

        Texture art = artFor(card.id);
        Texture frame = card.isCreature() ? creatureBigFrame : spellBigFrame;
        float pictureX = px + 22f, pictureY = py + 222f;
        if (art != null) {
            batch.begin();
            batch.setColor(Color.WHITE);
            batch.draw(art, pictureX + 12f, pictureY + 16f, 145f, 145f);
            batch.draw(frame, pictureX, pictureY, 170f, card.isCreature() ? 185f : 188f);
            batch.end();
        }

        batch.begin();
        titleFont.setColor(darkInk);
        titleFont.draw(batch, shorten(card.name, 34), px + 214f, py + ph - 39f, pw - 245f, Align.left, false);
        font.setColor(Color.valueOf("604326"));
        String kind = card.isCreature() ? "СУЩЕСТВО" : "ЗАКЛИНАНИЕ";
        font.draw(batch, categoryName(card.category) + " · " + kind, px + 214f, py + ph - 73f, pw - 245f, Align.left, false);
        smallFont.setColor(Color.valueOf("4e391f"));
        smallFont.draw(batch, "Стоимость: " + card.cost + "   ·   ресурс: " + categoryName(card.category), px + 214f, py + ph - 104f, pw - 245f, Align.left, false);
        if (card.isCreature()) {
            String stats;
            if (tooltipCreature != null) {
                stats = "Атака: " + tooltipCreature.attack + "   ·   Здоровье: " + tooltipCreature.health + "/" + tooltipCreature.maxHealth;
            } else if (card.id == 10 || card.id == 22 || card.id == 35 || card.id == 46) {
                int projectedAttack = Math.max(0, engine.human().getPower(card.resource()) - card.cost);
                stats = "Атака после оплаты: " + projectedAttack + "   ·   Здоровье: " + card.health;
            } else {
                stats = "Атака: " + card.attack + "   ·   Здоровье: " + card.health;
            }
            smallFont.draw(batch, stats, px + 214f, py + ph - 132f, pw - 245f, Align.left, true);
        }
        float descTop = py + ph - (card.isCreature() ? 197f : 169f);
        tooltipFont.setColor(Color.valueOf("3f2c1b"));
        String description = card.description == null ? "" : card.description.trim();
        if (description.isEmpty()) description = "Текст правила ещё не переведён в нативную версию.";
        smallFont.setColor(Color.valueOf("604326"));
        smallFont.draw(batch, "ПРАВИЛО КАРТЫ", px + 214f, descTop + 18f, pw - 245f, Align.left, false);
        tooltipFont.setColor(Color.valueOf("4a3523"));
        tooltipFont.draw(batch, description, px + 214f, descTop - 8f, pw - 245f, Align.left, true);
        smallFont.setColor(Color.valueOf("604326"));
        smallFont.draw(batch, "ЭФФЕКТ СЕЙЧАС", px + 214f, descTop - 108f, pw - 245f, Align.left, false);
        tooltipFont.setColor(Color.valueOf("3b2a1d"));
        String liveHint = engine.currentEffectHint(card, tooltipSide >= 0 ? tooltipSide : BattleEngine.HUMAN);
        tooltipFont.draw(batch, liveHint, px + 214f, descTop - 134f, pw - 245f, Align.left, true);

        smallFont.setColor(Color.valueOf("5b442b"));
        smallFont.draw(batch, "Долгое нажатие — описание и расчёт. Коснитесь экрана, чтобы закрыть.", px + 22f, py + 26f, pw - 44f, Align.center, false);
        batch.end();
    }

    private void drawButtons() {
        batch.begin();
        batch.setColor(Color.WHITE);
        batch.draw(button, END_X, END_Y, END_W, END_H);
        smallFont.setColor(Color.valueOf("4b3523"));
        smallFont.draw(batch, engine.isGameOver() ? "НОВЫЙ БОЙ" : "ЗАВЕРШИТЬ ХОД", END_X, END_Y + 21f, END_W, Align.center, false);
        batch.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(gold);
        shapes.rect(END_X, END_Y, END_W, END_H);
        shapes.end();

        batch.begin();
        smallFont.setColor(Color.valueOf("eadcbf"));
        smallFont.draw(batch, "‹ МЕНЮ", MENU_X, MENU_Y);
        batch.end();
    }

    private void drawDraggingCard() {
        if (heldCardId == null || !dragging) return;
        CardDefinition card = engine.cardForHuman(heldCardId);
        if (card == null) return;
        Texture art = artFor(card.id);
        Texture frame = card.isCreature() ? creatureBigFrame : spellBigFrame;
        float x = dragPoint.x - 64f;
        float y = dragPoint.y - 70f;
        batch.begin();
        batch.setColor(1f, 1f, 1f, 0.92f);
        if (art != null) batch.draw(art, x + 12.5f, y + 25f, 103f, 103f);
        batch.draw(frame, x, y, DECK_W, card.isCreature() ? CREATURE_H : DECK_W * spellBigFrame.getHeight() / spellBigFrame.getWidth());
        batch.end();
    }

    private Color categoryColor(String category) {
        if (category == null) return Color.valueOf("9b7cc3");
        switch (category.toLowerCase(Locale.ROOT)) {
            case "fire": return Color.valueOf("d96a4c");
            case "water": return Color.valueOf("55a4dc");
            case "air": return Color.valueOf("b8a0ed");
            case "earth": return Color.valueOf("75b86c");
            default: return Color.valueOf("a18acb");
        }
    }

    private String categoryFor(Element e) {
        switch (e) {
            case FIRE: return "Fire";
            case WATER: return "Water";
            case AIR: return "Air";
            case EARTH: return "Earth";
            default: return "Death";
        }
    }

    private String categoryName(String category) {
        if (category == null) return "Особая сила";
        switch (category.toLowerCase(Locale.ROOT)) {
            case "fire": return "Огонь";
            case "water": return "Вода";
            case "air": return "Воздух";
            case "earth": return "Земля";
            case "death": return "Смерть";
            case "holy": return "Свет";
            case "blood": return "Кровь";
            case "special": return "Особая сила";
            default: return category;
        }
    }

    private String shortCategory(String category) {
        if (category == null) return "СПЕЦ";
        switch (category.toLowerCase(Locale.ROOT)) {
            case "fire": return "ОГН";
            case "water": return "ВОД";
            case "air": return "ВОЗ";
            case "earth": return "ЗЕМ";
            default: return "СМР";
        }
    }

    private static String shorten(String text, int max) {
        if (text == null) return "";
        String value = text.replace('\n', ' ').trim();
        if (value.length() <= max) return value;
        return value.substring(0, Math.max(1, max - 1)).trim() + "…";
    }

    private static boolean inside(float x, float y, float rx, float ry, float rw, float rh) {
        return x >= rx && x <= rx + rw && y >= ry && y <= ry + rh;
    }

    private static boolean insideY(float y, float low, float high) { return y >= low && y <= high; }

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }

    @Override public void dispose() {
        background.dispose();
        detailPanel.dispose();
        parchment.dispose();
        button.dispose();
        playerPortrait.dispose();
        opponentPortrait.dispose();
        creatureBigFrame.dispose();
        spellBigFrame.dispose();
        creatureSmallFrame.dispose();
        spellSmallFrame.dispose();
        pixel.dispose();
        for (Texture t : cardArt.values()) t.dispose();
        cardArt.clear();
        shapes.dispose();
        batch.dispose();
        font.dispose();
        smallFont.dispose();
        tinyFont.dispose();
        titleFont.dispose();
        tooltipFont.dispose();
    }
}
