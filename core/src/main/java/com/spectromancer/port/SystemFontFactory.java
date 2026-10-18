package com.spectromancer.port;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

/**
 * Uses a font already supplied by the operating system, avoiding bundled font files.
 * A built-in LibGDX bitmap font remains as a safe fallback on unusual systems.
 */
public final class SystemFontFactory {
    private static final String CYRILLIC =
            "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ" +
            "абвгдеёжзийклмнопрстуфхцчшщъыьэюя" +
            "ІЇЄҐієїґ·—…№";

    private static final String[] CANDIDATES = {
            "/system/fonts/Roboto-Regular.ttf",
            "/system/fonts/RobotoStatic-Regular.ttf",
            "C:/Windows/Fonts/arial.ttf",
            "C:/Windows/Fonts/segoeui.ttf",
            "/System/Library/Fonts/Supplemental/Arial.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf",
            "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf"
    };

    private SystemFontFactory() { }

    public static BitmapFont create(int size) {
        for (String path : CANDIDATES) {
            FreeTypeFontGenerator generator = null;
            try {
                FileHandle fontFile = Gdx.files.absolute(path);
                if (!fontFile.exists()) continue;
                generator = new FreeTypeFontGenerator(fontFile);
                FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
                parameter.size = size;
                parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS + CYRILLIC;
                parameter.kerning = true;
                return generator.generateFont(parameter);
            } catch (RuntimeException | LinkageError ignored) {
                // Continue to the next system font if this platform denies access or FreeType fails.
            } finally {
                if (generator != null) generator.dispose();
            }
        }
        return new BitmapFont();
    }
}
