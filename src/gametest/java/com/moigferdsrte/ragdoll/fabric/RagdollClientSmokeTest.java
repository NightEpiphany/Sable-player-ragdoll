package com.moigferdsrte.ragdoll.fabric;

import com.moigferdsrte.ragdoll.SablePlayerRagdoll;
import com.moigferdsrte.ragdoll.api.PlayerlessDespawnRule;
import com.moigferdsrte.ragdoll.api.PlayerlessRagdollSession;
import com.moigferdsrte.ragdoll.api.RagdollAPI;
import com.moigferdsrte.ragdoll.api.RagdollLimbOptions;
import com.moigferdsrte.ragdoll.entity.RagdollSeatEntity;
import com.moigferdsrte.ragdoll.fabric.network.RagdollTriggerPacket;
import com.moigferdsrte.ragdoll.mob.MobRagdollSourceRecovery;
import com.moigferdsrte.ragdoll.mob.api.MobRagdollLaunchOptions;
import com.moigferdsrte.ragdoll.mob.client.MobRagdollClientState;
import com.moigferdsrte.ragdoll.physics.RagdollBlockOwnership;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import java.nio.file.Files;
import java.util.UUID;

/** Opt-in test mod: creates an isolated flat world and exercises actual client/server packets. */
public final class RagdollClientSmokeTest implements ClientModInitializer {
    private int phase;
    private int ticks;
    private final long started = System.nanoTime();
    private volatile boolean setupReady;
    private volatile boolean verificationDone;
    private volatile Throwable failure;
    private volatile int pigId;
    private Pig pig;
    private PlayerlessRagdollSession corpse;
    private UUID playerId;

    @Override
    public void onInitializeClient() {
        if (Boolean.getBoolean("ragdoll.smoke")) ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private void onServer(Minecraft client, Runnable action) {
        client.getSingleplayerServer().execute(() -> {
            try { action.run(); } catch (Throwable error) { failure = error; }
        });
    }

    private void tick(Minecraft client) {
        if (phase == 5) return;
        try {
            if (failure != null) throw new IllegalStateException("Server smoke test failed", failure);
            require(System.nanoTime() - started < 240_000_000_000L, "Client smoke test timed out");
            if (phase == 0) {
                if (client.screen == null || client.getOverlay() != null) return;
                phase = 1;
                client.options.renderDistance().set(6);
                client.options.simulationDistance().set(5);
                var settings = new LevelSettings("Ragdoll Fabric smoke", GameType.CREATIVE, false,
                        Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                client.createWorldOpenFlows().createFreshLevel("ragdoll-smoke-" + System.currentTimeMillis(),
                        settings, new WorldOptions(1234L, false, false),
                        access -> access.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions(),
                        new TitleScreen());
                return;
            }
            if (client.player == null || client.level == null || client.getSingleplayerServer() == null) return;
            ticks++;
            if (phase == 1 && ticks >= 60) {
                playerId = client.player.getUUID();
                phase = 2;
                ticks = 0;
                onServer(client, () -> {
                    var server = client.getSingleplayerServer();
                    var player = server.getPlayerList().getPlayer(playerId);
                    var level = player.serverLevel();
                    player.teleportTo(level, 0, -60, 0, 0, 0);
                    player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                    player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
                    pig = EntityType.PIG.create(level);
                    pig.moveTo(2, -60, 3);
                    pig.setNoAi(true);
                    level.addFreshEntity(pig);
                    pigId = pig.getId();
                    corpse = RagdollAPI.spawnPlayerless(level, new Vec3(0, -60, 3), 0, Vec3.ZERO, PlayerlessDespawnRule.never());
                    require(corpse != null, "Playerless corpse failed to spawn");
                    setupReady = true;
                });
            } else if (phase == 2 && setupReady && ticks >= 60 && client.level.getEntity(pigId) != null) {
                client.setScreen(null);
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                ClientPlayNetworking.send(new RagdollTriggerPacket(RagdollLimbOptions.defaults(), 0));
                onServer(client, () -> require(RagdollAPI.launchMob((net.minecraft.server.level.ServerLevel) pig.level(), pig, Vec3.ZERO, Vec3.ZERO,
                        MobRagdollLaunchOptions.builder().durationTicks(600).build()) != null, "Mob request was rejected"));
                phase = 3;
                ticks = 0;
            } else if (phase == 3 && ticks >= 120) {
                require(client.player.getVehicle() instanceof RagdollSeatEntity, "Player seating packet did not reach client");
                require(MobRagdollClientState.isHidden(client.level.getEntity(pigId)), "Mob geometry request/response did not complete");
                Screenshot.grab(client.gameDirectory, "ragdoll-active.png", client.getMainRenderTarget(),
                        message -> SablePlayerRagdoll.LOGGER.info("Smoke screenshot: {}", message.getString()));
                onServer(client, () -> {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayer(playerId);
                    require(RagdollAPI.isRagdolled(player), "Server player is not ragdolled");
                    require(MobRagdollSourceRecovery.active(pig), "Mob was not converted on server");
                    require(RagdollBlockOwnership.loadedBlocks(player.serverLevel()).size() >= 18, "Expected player, corpse and mob parts");
                    com.moigferdsrte.ragdoll.physics.RagdollExpireHelper.expire(player.serverLevel(),
                            com.moigferdsrte.ragdoll.physics.RagdollSessionManager.activeRagdollForPlayer(player.serverLevel(), playerId), "api smoke release");
                    RagdollAPI.releaseMob(pig);
                    corpse.release();
                });
                phase = 4;
                ticks = 0;
            } else if (phase == 4 && ticks >= 140) {
                require(!(client.player.getVehicle() instanceof RagdollSeatEntity), "Client seat did not clear");
                require(!client.player.isInvisible(), "Client visibility did not restore");
                if (!verificationDone) {
                    onServer(client, () -> {
                        require(!MobRagdollSourceRecovery.active(pig) && !pig.isInvisible(), "Mob failed to recover");
                        require(RagdollBlockOwnership.loadedBlocks((net.minecraft.server.level.ServerLevel) pig.level()).isEmpty(), "Released physics parts remain");
                        verificationDone = true;
                    });
                    return;
                }
                Files.writeString(client.gameDirectory.toPath().resolve("smoke-success.txt"),
                        "PASS: integrated world, manual C2S launch, S2C seating, player/corpse rendering, mob pose round trip, recovery and cleanup\n");
                SablePlayerRagdoll.LOGGER.info("RAGDOLL_CLIENT_SMOKE_PASS");
                phase = 5;
                client.stop();
            }
        } catch (Throwable error) {
            SablePlayerRagdoll.LOGGER.error("RAGDOLL_CLIENT_SMOKE_FAIL", error);
            phase = 5;
            client.stop();
        }
    }
}
