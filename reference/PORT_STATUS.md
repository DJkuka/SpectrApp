# Current port status

This is a first playable-slice source project, not a complete reimplementation.

## Implemented in the native Java model

- Standard 60-HP duel state and six slots per side.
- Resource categories, initial resource allocation pattern, resource spending and growth at turn transition.
- A 20-card starter deck and a catalog of 223 card records extracted from the submitted ActionScript export.
- Summoning, one-use cards, selected targeted spells, lane attacks, health, win/loss, bounded battle log.
- Heuristic offline AI (choose and play available cards; it is not the original search/simulation AI).
- Selected effects for the first deck. Each effect still needs comparison with original behavior and edge cases.
- Touch-capable LibGDX screen, landscape Android launcher, desktop launcher.
- First graphics pass using original SWF art: 223 card illustrations, original ornate creature/spell frame templates, mage portraits, adapted title/battle backgrounds, paper and button textures. Artwork is displayed in card hand and creature slots; UI overlays costs, attack and health.
- OS-provided font lookup via FreeType so Russian/Cyrillic text can render without bundling a font file.
- No `INTERNET` permission, analytics, portal or advertising SDK in the Android module.

## Not implemented / not verified

- A full faithful port of `AIDecisionSystem` / `GameSituation`.
- All effects from all 223 cards; only a portion of special abilities are in the battle engine.
- Original timeline animations, battle effects, sound effects and scene transitions. The first pass uses static original card art and selected UI textures, not the complete original sprite/timeline system.
- Mage selection, exact original deck distribution, campaign, arena, draft, league, records, medals, and migration of original save data.
- A successful Gradle build or runtime test on an Android device. The current working environment has JDK but no Android SDK, Gradle installation, or Maven network access. A GitHub Actions workflow has been added to build an APK remotely.

## Regression test

Run `scripts/test-core.bat` on Windows or `./scripts/test-core.sh` on Linux/macOS. It compiles and exercises the dependency-free battle model only; it does not compile the LibGDX UI or make an APK.
