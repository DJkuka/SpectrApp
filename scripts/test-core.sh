#!/usr/bin/env sh
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
OUT="$ROOT/build/core-smoke-classes"
rm -rf "$OUT"
mkdir -p "$OUT"
javac -encoding UTF-8 -d "$OUT" \
  "$ROOT/core/src/main/java/com/spectromancer/port/Element.java" \
  "$ROOT/core/src/main/java/com/spectromancer/port/CardType.java" \
  "$ROOT/core/src/main/java/com/spectromancer/port/CardDefinition.java" \
  "$ROOT/core/src/main/java/com/spectromancer/port/CreatureState.java" \
  "$ROOT/core/src/main/java/com/spectromancer/port/PlayerState.java" \
  "$ROOT/core/src/main/java/com/spectromancer/port/CardCatalog.java" \
  "$ROOT/core/src/main/java/com/spectromancer/port/BattleEngine.java" \
  "$ROOT/tests/BattleEngineSmokeTest.java"
java -cp "$OUT" com.spectromancer.port.BattleEngineSmokeTest
