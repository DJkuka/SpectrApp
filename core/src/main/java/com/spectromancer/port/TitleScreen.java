package com.spectromancer.port;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;

/** Native title screen using artwork extracted from the supplied SWF. */
public final class TitleScreen extends ScreenAdapter {
    private static final float W = 1024f, H = 768f;
    private static final float BUTTON_X = 327f, BUTTON_Y = 151f, BUTTON_W = 370f, BUTTON_H = 62f;

    private final SpectromancerGame game;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(W, H, camera);
    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont titleFont = SystemFontFactory.create(28);
    private final BitmapFont bodyFont = SystemFontFactory.create(16);
    private final Vector2 point = new Vector2();
    private final Texture background = texture("gfx/title_bg.jpg");
    private final Texture button = texture("gfx/button.png");
    private boolean launched;

    public TitleScreen(SpectromancerGame game) {
        this.game = game;
        titleFont.setColor(Color.WHITE);
        bodyFont.setColor(Color.WHITE);
    }

    private static Texture texture(String path) {
        Texture result = new Texture(Gdx.files.internal(path));
        result.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return result;
    }

    @Override public void show() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override public boolean touchDown(int screenX, int screenY, int pointer, int buttonCode) {
                point.set(screenX, screenY);
                viewport.unproject(point);
                if (inside(point.x, point.y, BUTTON_X, BUTTON_Y, BUTTON_W, BUTTON_H)) {
                    startBattle();
                }
                return true;
            }
        });
    }

    private void startBattle() {
        if (launched) return;
        launched = true;
        game.setScreen(new BattleScreen(game));
        dispose();
    }

    @Override public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.045f, 0.06f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        shapes.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(background, 0, 0, W, H);
        batch.setColor(1f, 1f, 1f, 0.92f);
        batch.draw(button, BUTTON_X, BUTTON_Y, BUTTON_W, BUTTON_H);
        batch.setColor(Color.WHITE);
        titleFont.setColor(Color.valueOf("fff0c8"));
        titleFont.draw(batch, "НАЧАТЬ БОЙ", BUTTON_X, BUTTON_Y + 39, BUTTON_W, Align.center, false);
        bodyFont.setColor(Color.valueOf("e8dfca"));
        bodyFont.draw(batch, "ЛОКАЛЬНАЯ ДУЭЛЬ · БЕЗ СЕТИ И РЕКЛАМЫ", 150, 110, 724, Align.center, false);
        bodyFont.setColor(Color.valueOf("d2c2a3"));
        bodyFont.draw(batch, "Нативный порт для Android · первый этап переноса", 150, 77, 724, Align.center, false);
        batch.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.88f, 0.73f, 0.40f, 0.8f);
        shapes.rect(14, 14, W - 28, H - 28);
        shapes.rect(BUTTON_X, BUTTON_Y, BUTTON_W, BUTTON_H);
        shapes.end();
    }

    private static boolean inside(float x, float y, float rx, float ry, float rw, float rh) {
        return x >= rx && x <= rx + rw && y >= ry && y <= ry + rh;
    }

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }

    @Override public void dispose() {
        background.dispose();
        button.dispose();
        titleFont.dispose();
        bodyFont.dispose();
        batch.dispose();
        shapes.dispose();
    }
}
