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
  "minecraft:weakness,60,1",
  "minecraft:mining_fatigue,60,1"
]

[durability]
enableDurabilityPenalty = false
durabilityLoss = 80
```

Curios and Accessories are optional. Their equipped slots participate only when the corresponding mod is installed and durability penalties are enabled.

## Supported versions

The 1.0.0 release uses one Keep But Penalty source line for the mainstream NeoForge targets that pass the same contract:

- Minecraft 1.21.1
- Minecraft 1.21.4
- Minecraft 1.21.5
- Minecraft 1.21.8
- Minecraft 1.21.11

RinLib 0.3.0 or newer is required.

## Upgrading from 0.1.6

Version 0.1.6 wrote `keepInventory=true` into existing worlds. Version 1.0.0 never mutates that rule and cannot infer the administrator's original intent. Reset legacy worlds once if the vanilla baseline should be restored:

```mcfunction
/gamerule keepInventory false
```

Minecraft 1.21.11 uses the renamed rule key:

```mcfunction
/gamerule minecraft:keep_inventory false
```

## Downloads

- [Modrinth](https://modrinth.com/mod/keep-but-penalty)
- [GitHub Releases](https://github.com/rinchan-hoshino/keep-but-penalty/releases)
