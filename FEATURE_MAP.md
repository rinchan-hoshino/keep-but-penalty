# Keep But Penalty — Feature Map (2.0.0)

## Architecture Overview

Keep But Penalty 2.0.0 is a zero-dependency multi-loader mod sharing a single `common` codebase across 8 targets (`1.20.1` Fabric/Forge, `1.21.1` Fabric/NeoForge, `26.1.2` Fabric/NeoForge, `26.2` Fabric/NeoForge) using thin version bridges (`v120`, `v121`, `v26`).

```
common/src/main/java/dev/rinchan/keepbutpenalty/
├── KeepButPenalty.java                  # Death & respawn orchestration, summary builder
├── config/
│   ├── SimpleToml.java                  # Zero-dependency sectioned TOML parser & writer
│   ├── KeepButPenaltyConfig.java        # Config schema, hot save, legacy -common.toml migration
│   ├── DurabilityPenaltyMode.java       # PERCENT_MAX | FLAT
│   └── ExclusionMatcher.java            # Item ID & #tag exclusion compiler
├── policy/
│   ├── ExperiencePenaltyPolicy.java     # Exact forward & inverse vanilla XP math
│   ├── DurabilityPenaltyPolicy.java     # Vanilla-decoupled durability & break evaluation
│   ├── HungerPenaltyPolicy.java         # Anti-suicide hunger floor & saturation clamping
│   ├── DeathStreakPolicy.java           # Consecutive death streak state machine
│   └── RespawnDebuffSpec.java           # "effect_id,duration_seconds,level" parser
├── state/
│   ├── KeepInventoryProjection.java     # Reentrant ThreadLocal keepInventory scope
│   ├── PendingRespawnState.java         # Death-to-respawn penalty snapshot
│   └── PlayerPenaltyAccess.java         # ServerPlayer state accessor interface
├── compat/
│   └── AccessoryCompat.java             # Reflection bridge for Curios, Trinkets, Accessories
└── mixin/
    └── ServerPlayerMixin.java           # Pure Sponge Mixin hooks on die() & restoreFrom()
```

## Version-Specific Bridges (`common/src/{v120,v121,v26}/`)

| Component | `v120` (1.20.1) | `v121` (1.21.1) | `v26` (26.1.2 / 26.2) |
| :--- | :--- | :--- | :--- |
| `VersionBridge` | `ResourceLocation`, 3-arg `hurtAndBreak`, `MOB_EFFECT.get` | `ResourceLocation`, `hurtAndBreak(int, LivingEntity/ServerLevel, ...)`, `MOB_EFFECT.getHolder` | `Identifier`, `hurtAndBreak(int, LivingEntity/ServerLevel, ...)`, `MOB_EFFECT.get` |
| `GameRulesMixin` | `GameRules.getBoolean` (`RULE_KEEPINVENTORY`) | `GameRules.getBoolean` (`RULE_KEEPINVENTORY`) | `GameRules.get` (`KEEP_INVENTORY`) |
| `DeathStreakPersistenceMixin` | `CompoundTag` | `CompoundTag` | `ValueOutput` / `ValueInput` |
| `PlayerListMixin` | 2-arg `respawn(ServerPlayer, boolean)` | 3-arg `respawn(ServerPlayer, boolean, RemovalReason)` | 3-arg `respawn(ServerPlayer, boolean, RemovalReason)` |
| `KeepButPenaltyConfigScreen` | `GuiGraphics` + `renderBackground(graphics)` | `GuiGraphics` | `GuiGraphicsExtractor` + `extractRenderState` |
| Datapack Exclusion Tag | `data/keep_but_penalty/tags/items/excluded.json` | `data/keep_but_penalty/tags/item/excluded.json` | `data/keep_but_penalty/tags/item/excluded.json` |

## Loader Entrypoints

- **Fabric** (`fabric/`): `KeepButPenaltyFabric` (`ModInitializer`), `client.KeepButPenaltyModMenu` (`ModMenuApi` via compile-only stub).
- **Forge** (`forge/`): `KeepButPenaltyForge` (`@Mod`), `client.KeepButPenaltyForgeClient` (`ConfigScreenHandler.ConfigScreenFactory`).
- **NeoForge** (`neoforge/`): `KeepButPenaltyNeoForge` (`@Mod`), `client.KeepButPenaltyNeoForgeClient` (`@Mod(dist = Dist.CLIENT)`, `IConfigScreenFactory`).

## Configuration & Invariants

- **Experience Penalty**: Exact mathematical inverse guarantees `totalExperience(level, progress) == remainingXp` with zero calls to `giveExperiencePoints`.
- **Durability Penalty**:
  - `PERCENT_MAX`: `max(1, round(maxDamage * percent))` when `percent > 0`.
  - `FLAT`: fixed `durabilityFlat` points.
  - Elytra always caps at `maxDamage - 1`.
  - `allowBreak = false`: caps at `maxDamage - 1`.
  - `allowBreak = true`: skips stacks already `>= maxDamage`; sets `maxDamage - 1` then invokes standard `ItemStack.hurtAndBreak(max(1, maxDamage * 16), ...)`.
- **Hunger Preservation**: Only applies when pre-death `foodLevel < 20`; restores `max(minRespawnFoodLevel, beforeFood)` and `min(beforeSaturation, newFood)`.
- **Legacy Migration**: First boot without `keep_but_penalty.toml` reads `keep_but_penalty-common.toml`; active legacy durability penalties migrate to `FLAT` using legacy `durabilityLoss`, while inactive ones default to `PERCENT_MAX`.

## Verification Gates

Enable the local `pre-push` hook and run the complete verification suite (native with JDK 17/21/25, or reproducible container via `Dockerfile.verify`):

```bash
./scripts/setup-hooks.sh
./scripts/verify.sh [--container]
```

| Gate | Implementation | Command |
| :--- | :--- | :--- |
| **1. Packaged-JAR Real Runtime E2E** | `neoforge/src/e2e/.../KeepButPenaltyGameTests.java` (loads real built `keep_but_penalty-neoforge-*.jar` from `mods/` into Minecraft `1.21.1` `gameTestServer`: `ServerPlayer.die()` → `PlayerList.respawn()`, inventory retention, `-10%` durability & `allowBreak=false` 1-durability floor, `-25%` exact XP, anti-suicide hunger floor `6`, progressive debuffs, Totem of Undying & cancelled `LivingDeathEvent` zero-penalty) + `ConfigScreenLifecycleTest.java` | `python3 scripts/verify-e2e.py` (or `./gradlew :neoforge:check -Pminecraft_version=1.21.1 -Ploader=neoforge`) |
| **2. Property Invariant Fuzzing** | `common/src/test/.../PenaltyInvariantsPropertyTest.java` (fixed-seed `2,000` iterations per property across XP 0..200 inverse/retention, durability `allowBreak=false/true` & Elytra, hunger floor/saturation, and death-streak monotonic schedule) | `./gradlew :neoforge:test --tests '*PropertyTest' -Pminecraft_version=1.21.1 -Ploader=neoforge` |
| **3. Architecture Guardrails** | `common/src/test/.../ArchitectureContractTest.java` + `scripts/build-matrix.py` (Dedicated Server client-class isolation, Mixin bidirectional closure `mixins.json` ↔ bytecode & `@Mixin` sources, clean package redline: zero `rinlib`, zero `com/terraformersmc/modmenu` stub leakage across all 8 JARs) | `./gradlew :neoforge:test --tests '*ArchitectureContractTest' -Pminecraft_version=1.21.1 -Ploader=neoforge` & `python3 scripts/build-matrix.py` |
