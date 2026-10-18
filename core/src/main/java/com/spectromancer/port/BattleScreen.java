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
 * Native 4:3 battlefield layout based on the supplied original Spectromancer screenshot.
 * The original hand grid, left command rail, mage panels, resource bars and two rows of
 * six battlefield lanes are recreated with LibGDX. Battle rules live in BattleEngine.
 */
public final class BattleScreen extends ScreenAdapter {
    private static final float W = 1024f, H = 768f;

    // Original six-lane board geometry.
    private static final float BOARD_X = 356f;
    private static final float BOARD_STEP = 103.3f;
    private static final float SLOT_W = 82f;
    private static final float SLOT_H = 98f;
    private static final float ENEMY_Y = 612f;
    private static final float HUMAN_Y = 490f;

    // Original 5 x 4 hand grid (20 cards visible at once).
    private static final int HAND_COLS = 5;
    private static final int HAND_ROWS = 4;
    private static final float HAND_X = 509f;
    private static final float HAND_Y = 11f;
    private static final float HAND_STEP_X = 102f;
    private static final float HAND_STEP_Y = 102f;
    private static final float HAND_CARD_W = 96f;
    private static final float HAND_CARD_H = 98f;

    // Player/resource strips run along the right of the original portrait column.
    private static final float SUMMARY_X = 202f;
    private static final float SUMMARY_Y_TOP = 731f;
    private static final float SUMMARY_Y_BOTTOM = 431f;
    private static final float SUMMARY_H = 36f;
    private static final float SUMMARY_W = 298f;
    private static final float RESOURCE_X = 502f;
    private static final float RESOURCE_W = (W - RESOURCE_X) / 5f;

    // Bottom-left original description and combat log panels.
    private static final float INFO_X = 0f, INFO_Y = 235f, INFO_W = 494f, INFO_H = 190f;
    private static final float LOG_X = 0f, LOG_Y = 42f, LOG_W = 494f, LOG_H = 180f;
    private static final float ADVICE_X = 132f, ADVICE_Y = 5f, ADVICE_W = 185f, ADVICE_H = 34f;

    private static final int OVERLAY_NONE = 0;
    private static final int OVERLAY_HELP = 1;
    private static final int OVERLAY_OPTIONS = 2;
    private static final int OVERLAY_SURRENDER = 3;

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
    private final Texture creatureBigFrame;
    private final Texture spellBigFrame;
    private final Texture creatureSmallFrame;
    private final Texture spellSmallFrame;
    private final Texture leftMenu;
    private final Texture mageTop;
    private final Texture mageSigil;
    private final Texture mageBottom;
    private final Texture infoFrame;
    private final Texture logFrame;
    private final Texture resourceFrame;
    private final Texture pixel;

    private Integer heldCardId;
    private boolean dragging;
    private boolean pointerDown;
    private boolean pointerMoved;
    private boolean longPressTriggered;
    private boolean dismissTooltipGesture;
    private boolean overlayDismissGesture;
    private boolean pressedAdvice;
    private int pressedSidebarAction;
    private float holdSeconds;
    private float touchStartX, touchStartY;
    private CardDefinition tooltipCard;
    private CreatureState tooltipCreature;
    private int tooltipSide = -1;
    private int tooltipSlot = -1;
    private int overlayMode = OVERLAY_NONE;

    private final Color gold = Color.valueOf("e8ca81");
    private final Color parchmentText = Color.valueOf("f2e7d1");
    private final Color darkInk = Color.valueOf("3d2b20");
    private final Color muted = Color.valueOf("b6ad9b");

