package dev.rinchan.keepbutpenalty.e2e;

import com.mojang.authlib.GameProfile;
import dev.rinchan.keepbutpenalty.config.KeepButPenaltyConfig;
import dev.rinchan.keepbutpenalty.policy.ExperiencePenaltyPolicy;
import io.netty.channel.embedded.EmbeddedChannel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Deliverable-level black-box GameTest E2E suite running inside a real Minecraft 1.21.1
 * NeoForge server against the real packaged keep_but_penalty JAR in mods/.
 * Zero internal bypasses: every scenario drives vanilla ServerPlayer.hurt/die and PlayerList.respawn.
 */
@GameTestHolder(KeepButPenaltyE2EMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KeepButPenaltyGameTests {
    private static final String EMPTY_TEMPLATE = "empty";

    private record ConnectedPlayer(ServerPlayer player, EmbeddedChannel channel) {}

    private static ConnectedPlayer spawnRealPlayer(ServerLevel level, String name) {
        MinecraftServer server = level.getServer();
        GameProfile profile = new GameProfile(UUID.randomUUID(), name);
        ServerPlayer player = new ServerPlayer(server, level, profile, ClientInformation.createDefault());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        EmbeddedChannel channel = new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false));
        player.setGameMode(GameType.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        return new ConnectedPlayer(player, channel);
    }

    private static void cleanupPlayer(ServerLevel level, ServerPlayer player, EmbeddedChannel channel) {
        if (player != null) {
            level.getServer().getPlayerList().remove(player);
        }
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
    }

    private static void writeAndReloadConfig(String toml) {
        Path configDir = FMLPaths.CONFIGDIR.get();
        Path file = configDir.resolve(KeepButPenaltyConfig.FILE_NAME);
        try {
            Files.createDirectories(configDir);
            Files.writeString(file, toml, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write E2E config file: " + file, e);
        }
        KeepButPenaltyConfig.load(configDir);
    }

    private static void restoreDefaultConfig() {
        writeAndReloadConfig(KeepButPenaltyConfig.defaults().toToml());
    }

    private static void assertEq(GameTestHelper helper, Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            helper.fail(message + " (expected=" + expected + ", actual=" + actual + ")");
        }
    }

    private static void assertTrue(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }

    @GameTest(template = EMPTY_TEMPLATE, templateNamespace = KeepButPenaltyE2EMod.MOD_ID)
    public static void packagedJarLoadedFromModsDir(GameTestHelper helper) {
        assertTrue(helper, ModList.get().isLoaded("keep_but_penalty"),
                "keep_but_penalty mod must be loaded");
        Path modPath = ModList.get().getModFileById("keep_but_penalty").getFile().getFilePath();
        String pathStr = modPath.toAbsolutePath().toString();
        assertTrue(helper, pathStr.endsWith(".jar"),
                "keep_but_penalty must be loaded from packaged .jar, not exploded classes: " + pathStr);
        assertTrue(helper, pathStr.contains("keep_but_penalty-neoforge-"),
                "Unexpected packaged JAR path: " + pathStr);
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE, templateNamespace = KeepButPenaltyE2EMod.MOD_ID)
    public static void fullDeathRespawnPenaltyLoop(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(false, level.getServer());
        restoreDefaultConfig();

        ConnectedPlayer conn = spawnRealPlayer(level, "E2EFullLoop");
        ServerPlayer active = conn.player();
        try {
            // Equip items: undamaged helmet, near-broken chestplate (3 left), undamaged sword, undamaged shield
            ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET); // maxDamage = 363 -> -10% = 36
            ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE); // maxDamage = 528 -> 525 + 53 -> clamp 527 (1 left)
            chest.setDamageValue(chest.getMaxDamage() - 3);
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD); // maxDamage = 1561 -> -10% = 156
            ItemStack shield = new ItemStack(Items.SHIELD); // maxDamage = 336 -> -10% = 34

            active.setItemSlot(EquipmentSlot.HEAD, helmet);
            active.setItemSlot(EquipmentSlot.CHEST, chest);
            active.setItemSlot(EquipmentSlot.MAINHAND, sword);
            active.setItemSlot(EquipmentSlot.OFFHAND, shield);
            active.getInventory().setItem(5, new ItemStack(Items.COBBLESTONE, 64));

            // Level 30 exact = 1395 XP -> keep 75% = 1046 XP (-25%)
            active.experienceLevel = 30;
            active.experienceProgress = 0.0f;
            active.totalExperience = 1395;

            // Low hunger below floor 6 -> must restore to floor 6, not reset to 20
            active.getFoodData().setFoodLevel(2);
            active.getFoodData().setSaturation(1.5f);

            // Real death via vanilla damage pipeline -> ServerPlayer.die()
            active.hurt(level.damageSources().genericKill(), 1000.0f);
            assertTrue(helper, active.isDeadOrDying(), "Player must be dead after lethal damage");

            // Real respawn via PlayerList.respawn()
            ServerPlayer respawned = level.getServer().getPlayerList()
                    .respawn(active, false, Entity.RemovalReason.KILLED);
            active = respawned;

            // 1. World gamerule was NEVER mutated
            assertTrue(helper, !level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY),
                    "keepInventory gamerule must remain false after death/respawn");

            // 2. Inventory & equipment preserved
            ItemStack keptCobble = respawned.getInventory().getItem(5);
            assertTrue(helper, keptCobble.is(Items.COBBLESTONE) && keptCobble.getCount() == 64,
                    "Inventory stack must be preserved across respawn");

            ItemStack postHelmet = respawned.getItemBySlot(EquipmentSlot.HEAD);
            ItemStack postChest = respawned.getItemBySlot(EquipmentSlot.CHEST);
            ItemStack postSword = respawned.getItemBySlot(EquipmentSlot.MAINHAND);
            ItemStack postShield = respawned.getItemBySlot(EquipmentSlot.OFFHAND);
            assertTrue(helper, !postHelmet.isEmpty() && !postChest.isEmpty()
                    && !postSword.isEmpty() && !postShield.isEmpty(),
                    "All equipped items must be preserved");

            // 3. Exact -10% max durability & allowBreak=false 1-durability floor
            assertEq(helper, (int) Math.round(postHelmet.getMaxDamage() * 0.10D), postHelmet.getDamageValue(),
                    "Helmet must take exact -10% max durability");
            assertEq(helper, postChest.getMaxDamage() - 1, postChest.getDamageValue(),
                    "Near-broken chestplate must clamp at 1 durability remaining under allowBreak=false");
            assertEq(helper, (int) Math.round(postSword.getMaxDamage() * 0.10D), postSword.getDamageValue(),
                    "Sword must take exact -10% max durability");
            assertEq(helper, (int) Math.round(postShield.getMaxDamage() * 0.10D), postShield.getDamageValue(),
                    "Shield must take exact -10% max durability");

            // 4. Exact -25% total XP penalty
            int actualTotalXp = ExperiencePenaltyPolicy.totalExperience(
                    respawned.experienceLevel, respawned.experienceProgress);
            assertEq(helper, 1046, actualTotalXp, "Total XP must be exact 75% (1046) of 1395");
            assertEq(helper, 1046, respawned.totalExperience, "ServerPlayer.totalExperience must match 1046");

            // 5. Anti-suicide hunger floor = 6 (never reset to 20)
            assertEq(helper, 6, respawned.getFoodData().getFoodLevel(),
                    "Hunger below floor must be floored to 6 on respawn");
            assertEq(helper, 1.5f, respawned.getFoodData().getSaturationLevel(),
                    "Saturation must be preserved on respawn");

            helper.succeed();
        } finally {
            cleanupPlayer(level, active, conn.channel());
            restoreDefaultConfig();
        }
    }

    @GameTest(template = EMPTY_TEMPLATE, templateNamespace = KeepButPenaltyE2EMod.MOD_ID)
    public static void hungerPreservationPreventsSuicideRefill(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(false, level.getServer());
        restoreDefaultConfig();

        ConnectedPlayer conn = spawnRealPlayer(level, "E2EHunger");
        ServerPlayer active = conn.player();
        try {
            active.getFoodData().setFoodLevel(11);
            active.getFoodData().setSaturation(8.0f);

            active.hurt(level.damageSources().genericKill(), 1000.0f);
            active = level.getServer().getPlayerList().respawn(active, false, Entity.RemovalReason.KILLED);

            assertEq(helper, 11, active.getFoodData().getFoodLevel(),
                    "Mid-level hunger (11) must be preserved, never refilled to 20");
            assertEq(helper, 8.0f, active.getFoodData().getSaturationLevel(),
                    "Saturation must be preserved");
            helper.succeed();
        } finally {
            cleanupPlayer(level, active, conn.channel());
            restoreDefaultConfig();
        }
    }

    @GameTest(template = EMPTY_TEMPLATE, templateNamespace = KeepButPenaltyE2EMod.MOD_ID)
    public static void progressiveConsecutiveDeathDebuffs(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(false, level.getServer());
        writeAndReloadConfig("""
                [death]
                keepInventory = true
                enableExperiencePenalty = true
                experienceKeepRatio = 0.75
                preserveHungerOnDeath = true
                minRespawnFoodLevel = 6
                skipOnPvp = false
                respawnDebuffs = ["minecraft:weakness,60,1", "minecraft:mining_fatigue,60,1", "minecraft:slowness,60,1"]
                respawnDebuffDurationsSecondsByDeath = [0, 30, 60]
                respawnDebuffStreakResetSeconds = 600
                showRespawnSummary = true

                [durability]
                enableDurabilityPenalty = true
                penaltyMode = "PERCENT_MAX"
                durabilityPercent = 0.1
                durabilityFlat = 80
                allowBreak = false
                """);

        ConnectedPlayer conn = spawnRealPlayer(level, "E2EDebuffs");
        ServerPlayer active = conn.player();
        try {
            // Death 1 -> schedule[0] = 0s -> no debuffs
            active.hurt(level.damageSources().genericKill(), 1000.0f);
            active = level.getServer().getPlayerList().respawn(active, false, Entity.RemovalReason.KILLED);
            assertTrue(helper, !active.hasEffect(MobEffects.WEAKNESS), "Death 1 must not apply Weakness");
            assertTrue(helper, !active.hasEffect(MobEffects.DIG_SLOWDOWN), "Death 1 must not apply Mining Fatigue");
            assertTrue(helper, !active.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Death 1 must not apply Slowness");

            // Death 2 -> schedule[1] = 30s (600 ticks) -> all 3 debuffs applied
            active.hurt(level.damageSources().genericKill(), 1000.0f);
            active = level.getServer().getPlayerList().respawn(active, false, Entity.RemovalReason.KILLED);
            MobEffectInstance w2 = active.getEffect(MobEffects.WEAKNESS);
            MobEffectInstance f2 = active.getEffect(MobEffects.DIG_SLOWDOWN);
            MobEffectInstance s2 = active.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            assertTrue(helper, w2 != null && w2.getDuration() == 600, "Death 2 Weakness must be 600 ticks");
            assertTrue(helper, f2 != null && f2.getDuration() == 600, "Death 2 Mining Fatigue must be 600 ticks");
            assertTrue(helper, s2 != null && s2.getDuration() == 600, "Death 2 Slowness must be 600 ticks");

            // Death 3 -> schedule[2] = 60s (1200 ticks) -> progressive escalation
            active.hurt(level.damageSources().genericKill(), 1000.0f);
            active = level.getServer().getPlayerList().respawn(active, false, Entity.RemovalReason.KILLED);
            MobEffectInstance w3 = active.getEffect(MobEffects.WEAKNESS);
            MobEffectInstance f3 = active.getEffect(MobEffects.DIG_SLOWDOWN);
            MobEffectInstance s3 = active.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            assertTrue(helper, w3 != null && w3.getDuration() == 1200, "Death 3 Weakness must be 1200 ticks");
            assertTrue(helper, f3 != null && f3.getDuration() == 1200, "Death 3 Mining Fatigue must be 1200 ticks");
            assertTrue(helper, s3 != null && s3.getDuration() == 1200, "Death 3 Slowness must be 1200 ticks");

            helper.succeed();
        } finally {
            cleanupPlayer(level, active, conn.channel());
            restoreDefaultConfig();
        }
    }

    @GameTest(template = EMPTY_TEMPLATE, templateNamespace = KeepButPenaltyE2EMod.MOD_ID)
    public static void totemOfUndyingNeverAppliesPenalty(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(false, level.getServer());
        restoreDefaultConfig();

        ConnectedPlayer conn = spawnRealPlayer(level, "E2ETotem");
        ServerPlayer active = conn.player();
        try {
            active.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
            active.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            active.experienceLevel = 30;
            active.experienceProgress = 0.0f;
            active.totalExperience = 1395;
            active.getFoodData().setFoodLevel(10);

            // Expire vanilla 60-tick post-login spawn invulnerability so non-bypass damage lands cleanly
            for (int t = 0; t < 65; t++) {
                active.tick();
            }

            // Lethal damage that does NOT bypass invulnerability -> Totem of Undying activates!
            boolean damaged = active.hurt(level.damageSources().magic(), 1000.0f);
            assertTrue(helper, damaged, "Lethal magic damage must register on player to trigger Totem");

            assertTrue(helper, active.isAlive(), "Player holding Totem of Undying must survive lethal damage");
            assertTrue(helper, active.getOffhandItem().isEmpty(), "Totem of Undying must be consumed");
            assertEq(helper, 0, active.getItemBySlot(EquipmentSlot.HEAD).getDamageValue(),
                    "Totem save must NOT apply durability penalty");
            assertEq(helper, 30, active.experienceLevel, "Totem save must NOT apply XP level penalty");
            assertEq(helper, 1395,
                    ExperiencePenaltyPolicy.totalExperience(active.experienceLevel, active.experienceProgress),
                    "Totem save must NOT apply total XP penalty");
            assertTrue(helper, !level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY),
                    "keepInventory gamerule must remain false");
            helper.succeed();
        } finally {
            cleanupPlayer(level, active, conn.channel());
            restoreDefaultConfig();
        }
    }

    @GameTest(template = EMPTY_TEMPLATE, templateNamespace = KeepButPenaltyE2EMod.MOD_ID)
    public static void cancelledDeathEventNeverAppliesPenalty(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(false, level.getServer());
        restoreDefaultConfig();

        ConnectedPlayer conn = spawnRealPlayer(level, "E2ECancelDeath");
        ServerPlayer active = conn.player();
        UUID targetUuid = active.getUUID();
        Consumer<LivingDeathEvent> listener = event -> {
            if (event.getEntity().getUUID().equals(targetUuid)) {
                event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            active.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
            active.experienceLevel = 30;
            active.experienceProgress = 0.0f;
            active.totalExperience = 1395;
            active.getFoodData().setFoodLevel(10);

            // Direct ServerPlayer.die() -> NeoForge LivingDeathEvent cancels it before TAIL!
            active.die(level.damageSources().genericKill());

            assertEq(helper, 0, active.getItemBySlot(EquipmentSlot.HEAD).getDamageValue(),
                    "Cancelled LivingDeathEvent must NOT apply durability penalty");
            assertEq(helper, 30, active.experienceLevel,
                    "Cancelled LivingDeathEvent must NOT apply XP penalty");
            assertEq(helper, 1395,
                    ExperiencePenaltyPolicy.totalExperience(active.experienceLevel, active.experienceProgress),
                    "Cancelled LivingDeathEvent must preserve total XP");
            assertTrue(helper, !level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY),
                    "KeepInventoryProjection must unwind cleanly even when die() returns early");
            helper.succeed();
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
            cleanupPlayer(level, active, conn.channel());
            restoreDefaultConfig();
        }
    }

    @GameTest(template = EMPTY_TEMPLATE, templateNamespace = KeepButPenaltyE2EMod.MOD_ID)
    public static void inventoryToolsPenalizedOnceAndRespectExclusions(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(false, level.getServer());
        writeAndReloadConfig("""
                [death]
                keepInventory = true
                enableExperiencePenalty = false

                [durability]
                enableDurabilityPenalty = true
                penaltyMode = "PERCENT_MAX"
                durabilityPercent = 0.1
                allowBreak = false
                damageArmor = true
                damageMainHand = true
                damageOffHand = true
                damageInventory = true
                excludedItems = ["#keep_but_penalty:excluded", "minecraft:diamond_shovel"]
                """);

        ConnectedPlayer conn = spawnRealPlayer(level, "E2EInvDamage");
        ServerPlayer active = conn.player();
        try {
            ItemStack mainHandSword = new ItemStack(Items.DIAMOND_SWORD);
            ItemStack hotbarPick = new ItemStack(Items.DIAMOND_PICKAXE);
            ItemStack bagAxe = new ItemStack(Items.DIAMOND_AXE);
            ItemStack excludedShovel = new ItemStack(Items.DIAMOND_SHOVEL);
            ItemStack food = new ItemStack(Items.COOKED_BEEF, 16);

            active.getInventory().selected = 0;
            active.getInventory().setItem(0, mainHandSword);
            active.getInventory().setItem(3, hotbarPick);
            active.getInventory().setItem(12, bagAxe);
            active.getInventory().setItem(13, excludedShovel);
            active.getInventory().setItem(14, food);

            active.hurt(level.damageSources().genericKill(), 1000.0f);
            active = level.getServer().getPlayerList().respawn(active, false, Entity.RemovalReason.KILLED);

            int expected10Pct = (int) Math.round(mainHandSword.getMaxDamage() * 0.10D);
            assertEq(helper, expected10Pct, active.getInventory().getItem(0).getDamageValue(),
                    "Active main-hand sword must be damaged exactly once (never double-damaged)");
            assertEq(helper, expected10Pct, active.getInventory().getItem(3).getDamageValue(),
                    "Non-held hotbar pickaxe must be penalized when damageInventory=true");
            assertEq(helper, expected10Pct, active.getInventory().getItem(12).getDamageValue(),
                    "Main inventory axe must be penalized when damageInventory=true");
            assertEq(helper, 0, active.getInventory().getItem(13).getDamageValue(),
                    "Excluded inventory item must remain untouched");
            assertEq(helper, 16, active.getInventory().getItem(14).getCount(),
                    "Non-damageable food stack must remain untouched");

            // Now toggle damageInventory = false and verify non-held tools take zero additional damage
            writeAndReloadConfig("""
                    [death]
                    keepInventory = true
                    enableExperiencePenalty = false

                    [durability]
                    enableDurabilityPenalty = true
                    penaltyMode = "PERCENT_MAX"
                    durabilityPercent = 0.1
                    allowBreak = false
                    damageArmor = true
                    damageMainHand = true
                    damageOffHand = true
                    damageInventory = false
                    excludedItems = ["#keep_but_penalty:excluded", "minecraft:diamond_shovel"]
                    """);

            active.getInventory().selected = 1; // empty hand
            active.hurt(level.damageSources().genericKill(), 1000.0f);
            active = level.getServer().getPlayerList().respawn(active, false, Entity.RemovalReason.KILLED);

            assertEq(helper, expected10Pct, active.getInventory().getItem(0).getDamageValue(),
                    "Unheld sword must not take extra damage when damageInventory=false");
            assertEq(helper, expected10Pct, active.getInventory().getItem(3).getDamageValue(),
                    "Unheld hotbar pickaxe must not take extra damage when damageInventory=false");
            assertEq(helper, expected10Pct, active.getInventory().getItem(12).getDamageValue(),
                    "Unheld bag axe must not take extra damage when damageInventory=false");
            helper.succeed();
        } finally {
            cleanupPlayer(level, active, conn.channel());
            restoreDefaultConfig();
        }
    }
}
