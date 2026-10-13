package com.moigferdsrte.ragdoll.fabric;

import com.moigferdsrte.ragdoll.RagdollItemTags;
import com.moigferdsrte.ragdoll.api.PlayerlessDespawnRule;
import com.moigferdsrte.ragdoll.api.RagdollAPI;
import com.moigferdsrte.ragdoll.api.RagdollLimbOptions;
import com.moigferdsrte.ragdoll.api.RagdollLaunchOptions;
import com.moigferdsrte.ragdoll.api.RagdollPoseSnapshot;
import com.moigferdsrte.ragdoll.mob.MobRagdollSourceRecovery;
import com.moigferdsrte.ragdoll.physics.RagdollAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.CrashReport;
import net.minecraft.ReportType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class RagdollGameTests implements FabricGameTest {
    @GameTest(template = EMPTY_STRUCTURE)
    public void persistentEntityDataRoundTrip(GameTestHelper helper) {
        var pig = helper.spawn(EntityType.PIG, new BlockPos(2, 2, 2));
        helper.assertTrue(FabricEntityData.peek(pig) == null, "Ordinary entities should not allocate ragdoll data");
        FabricEntityData.get(pig).putString("test", "saved");
        var saved = pig.saveWithoutId(new CompoundTag());
        var restored = EntityType.PIG.create(helper.getLevel());
        restored.load(saved);
        helper.assertTrue(FabricEntityData.wasLoaded(restored), "Load flag should survive the mixin");
        helper.assertTrue("saved".equals(FabricEntityData.get(restored).getString("test")), "Data should survive save/load");
        FabricEntityData.get(pig).putString("test", "changed");
        helper.assertTrue("saved".equals(FabricEntityData.get(restored).getString("test")), "Data must not alias across entities");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void hiddenMobRestoresOriginalFlags(GameTestHelper helper) {
        var pig = helper.spawn(EntityType.PIG, new BlockPos(2, 2, 2));
        UUID session = UUID.randomUUID();
        MobRagdollSourceRecovery.begin(pig, session, helper.getLevel().getGameTime() + 100);
        pig.setInvisible(true);
        pig.setNoAi(true);
        pig.noPhysics = true;
        helper.assertTrue(MobRagdollSourceRecovery.active(pig), "Recovery record should be active");
        helper.assertFalse(pig.isAttackable(), "Hidden source must reject direct attacks");
        helper.assertTrue(pig.getBoundingBox().getXsize() < .01, "Hidden source must have a tiny hitbox");
        MobRagdollSourceRecovery.restore(pig);
        helper.assertFalse(pig.isInvisible() || pig.isNoAi() || pig.noPhysics, "Original entity flags should be restored");
        helper.assertFalse(MobRagdollSourceRecovery.hasRecord(pig), "Recovery record should be cleared");
        helper.assertTrue(pig.getBoundingBox().getXsize() > .1, "Normal hitbox should return");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void weaponMarkersAndDataPackTags(GameTestHelper helper) {
        var ordinary = new ItemStack(Items.STICK);
        helper.assertFalse(RagdollItemTags.canRagdollOnHit(ordinary), "Ordinary stick must not launch entities");
        RagdollItemTags.markTestItem(ordinary);
        helper.assertTrue(RagdollItemTags.canRagdollOnHit(ordinary), "Marked stick should launch entities");
        helper.assertFalse(RagdollItemTags.requiresCriticalHit(ordinary), "Ordinary launch marker should not require a critical hit");
        RagdollItemTags.markTestItemCritOnly(ordinary);
        helper.assertTrue(RagdollItemTags.requiresCriticalHit(ordinary), "Critical marker should be respected");
        helper.assertTrue(RagdollItemTags.canRagdollOnHit(new ItemStack(Items.BLAZE_ROD)), "Data pack launch tag should be loaded");
        helper.assertTrue(RagdollItemTags.requiresCriticalHit(new ItemStack(Items.BREEZE_ROD)), "Data pack critical tag should be loaded");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void configHotReloadIsAtomic(GameTestHelper helper) throws java.io.IOException {
        var path = FabricConfig.path();
        String original = java.nio.file.Files.readString(path);
        int cooldown = com.moigferdsrte.ragdoll.config.RagdollSettings.cooldownTicks();
        boolean enabled = com.moigferdsrte.ragdoll.config.RagdollSettings.enabled();
        try {
            java.nio.file.Files.writeString(path, "{cooldownTicks: 73, enabled: " + enabled + "}");
            java.nio.file.Files.setLastModifiedTime(path, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis() + 2000));
            FabricConfig.resetPolling();
            FabricConfig.tick(0);
            helper.assertTrue(com.moigferdsrte.ragdoll.config.RagdollSettings.cooldownTicks() == 73, "Polling should reload edited JSON5");
            java.nio.file.Files.writeString(path, "{enabled: " + !enabled + ", cooldownTicks: -1}");
            java.nio.file.Files.setLastModifiedTime(path, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis() + 4000));
            FabricConfig.tick(20);
            helper.assertTrue(com.moigferdsrte.ragdoll.config.RagdollSettings.cooldownTicks() == 73
                    && com.moigferdsrte.ragdoll.config.RagdollSettings.enabled() == enabled,
                    "Invalid reload must preserve the entire last valid configuration");
        } finally {
            java.nio.file.Files.writeString(path, original);
            FabricConfig.load();
            FabricConfig.resetPolling();
        }
        helper.assertTrue(com.moigferdsrte.ragdoll.config.RagdollSettings.cooldownTicks() == cooldown, "Original config should be restored");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void canceledLaunchDoesNotCreatePhysics(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        UUID target = player.getUUID();
        RagdollFabricEvents.onPlayerStart(event -> {
            if (event.player() == player) event.setCanceled(true);
        });
        int before = SubLevelContainer.getContainer(helper.getLevel()).getAllSubLevels().size();
        var session = RagdollAPI.launch(player, Vec3.ZERO, RagdollLaunchOptions.defaults(),
                new RagdollPoseSnapshot(RagdollLimbOptions.defaults(), 0));
        helper.assertTrue(session == null && !RagdollAPI.isRagdolled(player), "Canceled start must not seat the player");
        helper.assertTrue(SubLevelContainer.getContainer(helper.getLevel()).getAllSubLevels().size() == before,
                "Canceled start must not allocate physics bodies");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 160)
    public void playerlessPhysicsCreatesAndCleansUp(GameTestHelper helper) {
        var level = helper.getLevel();
        var session = RagdollAPI.spawnPlayerless(level, helper.absoluteVec(new Vec3(3, 3, 3)), 0,
                Vec3.ZERO, PlayerlessDespawnRule.never());
        helper.assertTrue(session != null, "Playerless ragdoll should be created on a dedicated server");
        var container = SubLevelContainer.getContainer(level);
        helper.assertTrue(container.getSubLevel(session.id()) != null, "Root physics body should be registered");
        var parts = java.util.List.copyOf(RagdollAssemblyHelper.linkedParts(level, session.id()));
        helper.assertTrue(parts.size() >= 6, "All player limbs should be assembled");
        helper.runAfterDelay(5, session::release);
        // Creation and orphan recovery each allow 40 ticks, with staggered policy checks.
        helper.runAfterDelay(120, () -> {
            for (var id : parts) helper.assertTrue(container.getSubLevel(id) == null, "Released limbs must be removed");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
    public void playerLaunchSeatsAndReleases(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        player.moveTo(helper.absoluteVec(new Vec3(3, 3, 3)));
        var session = RagdollAPI.launch(player, Vec3.ZERO, RagdollLaunchOptions.defaults(),
                new RagdollPoseSnapshot(RagdollLimbOptions.defaults(), 0));
        helper.assertTrue(session != null, "Player ragdoll should be created");
        helper.assertTrue(RagdollAPI.isRagdolled(player), "Player should be seated in their ragdoll");
        helper.runAfterDelay(5, session::release);
        helper.runAfterDelay(20, () -> {
            helper.assertFalse(RagdollAPI.isRagdolled(player), "Release must clear the active player session");
            helper.assertFalse(player.isInvisible(), "Release must restore player visibility");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void crashReportIncludesDiagnostics(GameTestHelper helper) {
        var report = new CrashReport("Integration test", new IllegalStateException("test"));
        String first = report.getFriendlyReport(ReportType.CRASH);
        String second = report.getFriendlyReport(ReportType.CRASH);
        helper.assertTrue(first.contains("PlayerRagdoll Fabric") && first.contains("sable_player_ragdoll.json5"), "Crash report should include ragdoll diagnostics");
        helper.assertTrue(second.indexOf("PlayerRagdoll Fabric") == second.lastIndexOf("PlayerRagdoll Fabric"), "Diagnostics should only be appended once");
        helper.succeed();
    }
}
