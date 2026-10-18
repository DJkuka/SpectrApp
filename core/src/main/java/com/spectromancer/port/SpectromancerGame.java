package com.spectromancer.port;

import com.badlogic.gdx.Game;

public final class SpectromancerGame extends Game {
    @Override public void create() {
        setScreen(new TitleScreen(this));
    }
}
