package com.spectromancer.port;

/** Resource families used by the original game's card categories. */
public enum Element {
    FIRE("Огонь"), WATER("Вода"), AIR("Воздух"), EARTH("Земля"), SPECIAL("Особая сила");

    private final String label;
    Element(String label) { this.label = label; }
    public String label() { return label; }

    public static Element fromCategory(String category) {
        if (category == null) return SPECIAL;
        switch (category.toLowerCase(java.util.Locale.ROOT)) {
            case "fire": return FIRE;
            case "water": return WATER;
            case "air": return AIR;
            case "earth": return EARTH;
            default: return SPECIAL;
        }
    }
}