    public BattleScreen(SpectromancerGame game) {
        this.game = game;
        this.engine = new BattleEngine();
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(W, H, camera);
        this.background = texture("gfx/battle_bg.jpg");
        this.detailPanel = texture("gfx/detail_panel.png");
        this.parchment = texture("gfx/parchment.png");
        this.button = texture("gfx/button.png");
        this.creatureBigFrame = texture("gfx/frames/creature_big.png");
        this.spellBigFrame = texture("gfx/frames/spell_big.png");
        this.creatureSmallFrame = texture("gfx/frames/creature_small.png");
        this.spellSmallFrame = texture("gfx/frames/spell_small.png");
        this.leftMenu = texture("gfx/ui/left_menu.png");
        this.mageTop = texture("gfx/ui/mage_top.png");
        this.mageSigil = texture("gfx/ui/mage_sigil.png");
        this.mageBottom = texture("gfx/ui/mage_bottom.png");
        this.infoFrame = texture("gfx/ui/info_frame.png");
        this.logFrame = texture("gfx/ui/log_frame.png");
        this.resourceFrame = texture("gfx/ui/resource_frame.png");
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

                // Modal/help/tooltip taps are consumed and never leak into the battlefield.
                if (overlayMode != OVERLAY_NONE) {
                    handleOverlayTap(point.x, point.y);
                    overlayDismissGesture = true;
                    pointerDown = false;
                    clearDrag();
                    return true;
                }
                if (tooltipCard != null) {
                    clearTooltip();
                    dismissTooltipGesture = true;
                    pointerDown = false;
                    clearDrag();
                    return true;
                }

                dismissTooltipGesture = false;
                overlayDismissGesture = false;
                pointerDown = true;
                pointerMoved = false;
                longPressTriggered = false;
                holdSeconds = 0f;
                dragPoint.set(point);
                touchStartX = point.x;
                touchStartY = point.y;
                dragging = false;
                pressedSidebarAction = sidebarActionAt(point.x, point.y);
                pressedAdvice = inside(point.x, point.y, ADVICE_X, ADVICE_Y, ADVICE_W, ADVICE_H);
                heldCardId = (pressedSidebarAction == 0 && !pressedAdvice) ? cardAt(point.x, point.y) : null;
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
                if (overlayDismissGesture) {
                    overlayDismissGesture = false;
                    clearDrag();
                    return true;
                }
                if (dismissTooltipGesture) {
                    dismissTooltipGesture = false;
                    clearDrag();
                    return true;
                }
                if (pressedSidebarAction != 0 && sidebarActionAt(point.x, point.y) == pressedSidebarAction) {
                    doSidebarAction(pressedSidebarAction);
                    clearDrag();
                    return true;
                }
                if (pressedAdvice && inside(point.x, point.y, ADVICE_X, ADVICE_Y, ADVICE_W, ADVICE_H)) {
                    overlayMode = OVERLAY_HELP;
                    clearDrag();
                    return true;
                }
                if (longPressTriggered) {
                    clearDrag();
                    longPressTriggered = false;
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

    private int sidebarActionAt(float x, float y) {
        // The original screenshot has four labeled buttons at far left and a circular
        // skip-turn control in the left half of the sigil between the mage portraits.
        if (x >= 81f && x <= 139f && y >= 563f && y <= 638f) return 2; // Skip Turn sigil
        if (x < 0f || x > 78f) return 0;
        if (y >= 685f) return 1;      // View Help
        if (y >= 598f && y < 685f) return 3; // Options
        if (y >= 512f && y < 598f) return 4; // Main Menu
        if (y >= 434f && y < 512f) return 5; // Surrender
        return 0;
    }

    private void doSidebarAction(int action) {
        switch (action) {
            case 1:
                overlayMode = OVERLAY_HELP;
                break;
            case 2:
                if (engine.isGameOver()) engine.restart(); else engine.endHumanTurn();
                break;
            case 3:
                overlayMode = OVERLAY_OPTIONS;
                break;
            case 4:
                game.setScreen(new TitleScreen(game));
                dispose();
                break;
            case 5:
                overlayMode = OVERLAY_SURRENDER;
                break;
            default:
                break;
        }
    }

    private void handleOverlayTap(float x, float y) {
        if (overlayMode == OVERLAY_SURRENDER) {
            if (inside(x, y, 536f, 261f, 154f, 46f)) engine.surrender();
            overlayMode = OVERLAY_NONE;
        } else {
            // Help and options use a simple tap-anywhere-to-close interaction for touch devices.
            overlayMode = OVERLAY_NONE;
        }
    }

    private void clearDrag() {
        heldCardId = null;
        dragging = false;
        pressedSidebarAction = 0;
        pressedAdvice = false;
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
        if (!pointerDown || pointerMoved || dragging || longPressTriggered || tooltipCard != null || overlayMode != OVERLAY_NONE) return;
        if (pressedSidebarAction != 0 || pressedAdvice) return;
        holdSeconds += Math.min(Math.max(delta, 0f), 0.1f);
        if (holdSeconds < 0.48f) return;

        if (heldCardId != null) {
            tooltipCard = engine.cardForHuman(heldCardId);
            tooltipCreature = null;
            tooltipSide = BattleEngine.HUMAN;
            tooltipSlot = -1;
        } else {
            int slot = slotAt(touchStartX);
            int side = -1;
            if (insideY(touchStartY, ENEMY_Y - 5f, ENEMY_Y + SLOT_H + 5f)) side = BattleEngine.AI;
            else if (insideY(touchStartY, HUMAN_Y - 5f, HUMAN_Y + SLOT_H + 5f)) side = BattleEngine.HUMAN;
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

    private Integer cardAt(float x, float y) {
        if (x < HAND_X || y < HAND_Y) return null;
        int col = (int) ((x - HAND_X) / HAND_STEP_X);
        int rowFromBottom = (int) ((y - HAND_Y) / HAND_STEP_Y);
        if (col < 0 || col >= HAND_COLS || rowFromBottom < 0 || rowFromBottom >= HAND_ROWS) return null;
        float left = HAND_X + col * HAND_STEP_X;
        float bottom = HAND_Y + rowFromBottom * HAND_STEP_Y;
        if (x > left + HAND_CARD_W || y > bottom + HAND_CARD_H) return null;
        int rowFromTop = HAND_ROWS - 1 - rowFromBottom;
        int index = rowFromTop * HAND_COLS + col;
        if (index >= engine.human().deck.size()) return null;
        return engine.human().deck.get(index).id;
    }

    private void handleBoardTap(float x, float y) {
        int slot = slotAt(x);
        if (slot < 0) return;
        if (insideY(y, HUMAN_Y - 4f, HUMAN_Y + SLOT_H + 4f)) {
            engine.tapSlot(BattleEngine.HUMAN, slot);
        } else if (insideY(y, ENEMY_Y - 4f, ENEMY_Y + SLOT_H + 4f)) {
            engine.tapSlot(BattleEngine.AI, slot);
        }
    }

    private void tapSlotAt(float x, float y) {
        int slot = slotAt(x);
        if (slot < 0) return;
        if (insideY(y, HUMAN_Y - 4f, HUMAN_Y + SLOT_H + 4f)) {
            engine.tapSlot(BattleEngine.HUMAN, slot);
        } else if (insideY(y, ENEMY_Y - 4f, ENEMY_Y + SLOT_H + 4f)) {
            engine.tapSlot(BattleEngine.AI, slot);
        }
    }

    private int slotAt(float x) {
        if (x < BOARD_X) return -1;
        int slot = (int) ((x - BOARD_X) / BOARD_STEP);
        if (slot < 0 || slot >= PlayerState.BOARD_SIZE) return -1;
        float left = BOARD_X + slot * BOARD_STEP;
        return x <= left + SLOT_W + 10f ? slot : -1;
    }

    @Override public void render(float delta) {
        checkForLongPress(delta);
        Gdx.gl.glClearColor(0.035f, 0.045f, 0.05f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        shapes.setProjectionMatrix(camera.combined);

        batch.begin();
        batch.setColor(Color.WHITE);
        batch.draw(background, 0f, 0f, W, H);
        batch.end();

        drawBoardSlots();
        drawBattleCards();
        drawPlayerPanels();
        drawSelectedCardPanel();
        drawCombatLogPanel();
        drawHandGrid();
        drawHelpButton();
        drawDraggingCard();
        if (tooltipCard != null) drawCardTooltip();
        if (overlayMode != OVERLAY_NONE) drawOverlay();
    }

    private void drawBoardSlots() {
        CardDefinition selected = engine.selectedCardId() == null ? null : engine.cardForHuman(engine.selectedCardId());
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int side = 0; side < 2; side++) {
            int sideId = side == 0 ? BattleEngine.AI : BattleEngine.HUMAN;
            PlayerState player = engine.player(sideId);
            float y = sideId == BattleEngine.AI ? ENEMY_Y : HUMAN_Y;
            for (int slot = 0; slot < PlayerState.BOARD_SIZE; slot++) {
                float x = BOARD_X + slot * BOARD_STEP;
                boolean targetable = false;
                if (selected != null) {
                    if (selected.isCreature()) targetable = sideId == BattleEngine.HUMAN
                            && (player.board[slot] == null || selected.id == 52);
                    else if (selected.requiresTarget()) {
                        boolean friendly = engine.targetBelongsToHuman(selected);
                        targetable = (friendly ? sideId == BattleEngine.HUMAN : sideId == BattleEngine.AI)
                                && player.board[slot] != null;
                    }
                }
                shapes.setColor(targetable ? new Color(0.90f, 0.69f, 0.22f, 0.22f) : new Color(0.09f, 0.10f, 0.08f, 0.07f));
                shapes.rect(x, y, SLOT_W, SLOT_H);
            }
        }
        shapes.end();

        batch.begin();
        batch.setColor(Color.WHITE);
        for (int side = 0; side < 2; side++) {
            float y = side == 0 ? ENEMY_Y : HUMAN_Y;
            for (int slot = 0; slot < PlayerState.BOARD_SIZE; slot++) {
                float x = BOARD_X + slot * BOARD_STEP;
                batch.draw(creatureSmallFrame, x, y, SLOT_W, SLOT_H);
            }
        }
        batch.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(new Color(0.86f, 0.74f, 0.47f, 0.72f));
        shapes.rect(348f, ENEMY_Y - 5f, 624f, SLOT_H + 10f);
        shapes.rect(348f, HUMAN_Y - 5f, 624f, SLOT_H + 10f);
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
            float x = BOARD_X + i * BOARD_STEP;
            CreatureState creature = player.board[i];
            if (creature == null) continue;
            CardDefinition card = creature.card;
            Texture art = artFor(card.id);
            Texture frame = card.isCreature() ? creatureSmallFrame : spellSmallFrame;
            if (art != null) {
                batch.setColor(Color.WHITE);
                batch.draw(art, x + 9f, y + 17f, 64f, 63f);
            }
            batch.setColor(Color.WHITE);
            batch.draw(frame, x, y, SLOT_W, SLOT_H);
            smallFont.setColor(Color.valueOf("293053"));
            smallFont.draw(batch, String.valueOf(card.cost), x + SLOT_W - 15f, y + SLOT_H - 10f);
            if (card.isCreature()) {
                smallFont.setColor(Color.valueOf("8d3939"));
                smallFont.draw(batch, String.valueOf(creature.attack), x + 5f, y + 10f);
                smallFont.setColor(Color.valueOf("31723a"));
                smallFont.draw(batch, String.valueOf(creature.health), x + SLOT_W - 18f, y + 10f);
                if (!creature.canAttack) {
                    batch.setColor(0.08f, 0.10f, 0.13f, 0.22f);
                    batch.draw(pixel, x + 6f, y + 14f, SLOT_W - 12f, SLOT_H - 24f);
                    batch.setColor(Color.WHITE);
                }
            }
        }
    }

    private void drawPlayerPanels() {
        // The portrait column is cropped from the reference screenshot so the frame and sigil
        // match the original rather than an approximation from generic avatar artwork.
        batch.begin();
        batch.setColor(Color.WHITE);
        batch.draw(leftMenu, 0f, 434f, 78f, 334f);
        batch.draw(mageTop, 78f, 638f, 124f, 130f);
        batch.draw(mageSigil, 78f, 564f, 124f, 75f);
        batch.draw(mageBottom, 78f, 435f, 124f, 129f);
        batch.end();

        drawLifeBar(engine.opponent(), false);
        drawLifeBar(engine.human(), true);
        drawResourceBar(engine.opponent(), false);
        drawResourceBar(engine.human(), true);

    }

    private void drawLifeBar(PlayerState player, boolean human) {
        float y = human ? SUMMARY_Y_BOTTOM : SUMMARY_Y_TOP;
        batch.begin();
        batch.setColor(new Color(1f, 1f, 1f, 0.94f));
        batch.draw(detailPanel, SUMMARY_X, y, SUMMARY_W, SUMMARY_H);
        batch.setColor(Color.WHITE);
        batch.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(new Color(0.39f, 0.28f, 0.17f, 0.85f));
        shapes.rect(SUMMARY_X, y, SUMMARY_W, SUMMARY_H);
        shapes.end();
        batch.begin();
        titleFont.setColor(Color.valueOf("332316"));
        titleFont.draw(batch, String.valueOf(player.health), SUMMARY_X + 8f, y + 25f);
        tinyFont.setColor(Color.valueOf("47321f"));
        tinyFont.draw(batch, "Life", SUMMARY_X + 9f, y + 10f);
        smallFont.setColor(Color.valueOf("3c2819"));
        smallFont.draw(batch, human ? "Cooler" : "Archmage", SUMMARY_X + 67f, y + 22f);
        batch.end();
    }

    private void drawResourceBar(PlayerState player, boolean human) {
        float y = human ? SUMMARY_Y_BOTTOM : SUMMARY_Y_TOP;
        Element[] order = {Element.FIRE, Element.WATER, Element.AIR, Element.EARTH, Element.SPECIAL};
        for (int i = 0; i < order.length; i++) {
            float x = RESOURCE_X + i * RESOURCE_W;
            Element element = order[i];
            String label = categoryFor(element);
            batch.begin();
            batch.setColor(new Color(1f, 1f, 1f, 0.95f));
            batch.draw(parchment, x, y, RESOURCE_W, SUMMARY_H);
            batch.setColor(Color.WHITE);
            batch.draw(resourceFrame, x, y, RESOURCE_W, SUMMARY_H);
            batch.end();
            batch.begin();
            smallFont.setColor(Color.valueOf("4b321e"));
            smallFont.draw(batch, label + ": " + player.getPower(element), x + 4f, y + 22f, RESOURCE_W - 8f, Align.center, false);
            batch.end();
        }
    }

    private void drawSelectedCardPanel() {
        drawPaperPanel(INFO_X, INFO_Y, INFO_W, INFO_H, infoFrame);
        CardDefinition selected = engine.selectedCardId() == null ? null : engine.cardForHuman(engine.selectedCardId());
        if (selected == null) {
            batch.begin();
            titleFont.setColor(Color.valueOf("4d321c"));
            titleFont.draw(batch, "CARD DETAILS", 20f, INFO_Y + INFO_H - 18f);
            smallFont.setColor(Color.valueOf("503922"));
            String status = engine.status();
            smallFont.draw(batch, status, 20f, INFO_Y + INFO_H - 42f, INFO_W - 38f, Align.left, true);
            List<String> logs = engine.logLines();
            if (!logs.isEmpty()) {
                String recent = logs.get(logs.size() - 1);
                tinyFont.setColor(Color.valueOf("5a432d"));
                tinyFont.draw(batch, recent, 20f, INFO_Y + 39f, INFO_W - 38f, Align.left, true);
            }
            batch.end();
            return;
        }

        Texture art = artFor(selected.id);
        Texture frame = selected.isCreature() ? creatureBigFrame : spellBigFrame;
        if (art != null) {
            batch.begin();
            batch.setColor(Color.WHITE);
            batch.draw(art, 23f, INFO_Y + 45f, 120f, 120f);
            batch.draw(frame, 10f, INFO_Y + 26f, 143f, 151f);
            batch.end();
        }
        batch.begin();
        titleFont.setColor(Color.valueOf("4d321c"));
        titleFont.draw(batch, selected.name, 170f, INFO_Y + INFO_H - 22f, 305f, Align.left, false);
        smallFont.setColor(Color.valueOf("255a25"));
        smallFont.draw(batch, categoryName(selected.category) + (selected.isCreature() ? " creature" : " spell") + ", cost " + selected.cost,
                171f, INFO_Y + INFO_H - 47f, 300f, Align.left, false);
        smallFont.setColor(Color.valueOf("503922"));
        String stats = selected.isCreature()
                ? "Attack " + selected.attack + "   Life " + selected.health
                : "School: " + categoryName(selected.category);
        smallFont.draw(batch, stats, 171f, INFO_Y + INFO_H - 68f, 300f, Align.left, false);
        String description = selected.description == null ? "" : selected.description.trim();
        if (description.isEmpty()) description = "Описание эффекта пока не перенесено.";
        tooltipFont.setColor(Color.valueOf("3d2b20"));
        tooltipFont.draw(batch, description, 171f, INFO_Y + INFO_H - 91f, 300f, Align.left, true);
        batch.end();
    }

    private void drawCombatLogPanel() {
        drawPaperPanel(LOG_X, LOG_Y, LOG_W, LOG_H, logFrame);
        List<String> logs = engine.logLines();
        batch.begin();
        tinyFont.setColor(Color.valueOf("6b4c2e"));
        tinyFont.draw(batch, "BATTLE LOG", 15f, LOG_Y + LOG_H - 14f);
        smallFont.setColor(Color.valueOf("3f3023"));
        int maxLines = 5;
        int first = Math.max(0, logs.size() - maxLines);
        float y = LOG_Y + LOG_H - 36f;
        for (int i = first; i < logs.size(); i++) {
            String line = logs.get(i);
            smallFont.draw(batch, line, 15f, y, LOG_W - 28f, Align.left, true);
            y -= 24f;
        }
        tinyFont.setColor(Color.valueOf("6b4c2e"));
        String status = engine.status();
        smallFont.draw(batch, status, 15f, LOG_Y + 20f, LOG_W - 28f, Align.left, true);
        batch.end();
    }

    private void drawHandGrid() {
        // Subdued paper backing makes the four card rows readable but leaves the original sky visible.
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(new Color(0.54f, 0.54f, 0.46f, 0.52f));
        shapes.rect(499f, 4f, 525f, 420f);
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(new Color(0.64f, 0.48f, 0.28f, 0.75f));
        shapes.rect(499f, 4f, 525f, 420f);
        shapes.end();

        List<CardDefinition> deck = engine.human().deck;
        batch.begin();
        for (int index = 0; index < deck.size() && index < HAND_COLS * HAND_ROWS; index++) {
            CardDefinition card = deck.get(index);
            int col = index % HAND_COLS;
            int rowFromTop = index / HAND_COLS;
            float x = HAND_X + col * HAND_STEP_X;
            float y = HAND_Y + (HAND_ROWS - 1 - rowFromTop) * HAND_STEP_Y;
            boolean used = engine.human().usedCards.contains(card.id);
            boolean enabled = engine.isCardEnabledForHuman(card);
            boolean selected = card.id == (engine.selectedCardId() == null ? -1 : engine.selectedCardId());
            Texture art = artFor(card.id);
            Texture frame = card.isCreature() ? creatureSmallFrame : spellSmallFrame;
            if (art != null) {
                batch.setColor(used ? new Color(0.39f, 0.39f, 0.39f, 0.95f) : Color.WHITE);
                batch.draw(art, x + 13f, y + 17f, 70f, 66f);
            }
            batch.setColor(Color.WHITE);
            batch.draw(frame, x, y, HAND_CARD_W, HAND_CARD_H);

            smallFont.setColor(used ? muted : Color.valueOf("293053"));
            smallFont.draw(batch, String.valueOf(card.cost), x + HAND_CARD_W - 17f, y + HAND_CARD_H - 8f);
            if (card.isCreature()) {
                smallFont.setColor(used ? muted : Color.valueOf("8d3939"));
                String atk = (card.id == 10 || card.id == 22 || card.id == 35 || card.id == 46)
                        ? "?" : String.valueOf(card.attack);
                smallFont.draw(batch, atk, x + 6f, y + 9f);
                smallFont.setColor(used ? muted : Color.valueOf("31723a"));
                smallFont.draw(batch, String.valueOf(card.health), x + HAND_CARD_W - 20f, y + 9f);
            }
            if (used || !enabled) {
                float alpha = used ? 0.23f : 0.12f;
                batch.setColor(0.09f, 0.08f, 0.07f, alpha);
                batch.draw(pixel, x + 2f, y + 2f, HAND_CARD_W - 4f, HAND_CARD_H - 4f);
                batch.setColor(Color.WHITE);
            }
            if (selected) {
                // Gold selection border is drawn later with ShapeRenderer to keep it crisp.
            }
        }
        batch.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        List<CardDefinition> drawnDeck = engine.human().deck;
        for (int index = 0; index < drawnDeck.size() && index < HAND_COLS * HAND_ROWS; index++) {
            CardDefinition card = drawnDeck.get(index);
            if (card.id != (engine.selectedCardId() == null ? -1 : engine.selectedCardId())) continue;
            int col = index % HAND_COLS;
            int row = index / HAND_COLS;
            float x = HAND_X + col * HAND_STEP_X;
            float y = HAND_Y + (HAND_ROWS - 1 - row) * HAND_STEP_Y;
            shapes.setColor(gold);
            shapes.rect(x - 1f, y - 1f, HAND_CARD_W + 2f, HAND_CARD_H + 2f);
        }
        shapes.end();
    }

    private void drawHelpButton() {
        batch.begin();
        batch.setColor(Color.WHITE);
        batch.draw(button, ADVICE_X, ADVICE_Y, ADVICE_W, ADVICE_H);
        smallFont.setColor(Color.valueOf("4b3523"));
        smallFont.draw(batch, "GET ADVICE", ADVICE_X, ADVICE_Y + 23f, ADVICE_W, Align.center, false);
        batch.end();
    }

    private void drawPaperPanel(float x, float y, float w, float h) {
        drawPaperPanel(x, y, w, h, null);
    }

    private void drawPaperPanel(float x, float y, float w, float h, Texture ornamentalFrame) {
        batch.begin();
        batch.setColor(new Color(1f, 1f, 1f, 0.93f));
        batch.draw(parchment, x, y, w, h);
        batch.setColor(Color.WHITE);
        if (ornamentalFrame != null) batch.draw(ornamentalFrame, x, y, w, h);
        batch.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(new Color(0.48f, 0.34f, 0.20f, 0.92f));
        if (ornamentalFrame == null) shapes.rect(x, y, w, h);
        shapes.setColor(new Color(0.83f, 0.67f, 0.40f, 0.72f));
        shapes.rect(x + 3f, y + 3f, w - 6f, h - 6f);
        shapes.end();
    }

    private void drawDraggingCard() {
        if (heldCardId == null || !dragging) return;
        CardDefinition card = engine.cardForHuman(heldCardId);
        if (card == null) return;
        Texture art = artFor(card.id);
        Texture frame = card.isCreature() ? creatureSmallFrame : spellSmallFrame;
        float x = dragPoint.x - HAND_CARD_W / 2f;
        float y = dragPoint.y - HAND_CARD_H / 2f;
        batch.begin();
        batch.setColor(1f, 1f, 1f, 0.94f);
        if (art != null) batch.draw(art, x + 13f, y + 17f, 70f, 66f);
        batch.draw(frame, x, y, HAND_CARD_W, HAND_CARD_H);
        batch.end();
    }

    private void drawCardTooltip() {
        final float px = 166f, py = 96f, pw = 692f, ph = 576f;
        CardDefinition card = tooltipCard;
        if (card == null) return;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.76f);
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
        if (art != null) {
            batch.begin();
            batch.setColor(Color.WHITE);
            batch.draw(art, px + 22f, py + 319f, 170f, 170f);
            batch.draw(frame, px + 10f, py + 304f, 194f, 200f);
            batch.end();
        }
        batch.begin();
        titleFont.setColor(darkInk);
        titleFont.draw(batch, card.name, px + 225f, py + ph - 39f, pw - 250f, Align.left, false);
        font.setColor(Color.valueOf("604326"));
        font.draw(batch, categoryName(card.category) + " · " + (card.isCreature() ? "СУЩЕСТВО" : "ЗАКЛИНАНИЕ"),
                px + 225f, py + ph - 75f, pw - 250f, Align.left, false);
        smallFont.setColor(Color.valueOf("4e391f"));
        smallFont.draw(batch, "Стоимость: " + card.cost + "   ·   ресурс: " + categoryName(card.category),
                px + 225f, py + ph - 108f, pw - 250f, Align.left, false);
        if (card.isCreature()) {
            String stats;
            if (tooltipCreature != null) {
                stats = "Атака: " + tooltipCreature.attack + "   ·   Здоровье: "
                        + tooltipCreature.health + "/" + tooltipCreature.maxHealth;
            } else if (card.id == 10 || card.id == 22 || card.id == 35 || card.id == 46) {
                int projectedAttack = Math.max(0, engine.human().getPower(card.resource()) - card.cost);
                stats = "Атака после оплаты: " + projectedAttack + "   ·   Здоровье: " + card.health;
            } else {
                stats = "Атака: " + card.attack + "   ·   Здоровье: " + card.health;
            }
            smallFont.draw(batch, stats, px + 225f, py + ph - 137f, pw - 250f, Align.left, true);
        }
        float descTop = py + ph - (card.isCreature() ? 198f : 171f);
        smallFont.setColor(Color.valueOf("604326"));
        smallFont.draw(batch, "ПРАВИЛО КАРТЫ", px + 225f, descTop + 18f, pw - 250f, Align.left, false);
        tooltipFont.setColor(Color.valueOf("4a3523"));
        String description = card.description == null ? "" : card.description.trim();
        if (description.isEmpty()) description = "Текст правила ещё не перенесён в нативную версию.";
        tooltipFont.draw(batch, description, px + 225f, descTop - 8f, pw - 250f, Align.left, true);
        smallFont.setColor(Color.valueOf("604326"));
        smallFont.draw(batch, "ЭФФЕКТ СЕЙЧАС", px + 225f, descTop - 111f, pw - 250f, Align.left, false);
        tooltipFont.setColor(Color.valueOf("3b2a1d"));
        String liveHint = engine.currentEffectHint(card, tooltipSide >= 0 ? tooltipSide : BattleEngine.HUMAN);
        tooltipFont.draw(batch, liveHint, px + 225f, descTop - 136f, pw - 250f, Align.left, true);
        smallFont.setColor(Color.valueOf("5b442b"));
        smallFont.draw(batch, "Долгое нажатие — описание. Коснитесь экрана, чтобы закрыть.",
                px + 22f, py + 25f, pw - 44f, Align.center, false);
        batch.end();
    }

    private void drawOverlay() {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.72f);
        shapes.rect(0f, 0f, W, H);
        shapes.end();
        float x = 210f, y = 218f, w = 604f, h = 332f;
        drawPaperPanel(x, y, w, h);
        batch.begin();
        titleFont.setColor(darkInk);
        if (overlayMode == OVERLAY_HELP) {
            titleFont.draw(batch, "VIEW HELP", x + 28f, y + h - 39f);
            smallFont.setColor(Color.valueOf("4b3523"));
            smallFont.draw(batch, "• Tap a card to select it or cast a non-targeted spell.", x + 30f, y + h - 84f, w - 60f, Align.left, false);
            smallFont.draw(batch, "• Drag a creature to an empty slot. Drag a targeted spell to a valid creature.", x + 30f, y + h - 116f, w - 60f, Align.left, true);
            smallFont.draw(batch, "• Hold a card or creature for about half a second to inspect its rules and live effect.", x + 30f, y + h - 157f, w - 60f, Align.left, true);
            smallFont.draw(batch, "• Use Skip Turn on the left to resolve attacks and let the opponent act.", x + 30f, y + h - 198f, w - 60f, Align.left, true);
            tinyFont.setColor(Color.valueOf("6b4c2e"));
            tinyFont.draw(batch, "Tap anywhere to close", x + 20f, y + 22f, w - 40f, Align.center, false);
        } else if (overlayMode == OVERLAY_OPTIONS) {
            titleFont.draw(batch, "OPTIONS", x + 28f, y + h - 39f);
            smallFont.setColor(Color.valueOf("4b3523"));
            smallFont.draw(batch, "This native prototype currently runs offline and has no sound or network services.",
                    x + 30f, y + h - 91f, w - 60f, Align.left, true);
            smallFont.draw(batch, "The original save system, full animation and sound settings are not ported yet.",
                    x + 30f, y + h - 133f, w - 60f, Align.left, true);
            tinyFont.setColor(Color.valueOf("6b4c2e"));
            tinyFont.draw(batch, "Tap anywhere to close", x + 20f, y + 22f, w - 40f, Align.center, false);
        } else if (overlayMode == OVERLAY_SURRENDER) {
            titleFont.draw(batch, "SURRENDER?", x + 28f, y + h - 39f);
            smallFont.setColor(Color.valueOf("4b3523"));
            smallFont.draw(batch, "Вы уверены, что хотите сдаться?", x + 30f, y + h - 91f, w - 60f, Align.left, false);
            batch.setColor(Color.WHITE);
            batch.draw(button, 536f, 261f, 154f, 46f);
            smallFont.setColor(Color.valueOf("4b3523"));
            smallFont.draw(batch, "SURRENDER", 536f, 289f, 154f, Align.center, false);
            batch.draw(button, 338f, 261f, 154f, 46f);
            smallFont.draw(batch, "CANCEL", 338f, 289f, 154f, Align.center, false);
            tinyFont.setColor(Color.valueOf("6b4c2e"));
            tinyFont.draw(batch, "Tap Cancel or outside the button to return", x + 20f, y + 22f, w - 40f, Align.center, false);
        }
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
        creatureBigFrame.dispose();
        spellBigFrame.dispose();
        creatureSmallFrame.dispose();
        spellSmallFrame.dispose();
        leftMenu.dispose();
        mageTop.dispose();
        mageSigil.dispose();
        mageBottom.dispose();
        infoFrame.dispose();
        logFrame.dispose();
        resourceFrame.dispose();
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
