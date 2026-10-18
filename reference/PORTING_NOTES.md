# Notes from the supplied ActionScript 3 export

## Original source landmarks

- `Card.as`: `initializeCard(...)` contains IDs, category, type, label, cost, and creature attack/health values. 223 records were extracted into `assets/data/cards.json` and `CardCatalog.java`.
- `getName.as` / `getDefination.as`: maps between numeric card IDs and SWF card-symbol classes.
- `Player.as`: starting health, six creature lanes, `initPowersSingleDuel()`, `canUseCard()`, resource growth, creature and turn lifecycle.
- `CardDistributionAlgo.as` and `CardDistribution.as`: original starter-deck composition and opponent-specific deck substitutions. The first port does not reproduce this algorithm yet.
- `CreatureCard.as`: attack/health handling, `cardStartTurn()`, `playerEndsTurnFunction()`, `setDamage()` and per-card abilities.
- `SpellCard.as`: spell effects and damage calculation.
- `GamePlay.as`: card selection, spell targeting, end-turn and attack sequencing.
- `AIDecisionSystem.as`: original move generation and simulated board evaluation.
- `DummyPlayer.as`, `DummyCard.as`, `GameSituation.as`: AI simulation support; not yet ported.
- `DataManager.as`: Flash `SharedObject` local storage; persistence has not been ported.

## Native-port rule decisions

The current Java model ports the familiar six-lane structure, 60 health, one-time deck cards, elemental/special resource costs, growth on turn changes and the initial single-duel power allocation formula. A few abilities from the first 20-card deck have dedicated effect handlers. Any behaviour not backed by a handler should be treated as unimplemented or approximate, not as verified identical to Flash.

The original user-supplied ActionScript source is intentionally kept outside this project zip except for the extracted data catalog, to keep the project lightweight. The previously provided `Spectromancer_AS3_source.zip` remains the reference export.
