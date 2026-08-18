# Keep But Penalty

**Keep your inventory. Pay a real death penalty.**

Keep But Penalty is a lightweight NeoForge mod for packs and servers that want to avoid item drops, grave recovery loops, or corpse runs without making death free.

When a player dies, the mod can keep their inventory, remove part of their vanilla experience, damage equipped items, and apply vanilla combat debuffs after respawning.

## Default behavior

- Keeps player inventory on death without changing the vanilla `keepInventory` gamerule.
- Keeps one third of total vanilla experience after death.
- Removes two thirds of total vanilla experience after death.
- Applies vanilla Weakness I and Mining Fatigue I for one minute after a death respawn.
- Applies 80 durability damage to selected equipped items.
- Includes armor, main hand, off hand, Curios slots, and Accessories slots by default.
- Allows items to reach 0 durability, so it pairs well with no-break mods such as Keep My Sword.

## Configuration

Config file:

```text
config/keep_but_penalty-common.toml
```

Useful options:

- `death.keepInventory`
- `death.experienceKeepRatio`
- `death.enableRespawnDebuffs`
- `death.respawnDebuffDurationSeconds`
- `durability.durabilityLoss`
- `durability.damageArmor`
- `durability.damageMainHand`
- `durability.damageOffHand`
- `durability.damageCurios`
- `durability.damageAccessories`

## Requirements

- Minecraft 1.21.1
- NeoForge

Curios and Accessories are optional. If installed, their equipped item slots can be included in the durability penalty.
