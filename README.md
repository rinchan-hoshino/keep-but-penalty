# Keep But Penalty

<img src="docs/artwork/icon.png" alt="Keep But Penalty" width="128" height="128">

**Keep your inventory. Pay a fair price for death.**

Keep But Penalty is a lightweight, zero-dependency multi-loader mod for **Fabric**, **Forge**, and **NeoForge** that keeps player inventories on death without mutating the world's `keepInventory` gamerule—while replacing tedious corpse runs with fair, configurable experience, durability, hunger, and progressive respawn debuff penalties.

## Highlights

- **Death-Scoped Keep Inventory**: Temporarily projects `keepInventory = true` via a reentrant `ThreadLocal` only while `ServerPlayer.die` and `ServerPlayer.restoreFrom` execute. Your world's real `keepInventory` gamerule is never altered.
- **Exact Experience Penalty**: Defaults to keeping **75%** of total experience (25% lost). Computes `experienceLevel`, `experienceProgress`, and `totalExperience` directly with exact vanilla math—never calling `giveExperiencePoints`, so XP pickup events and Mending are never spuriously triggered.
- **Fair Durability Penalty**: Defaults to **10% of max durability** (`PERCENT_MAX`) or optional fixed points (`FLAT`), with `IdentityHashMap` deduplication across armor, hands, and optional **Curios**, **Trinkets**, and **Accessories** slots.
  - **Safe-by-Default & Vanilla-Decoupled Breaking Protocol**: Defaults to `allowBreak = false`, clamping durability loss to keep at least 1 durability remaining so items never shatter from death penalties. When explicitly set to `allowBreak = true` and an item reaches max damage, Keep But Penalty invokes standard vanilla `ItemStack.hurtAndBreak(...)`—destroying the item cleanly in vanilla (no `0/max` ghost armor) or naturally letting anti-break mods (such as *Keep My Sword*) take over with zero hardcoded coupling. Vanilla Elytra always stops at `maxDamage - 1` (broken-wing state).
  - **Item & Tag Exclusions**: Exempt specific items or `#tags` via `excludedItems` (pre-configured with `#keep_but_penalty:excluded`, `#tconstruct:modifiable`, `#tetra:modular_items`, and `#slashblade:swords`).
- **Anti-Suicide Hunger Preservation**: Preserves below-full hunger across death with a configurable floor (`minRespawnFoodLevel = 6`, 3 drumsticks) so players cannot reset hunger by dying while preventing starvation loops.
- **Progressive Respawn Debuffs**: Optional configurable status effects with consecutive-death duration scaling, persisted in player NBT.
- **Respawn Actionbar Summary**: Displays a concise one-line summary of actual death costs on respawn (suppressed when no penalty occurred).
- **In-Game Config Screen & 4 Languages**: Native zero-dependency `KeepButPenaltyConfigScreen` accessible via Mod Menu (Fabric) or the Mods screen (Forge / NeoForge), with full **English**, **简体中文**, **繁體中文**, and **日本語** localization.

## Configuration

Stored at `config/keep_but_penalty.toml` (edits saved in-game take effect immediately).

```toml
[death]
keepInventory = true
enableExperiencePenalty = true
experienceKeepRatio = 0.75
preserveHungerOnDeath = true
minRespawnFoodLevel = 6
skipOnPvp = false
respawnDebuffs = []
respawnDebuffDurationsSecondsByDeath = []
respawnDebuffStreakResetSeconds = 600
showRespawnSummary = true

[durability]
enableDurabilityPenalty = true
penaltyMode = "PERCENT_MAX"
durabilityPercent = 0.1
durabilityFlat = 80
allowBreak = false
damageArmor = true
damageMainHand = true
damageOffHand = true
damageCurios = true
damageTrinkets = true
damageAccessories = true
excludedItems = ["#keep_but_penalty:excluded", "#tconstruct:modifiable", "#tetra:modular_items", "#slashblade:swords"]
```

## Supported Versions & Loaders

Zero external library dependencies—single self-contained JAR per target:

| Minecraft | Loaders | Optional Accessory Compat |
| :--- | :--- | :--- |
| `1.20.1` | Fabric, Forge | Curios, Trinkets, Accessories |
| `1.21.1` | Fabric, NeoForge | Curios, Trinkets, Accessories |
| `26.1.2` | Fabric, NeoForge | Curios, Trinkets, Accessories |
| `26.2` | Fabric, NeoForge | Curios, Trinkets, Accessories |
| `26.3` | Fabric, NeoForge | Curios, Trinkets, Accessories |

## Building & Verification

Enable the local `pre-push` verification hook and run the full verification suite (10-target build, JUnit + property fuzzing, architecture bytecode guardrails, and packaged-JAR Minecraft runtime E2E):

```bash
./scripts/setup-hooks.sh
./scripts/verify.sh [--container]
```

## Download

- [Modrinth](https://modrinth.com/mod/keep-but-penalty)
- [GitHub Releases](https://github.com/rinchan-hoshino/keep-but-penalty/releases)
