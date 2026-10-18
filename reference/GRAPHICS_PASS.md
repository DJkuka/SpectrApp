# Graphics pass: what's changed

## Included in this revision

- A native title screen that uses the supplied game's title/logo/castle art.
- A 1280×720 landscape battlefield background adapted from the supplied SWF background resources.
- 223 individual card illustration crops from `BigData`, using the card-ID ordering confirmed by `GParams.getBigData(cardId)`.
- Four original transparent frame templates (large/small creature/spell frames); placeholder strings/numbers were cleared so the native code can draw the live card name and stats.
- Original player/opponent portrait crops.
- Original button/paper textures and a new horizontal detail strip made from the original paper texture.
- Card illustration and frame rendering in the deck hand and creature slots; selected/used/dragged states are drawn by the LibGDX screen.
- Correct `touchDragged` method signature for LibGDX's `InputAdapter`; the tired-creature tint is now drawn by the active `SpriteBatch`, not by opening `ShapeRenderer` inside an active batch.
- GitHub Actions workflow at `.github/workflows/android.yml`.

## Tests run here

- `sh scripts/test-core.sh`: PASS (223-card catalog, summon and cost, targeted spell, direct damage, victory, turn progression).
- Java source syntax/signature smoke compile against API-shaped LibGDX stubs: PASS. This catches ordinary Java syntax/override mistakes but is **not** a substitute for compiling against the real LibGDX artifacts.
- Asset inventory: 223/223 expected `card_###.jpg` crops present.
- APK build/device test: **not run in this environment**. Maven Central cannot be reached here and Android SDK/Gradle are not installed. Use the included GitHub Actions workflow for the real Gradle build.

## Still incomplete

This is a graphics milestone for the existing playable slice, not yet a full faithful recreation of Spectromancer. Original Flash timeline animations and sound, the campaign/arena/draft/league screens, all card special effects, and the original `AIDecisionSystem` still need to be ported and validated. The current native AI remains heuristic.
