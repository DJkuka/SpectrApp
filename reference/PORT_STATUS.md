# Current port status

This is a first playable-slice source project, not a complete reimplementation.

## Implemented in the native Java model

- Standard 60-HP duel state and six slots per side.
- Resource categories, initial resource allocation pattern, resource spending and growth at turn transition.
- A 20-card starter deck and a catalog of 223 card records extracted from the submitted ActionScript export.
- Summoning, one-use cards, selected targeted spells, lane attacks, health, win/loss, bounded battle log.
- Heuristic offline AI (choose and play available cards; it is not the original search/simulation AI).
- Selected effects for the first deck, including resource growth, summon effects, spell damage, healing and death triggers; additional mechanics for Armageddon, Meditation, Chain Lightning, Tornado, Rejuvenation and Drain Souls are now in the model for the later expanded deck.
- Classic elementals recalculate attack from their owner's matching power; Ice Golem / Steel Golem resist spell and creature-ability damage but still take ordinary lane attacks.
- Touch-capable LibGDX screen, landscape Android launcher, desktop launcher.
- Original-style 5×4 hand grid: all 20 starter cards visible at once, with touch-to-select/drag and long-press detail popups for hand cards and board creatures.
- Updated original-interface rendering: 223 card illustrations and ornate frames, a 4:3 board with six lanes per side, left menu rail, portrait/sigil column, top/bottom life/resource tabs, selected-card details and combat log. Artwork is displayed in hand and creature slots; UI overlays costs, attack and health.
- OS-provided font lookup via FreeType so Russian/Cyrillic text can render without bundling a font file.
- No `INTERNET` permission, analytics, portal or advertising SDK in the Android module.

## Not implemented / not verified

- A full faithful port of `AIDecisionSystem` / `GameSituation`.
- Long-press provides imported rule text and live effect previews for supported cards; full original upgrade states and all conditional/rules text are still pending.
- All effects from all 223 cards; only a portion of special abilities are in the battle engine.
- Original timeline animations, battle effects, sound effects and scene transitions. The first pass uses static original card art and selected UI textures, not the complete original sprite/timeline system.
- Mage selection, exact original deck distribution, campaign, arena, draft, league, records, medals, and migration of original save data.
- A successful Gradle build or runtime test on an Android device. The current working environment has JDK but no Android SDK, Gradle installation, or Maven network access. A GitHub Actions workflow has been added to build an APK remotely.

## Regression test

Run `scripts/test-core.bat` on Windows or `./scripts/test-core.sh` on Linux/macOS. It compiles and exercises the dependency-free battle model only; it does not compile the LibGDX UI or make an APK.


## Latest incremental work (0.2.0 prototype)

- Hand cards use the same 128x140 logical size as battlefield cards. Seven are visible at once; all 20 are available via paging controls/swipe.
- Long-press shows card art plus both the imported rule and a context-aware effect estimate.
- Additional card rules implemented and regression-tested: Armageddon, Meditation, Chain Lightning, Tornado, Rejuvenation, Drain Souls, Blood spell health cost, single-neighbour priority for Goblin Berserker, and occupied-slot replacement by Emissary of Dorlak.
- Lightning Bolt, Armageddon and Rejuvenation now evaluate elemental power before paying the card's cost, following the original Flash call order.


## Latest incremental work (0.3.0 original UI)

- Adopted 1024×768 logical coordinates from the supplied screenshot. The left menu / mage portraits / sigil / lower detail and log panels use frame crops from the reference, while gameplay art remains dynamically rendered.
- Hand layout is now five columns by four rows; all 20 starter cards are on-screen and use the original small frames. Long-press help remains available and is not routed into the card-play path.
- The center-left circular sigil area advances the turn; labeled menu controls open help/options, return to the title screen, or show a surrender confirmation.
- Added a `surrender()` battle-engine state transition and a regression test.
- Core smoke test passes. The LibGDX screen passed a Java syntax/signature compile against lightweight temporary API stubs, but this is not a substitute for the Gradle Android build or device testing.
