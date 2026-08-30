# Keep But Penalty

**Keep your inventory. Choose the cost of death.**

Keep But Penalty is a lightweight server-side NeoForge mod for packs and servers that want vanilla-owned inventory retention without grave recovery loops or persistent world-rule changes.

## Default behavior

- Keeps player inventory through a death-scoped projection of vanilla `keepInventory` behavior.
- Does not modify or save the world's real `keepInventory` gamerule.
- Does not copy inventories or own item drops/player clones.
- Applies no experience, durability, or respawn-effect penalty by default.

## Optional penalties

The common config can enable:

- A retained fraction of total vanilla experience.
- Direct durability loss for armor, hands, Curios, and Accessories equipment.
- Any configured respawn debuffs using `effect_id,duration_seconds,level` entries.

Example:

```toml
[death]
keepInventory = true
enableExperiencePenalty = true
experienceKeepRatio = 0.333333333
respawnDebuffs = [
  "minecraft:weakness,60,2",
  "minecraft:mining_fatigue,60,2",
  "minecraft:slowness,60,2"
]
# Optional progressive durations by consecutive final death. Empty keeps the
# per-effect durations above. Entries beyond the list reuse its last value.
respawnDebuffDurationsSecondsByDeath = [0, 5, 10, 15, 20]
respawnDebuffStreakResetSeconds = 600

[durability]
enableDurabilityPenalty = false
durabilityLoss = 80
```

Curios and Accessories are optional. Their equipped slots participate only when the corresponding mod is installed and durability penalties are enabled.

## Supported versions

The maintained release matrix follows modpack anchor versions rather than every Minecraft point release. One Keep But Penalty source line supports:

- Minecraft 1.21.1
- Minecraft 26.1.2
- Minecraft 26.2

RinLib 0.3.1 or newer is required.

## Upgrading from 0.1.6

Version 0.1.6 wrote `keepInventory=true` into existing worlds. Stable releases never mutate that rule and cannot infer the administrator's original intent. Reset legacy worlds once if the vanilla baseline should be restored:

```mcfunction
/gamerule keepInventory false
```

Minecraft 26.x uses the renamed rule key:

```mcfunction
/gamerule minecraft:keep_inventory false
```

## Downloads

- [Modrinth](https://modrinth.com/mod/keep-but-penalty)
- [GitHub Releases](https://github.com/rinchan-hoshino/keep-but-penalty/releases)
