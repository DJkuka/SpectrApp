# Spectromancer Native — Java / LibGDX prototype

This is the first native implementation milestone based on the ActionScript export provided in the conversation. It is **not yet a complete port of Spectromancer** and it does not use a Flash player or browser.

## What is included

- `core/`: platform-independent Java battle model and LibGDX 2D screen.
- `android/`: Android launcher, landscape layout, native LibGDX libraries, and **no INTERNET permission**.
- `desktop/`: a desktop launcher to iterate on battle logic without repeatedly installing the APK.
- `assets/data/cards.json`: 223 parsed card definitions (`id`, category, type, cost, attack, health, bonus, and the source description from `Card.as`).
- `CardCatalog.java`: the same stat metadata compiled into Java; 20 cards are selected for the first playable deck.
- `tests/` and `scripts/test-core.sh`: dependency-free smoke test for the battle engine.

The project uses LibGDX **1.14.2**, listed as the current stable version by the official LibGDX project-generation documentation at the time this project was prepared: https://libgdx.com/wiki/start/project-generation

## First playable slice

- 60 starting health and six creature lanes per side.
- Starting elemental/special power allocation copied from `Player.initPowersSingleDuel()` for a 16-power duel; resource growth is initialized at +1 per resource and increases on turn transitions.
- 20 imported card definitions: `1, 2, 6, 10, 14, 16, 20, 22, 25, 27, 30, 35, 37, 38, 42, 45, 49, 50, 51, 54`.
- Card costs, categories, creature/spell type, attack and health are taken from the supplied `Card.as` export.
- Creature summoning, resource spending, one-use cards, lane attacks, hero damage, health, victory/defeat, local AI actions, card targeting and a bounded combat log.
- Selected effects ported for the starter cards (e.g. Flame Wave, Inferno, Acidic Rain, Call to Thunder, Lightning Bolt, Nature's Ritual, Natural Fury, Stone Rain, Dark Ritual, Cursed Fog, Banshee, elemental-summon effects and several on-turn effects).
- Original-art title screen and battle-board pass. Card art is drawn inside imported ornate card frames; card costs, attack, health and category labels are overlaid from the ported metadata. The hand supports tap-to-select and drag-to-target. It uses FreeType to render Cyrillic with a font already present in the operating system; no font file is bundled. Original card illustration crops, title/battle backgrounds, card frames, mage portraits and paper/button textures exported from the user-provided SWF are included in `assets/gfx/`. The original animation system, sounds and game-wide screen flow are **not yet ported**.

## What is still a prototype / not faithful yet

1. The original `AIDecisionSystem` is substantially more involved: it simulates cloned boards via `DummyPlayer`, `DummyCard` and `GameSituation`. The current AI is a greedy heuristic, not a faithful port of the original AI.
2. Only the 20 listed cards are in the playable starter deck. Metadata for the rest is available in the catalog, but most of their abilities are not implemented.
3. The original effect system is extensive. Some card effects are approximate or omitted; the UI displays a generic note when a card's effect is not implemented.
4. Original Flash timeline animations, sounds, full campaign/arena/draft/deck builder, achievements, portal services, online records and original save migration have not been ported. The current graphic pass displays authentic card art and selected UI art but is not yet a complete reproduction of the original game.
5. The default 16-power duel and special category `Death` are used to focus the first slice. Mage selection, the original 20-card distribution algorithm and all game modes are pending. The assets bring the native prototype much closer visually, but this is still a first graphics pass rather than a complete game reproduction.

## Run the core smoke test

On a machine with Java/JDK installed:

- Linux/macOS: `./scripts/test-core.sh`
- Windows: `scripts\test-core.bat`

This test compiles the platform-independent model with `javac`; it does not require Android SDK, Gradle, or LibGDX libraries. It covers imported metadata, summoning/resource spending, targeted spell resolution, direct damage, win condition, and turn progression.

## Open / build Android

1. Install Android Studio and JDK 17.
2. Install Android SDK Platform 35 and Android Build Tools from SDK Manager. The app uses Java 11 source/target and requires a JDK 17+ Gradle runtime.
3. Open this project folder (`SpectromancerNative`) in Android Studio as a Gradle project and let it sync. The build files request Android Gradle Plugin 8.13.2 and Gradle 8.13 compatibility.
4. Select the `android` module and run `assembleDebug` from the Gradle task panel, or run `gradle :android:assembleDebug` from a terminal where Gradle 8.13 is installed and Android SDK is configured. On Windows with Gradle on PATH, `scripts\build-apk.bat` runs the core tests and builds the debug APK.
5. Expected output after a successful local build: `android/build/outputs/apk/debug/android-debug.apk` (the exact generated filename can vary with Android Gradle Plugin settings).

This environment does not include Android SDK or Gradle and has no package-repository network access, so an APK has **not** been built or device-tested here. The build project is prepared, but a successful APK build still needs to be verified locally.

## Compatibility target

- `minSdk 23`; Android 11 and Android 12 are both within the supported range.
- Landscape orientation; designed for a 1280×720 logical viewport and scales to different screens.
- No `INTERNET` permission or advertised/portal SDK in this Android module.

## Project structure

```text
SpectromancerNative/
  settings.gradle
  build.gradle
  gradle.properties
  assets/data/cards.json
  core/src/main/java/com/spectromancer/port/
  android/src/main/
  desktop/src/main/java/
  tests/BattleEngineSmokeTest.java
  scripts/test-core.sh
  scripts/build-apk.bat
  reference/PORT_STATUS.md
  scripts/test-core.bat
```

## Graphics pass in this revision

The `assets/gfx/` folder contains the 223 original card illustration crops from the submitted SWF atlas, cleaned transparent card-frame templates, mage portraits, a title-screen image, an adapted battlefield background, and paper/button textures. The Java screen loads these resources and renders the art on cards and creatures. The full original Flash animation/timeline system is not yet ported.

A GitHub Actions workflow lives at `.github/workflows/android.yml`. Push to `main`/`master` or manually run **Actions → Build native Android APK**; the resulting APK is uploaded as `Spectromancer-Native-debug`.


## Interaction + mechanics pass (2026-10-18)

- The hand now shows seven cards at near-battlefield size (128×140 logical units) per page; previous/next buttons and horizontal swipe across empty hand space navigate the full 20-card deck without shrinking the art.
- Long-press (about 0.5 seconds) on a hand card or an occupied battlefield creature opens a parchment panel. It shows the art, name, localized category/type, cost, current or projected stats, original imported rule text, and a live effect preview calculated from the current powers/board where supported. The help panel remains open after release; tap anywhere to close it. A long press never casts the selected spell by accident.
- The four classic Elementals use their owner's current matching elemental power as attack; the value refreshes when power is spent, grows or is reduced.
- Ice Golem / Steel Golem resist spell and creature-ability damage but still take ordinary lane-attack damage. This includes Wall of Fire, Fire Elemental, Goblin Berserker and Banshee ability damage.
- Additional baseline effects are implemented for Armageddon, Meditation, Chain Lightning, Tornado, Rejuvenation and Drain Souls. Lightning Bolt, Armageddon and Rejuvenation use pre-payment power, matching the order in the original `GamePlay.castSpell()` / `SpellCard.spellAttack()` flow. Blood-school spell health costs are also applied.
- Core smoke tests now cover those spell mechanics, live Lightning Bolt preview, Ice Golem immunity and dynamic Elemental attack.

The Android UI code and assets are included in this source revision. This environment still lacks Android SDK/Gradle and cannot run a device-level APK build; use GitHub Actions to build the APK after pushing this revision.


## Revision 0.2.0 — touch UX and rules pass

- Replaced the undersized multi-row hand with a 7-card paged row at the same logical card dimensions used on the battlefield. Tap the page arrows or swipe horizontally through gaps in the hand row.
- Long-press for about half a second on a hand card or occupied creature to open a detail panel. The panel shows original art, localized category/type, cost, creature stats, imported rule text, and a live calculation for supported effects. Tap to dismiss.
- Added contextual previews for damage and healing amounts, power changes, summoning effects, and death triggers for the starter set and selected other cards.
- Continued rules port: Armageddon, Meditation, Chain Lightning, Tornado, Rejuvenation, Drain Souls, and Blood-school spell health cost; scaling for Lightning Bolt, Armageddon, and Rejuvenation now uses pre-payment power, matching original call order.
- Corrected Goblin Berserker to damage at most one neighbour per owner turn (left takes priority), and fixed Emissary of Dorlak so it can replace an existing friendly creature even when all six lanes are occupied. Removed an attack-growth effect that belongs only to upgraded Bargul.
- Regression smoke tests cover these changes. Android UI compilation/device behavior still must be confirmed by the GitHub Actions APK build and a device install.
