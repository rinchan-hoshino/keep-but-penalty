# Changelog

All notable changes to **Keep But Penalty** are documented in this file.

## [2.0.0] - 2026-11-20

### Added
- **Multi-loader 8-target support**: Full support for **Fabric**, **Forge**, and **NeoForge** across Minecraft `1.20.1`, `1.21.1`, `26.1.2`, and `26.2` with zero external library dependencies.
- **Zero-dependency TOML config & automatic migration**: Moved configuration to `config/keep_but_penalty.toml` via built-in `SimpleToml`, with automatic lossless migration from legacy `config/keep_but_penalty-common.toml`.
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
- **Removed all NeoForge coupling and RinLib package remnants** from `common/`.

## [1.1.2] - 2026-04-01
- Gated durability penalty boundaries with unit tests.
- Required end-to-end keep inventory projection during death and respawn.

## [1.1.0] - 2026-03-15
- Added progressive respawn debuffs with consecutive death streak persistence in player NBT.

## [1.0.0] - 2026-02-01
- Stabilized death-scoped `keepInventory` projection without mutating world gamerules.
