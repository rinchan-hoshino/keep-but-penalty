# Changelog

## 1.1.0 — 2026-08-29

- Add an optional consecutive-final-death duration schedule for respawn debuffs.
- Persist and clone the death streak, resetting it after a configurable survival window.
- Observe uncancelled final deaths at NeoForge `LOWEST`, so downed/rescued players are not counted.
- Preserve legacy fixed per-effect durations when the progressive schedule is empty.

## 1.0.1 — 2026-08-25

- Maintain only the modpack anchor targets: NeoForge 1.21.1, 26.1.2, and 26.2.
- Remove the non-anchor 1.21.4, 1.21.5, 1.21.8, and 1.21.11 publication targets.
- Require RinLib 0.3.1 for the new 26.x state bridges.

## 1.0.0 — 2026-08-25

- Keep inventory through a death/respawn-scoped gamerule projection without mutating or serializing the world's real rule.
- Disable experience and durability penalties by default.
- Replace fixed Weakness/Mining Fatigue fields with an empty-by-default `respawnDebuffs` array using `effect_id,duration_seconds,level` entries.
- Move cross-version game-rule and mob-effect differences behind RinLib 0.3.0 so Keep But Penalty retains one source line.
- Publish the same behavior contract for NeoForge 1.21.1, 1.21.4, 1.21.5, 1.21.8, and 1.21.11.

## 0.1.6 — 2026-08-23

- Publish the vanilla `keepInventory` gamerule when the server starts, making inventory retention part of this mod's own contract.
- Keep vanilla death, drop, clone, Curios, and Accessories behavior instead of copying or restoring inventories manually.
- Apply configurable experience loss and equipped-item durability damage on death.
- Apply configurable one-minute Weakness and Mining Fatigue penalties after respawn.
