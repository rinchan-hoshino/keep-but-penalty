# Changelog

All notable changes to **Keep But Penalty** are documented in this file.

## [2.0.4] - 2026-11-28

### Added
- **Inventory & hotbar durability penalty (`damageInventory`)**: Added `durability.damageInventory` (default `true`) and an in-game config toggle across all versions (`1.20.1`, `1.21.1`, `26.x`). Damageable tools and weapons carried in the hotbar or main inventory now receive the same death durability penalty even when not actively held in hand, with `IdentityHashMap` deduplication ensuring no item is ever penalized twice. Existing configs lacking `damageInventory` default to `true`; set `damageInventory = false` to restore equipped-only behavior.

## [2.0.3] - 2026-11-21

### Fixed
- **1.20.1 Forge `pack.mcmeta` fix**: Added `pack.mcmeta` (`pack_format: 15`) to the Forge template resources to eliminate the `ResourcePackInfo` startup warning on Minecraft `1.20.1` Forge without affecting other targets.

### Added
- **Minecraft `26.3` support**: Added `26.3` profiles for Fabric and NeoForge, expanding the build matrix to 10 targets.
- **Verification**: Enforced `pack.mcmeta` JAR audit assertions and validated all 10 targets across the full verification suite.

## [2.0.2] - 2026-11-20

### Fixed
- **Dedicated Server isolation**: Moved client-only config screen registration and client lifecycle classes into dedicated client packages (`KeepButPenaltyForgeClient`, `KeepButPenaltyNeoForgeClient`, `KeepButPenaltyModMenu`) so dedicated servers never load client GUI classes.
- **Verification**: Validated across the 8-target build matrix, JUnit/Property tests, packaged-JAR architecture contracts, and real Minecraft 1.21.1 NeoForge GameTest E2E.

## [2.0.1] - 2026-11-20

### Changed
- **Safer default for `allowBreak`**: Changed the default value of `durability.allowBreak` from `true` to `false` so items are clamped to at least 1 durability remaining by default instead of breaking on death. Setting `allowBreak = true` continues to invoke standard vanilla `ItemStack.hurtAndBreak(...)` (or delegate to anti-break mods).

## [2.0.0] - 2026-11-20

### Added
- **Multi-loader 8-target support**: Full support for **Fabric**, **Forge**, and **NeoForge** across Minecraft `1.20.1`, `1.21.1`, `26.1.2`, and `26.2` with zero external library dependencies.
- **Zero-dependency TOML config**: Built-in `SimpleToml` configuration at `config/keep_but_penalty.toml` with instant hot-reload.
- **Out-of-the-box balanced defaults**: Experience penalty (keep 75%, lose 25%), percentage-based durability penalty (10% of max durability), and anti-suicide hunger preservation (`minRespawnFoodLevel = 6`) are enabled by default.
- **Anti-suicide hunger preservation**: Preserves below-full hunger and clamped saturation across death with a configurable safety floor (`preserveHungerOnDeath`, `minRespawnFoodLevel`).
- **PvP exemption**: Added `skipOnPvp` to optionally skip penalties when killed by another player.
- **Percentage & Flat durability modes**: Added `penaltyMode` (`PERCENT_MAX` / `FLAT`), `durabilityPercent`, `durabilityFlat`, and `excludedItems` supporting item IDs and `#tags` (with built-in `#keep_but_penalty:excluded` datapack tag and default exemptions for Tinkers' Construct, Tetra, and SlashBlade).
- **Trinkets support & reflection-based accessory compat**: Added optional `Trinkets` support alongside `Curios` and `Accessories` via zero-dependency reflection.
- **Respawn Actionbar summary**: Shows a concise summary of actual death costs on respawn (`showRespawnSummary`).
- **Native in-game Config Screen**: Added zero-dependency `KeepButPenaltyConfigScreen` wired to Mod Menu (Fabric), `ConfigScreenHandler` (Forge), and `IConfigScreenFactory` (NeoForge).
- **4-language localization**: Complete `en_us`, `zh_cn`, `zh_tw`, and `ja_jp` translations.
- **New 64×64 pixel-art icon**: Original armored ward badge artwork (`scripts/draw_icon.py`).

### Changed
- **Exact vanilla experience math**: Rewrote experience retention to directly compute and assign `experienceLevel`, `experienceProgress`, and `totalExperience` instead of calling `giveExperiencePoints`, eliminating unintended XP pickup events and Mending side effects.
- **Vanilla-decoupled durability breaking protocol**: When `allowBreak = true` and durability loss reaches `maxDamage`, items now trigger standard vanilla `ItemStack.hurtAndBreak(...)` (while Elytra stops at `maxDamage - 1`, and already-broken items `damage >= maxDamage` are left untouched).

## [1.1.2] - 2026-04-01
- Gated durability penalty boundaries with unit tests.
- Required end-to-end keep inventory projection during death and respawn.

## [1.1.0] - 2026-03-15
- Added progressive respawn debuffs with consecutive death streak persistence in player NBT.

## [1.0.0] - 2026-02-01
- Stabilized death-scoped `keepInventory` projection without mutating world gamerules.
