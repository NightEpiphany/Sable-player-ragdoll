package com.moigferdsrte.ragdoll.fabric;

import com.moigferdsrte.ragdoll.SablePlayerRagdoll;
import com.moigferdsrte.ragdoll.SablePlayerRagdollBootstrap;
import com.moigferdsrte.ragdoll.RagdollItemTags;
import com.moigferdsrte.ragdoll.RagdollGrabCallbacks;
import com.moigferdsrte.ragdoll.RagdollPoseRequestCallbacks;
import com.moigferdsrte.ragdoll.RagdollSeatCallbacks;
import com.moigferdsrte.ragdoll.api.RagdollAPI;
import com.moigferdsrte.ragdoll.api.RagdollAsyncPoseRequests;
import com.moigferdsrte.ragdoll.fabric.network.FabricNetwork;
import com.moigferdsrte.ragdoll.fabric.network.RagdollNetworking;
import com.moigferdsrte.ragdoll.mob.MobRagdollAssembly;
import com.moigferdsrte.ragdoll.physics.RagdollAssemblyHelper;
import com.moigferdsrte.ragdoll.physics.RagdollBlockOwnership;
import com.moigferdsrte.ragdoll.physics.RagdollExpireHelper;
import com.moigferdsrte.ragdoll.physics.RagdollSeatingHelper;
import com.moigferdsrte.ragdoll.physics.RagdollRelationships;
import com.moigferdsrte.ragdoll.physics.RagdollRegistry;
import com.moigferdsrte.ragdoll.physics.RagdollSessionManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.api.ModInitializer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import com.moigferdsrte.ragdoll.block.entity.RagdollPartBlockEntity;

public class SablePlayerRagdollFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricRegistries.register();
        FabricNetwork.register();
        FabricConfig.load();
        RagdollSeatCallbacks.setOnReleased(RagdollNetworking::notifyReleased);
        RagdollPoseRequestCallbacks.setOnRequestPose(RagdollNetworking::notifyRequestPose);
        RagdollGrabCallbacks.setOnGrabbed(RagdollNetworking::notifyGrabStarted);
        RagdollGrabCallbacks.setOnReleased(RagdollNetworking::notifyGrabEnded);
        SablePlayerRagdollBootstrap.init();
        registerServerCallbacks();
        registerCommands();
        com.moigferdsrte.ragdoll.fabric.event.RagdollInteractions.register();
        SablePlayerRagdoll.LOGGER.info("[sable_player_ragdoll] Fabric initialization complete");
    }

    private static void registerServerCallbacks() {
        ServerTickEvents.END_LEVEL_TICK.register(level -> {
            RagdollRelationships.discover(level);
            MobRagdollAssembly.tickActiveRagdolls(level);
        });
        ServerTickEvents.END_SERVER_TICK.register(server ->
                RagdollAsyncPoseRequests.tick(uuid -> server.getPlayerList().getPlayer(uuid)));
        ServerTickEvents.END_SERVER_TICK.register(server -> FabricConfig.tick(server.getTickCount()));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> FabricConfig.resetPolling());
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            RagdollRegistry.resetState();
            MobRagdollAssembly.resetRuntimeState();
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof LivingEntity living) {
                MobRagdollAssembly.hideLoadedRagdollSource(level, living, FabricEntityData.wasLoaded(living));
            }
        });
        EntityTrackingEvents.START_TRACKING.register((entity, player) ->
                MobRagdollAssembly.syncClientSourceState(player, entity));
        ServerEntityEvents.EQUIPMENT_CHANGE.register((entity, slot, from, to) -> {
            if (!(entity instanceof ServerPlayer player)) return;
            ServerLevel level = player.level();
            ServerSubLevel ragdoll = RagdollSessionManager.activeRagdollForPlayer(level, player.getUUID());
            if (ragdoll == null) return;
            for (java.util.UUID partId : RagdollAssemblyHelper.linkedParts(level, ragdoll.getUniqueId())) {
                SubLevel partLevel = SubLevelContainer.getContainer(level).getSubLevel(partId);
                if (partLevel == null) continue;
                var pos = partLevel.getPlot().getCenterBlock();
                if (partLevel.getLevel().getBlockEntity(pos) instanceof RagdollPartBlockEntity part) {
                    part.setItemForSlot(slot, to);
                    part.getLevel().sendBlockUpdated(pos, part.getBlockState(), part.getBlockState(), 3);
                }
            }
        });
        ServerLivingEntityEvents.AFTER_DEATH.register(SablePlayerRagdollFabric::onLivingDeath);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                RagdollSeatingHelper.unseatOnLogout(handler.player));
        AttackEntityCallback.EVENT.register((player, level, hand, target, hit) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer attacker) || !(target instanceof LivingEntity living)) {
                return InteractionResult.PASS;
            }
            if (RagdollAPI.isRagdolled(attacker)) return InteractionResult.FAIL;
            var weapon = attacker.getItemInHand(hand);
            if (!RagdollItemTags.canRagdollOnHit(weapon)
                    || (RagdollItemTags.requiresCriticalHit(weapon) && !isCriticalHit(attacker))) {
                return InteractionResult.PASS;
            }
            if (living instanceof ServerPlayer targetPlayer) {
                com.moigferdsrte.ragdoll.physics.RagdollRegistry.triggerWeaponHit(attacker, targetPlayer);
            } else {
                RagdollAPI.launchMob(level.getServer().getLevel(level.dimension()), living,
                        com.moigferdsrte.ragdoll.fabric.event.RagdollInteractions.weaponLaunch(attacker.getLookAngle()),
                        net.minecraft.world.phys.Vec3.ZERO,
                        com.moigferdsrte.ragdoll.mob.api.MobRagdollLaunchOptions.builder()
                                .durationTicks(100).wailing(com.moigferdsrte.ragdoll.api.RagdollWailingOptions.defaults()).build());
            }
            return InteractionResult.PASS;
        });
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            return RagdollSessionManager.activeRagdollForPlayer(serverPlayer.level(), serverPlayer.getUUID()) != null
                    ? InteractionResult.FAIL : InteractionResult.PASS;
        });
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                    && RagdollSessionManager.activeRagdollForPlayer(serverPlayer.level(), serverPlayer.getUUID()) != null) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }

    private static void onLivingDeath(LivingEntity entity, DamageSource source) {
        if (entity instanceof ServerPlayer player) {
            ServerLevel level = player.level();
            RagdollBlockOwnership.withOwner(RagdollBlockOwnership.ownerForPlayer(level, player.getUUID()), () -> {
                ServerSubLevel ragdoll = RagdollSessionManager.activeRagdollForPlayer(level, player.getUUID());
                if (ragdoll != null) RagdollExpireHelper.expire(level, ragdoll, "player died");
            });
            return;
        }
        if (!(entity instanceof Mob mob) || !(source.getEntity() instanceof ServerPlayer killer)
                || killer instanceof net.fabricmc.fabric.api.entity.FakePlayer) return;
        if (!(mob.level() instanceof ServerLevel level)) return;
        Vec3 away = mob.position().subtract(killer.position());
        if (away.lengthSqr() < 1.0E-6) away = killer.getLookAngle();
        Vec3 impulse = away.normalize().scale(4.5);
        Vec3 rotation = new Vec3(level.getRandom().nextDouble() * 4.0 - 2.0,
                level.getRandom().nextDouble() * 4.0 - 2.0, level.getRandom().nextDouble() * 4.0 - 2.0);
        RagdollAPI.launchMob(level, mob, mob.getDeltaMovement().add(impulse), rotation,
                com.moigferdsrte.ragdoll.mob.api.MobRagdollLaunchOptions.defaults());
    }

    private static boolean isCriticalHit(ServerPlayer attacker) {
        return attacker.fallDistance > 0.0F
                && !attacker.onGround()
                && !attacker.onClimbable()
                && !attacker.isInWater()
                && !attacker.hasEffect(MobEffects.BLINDNESS)
                && !attacker.isPassenger()
                && !attacker.isSprinting();
    }

    private static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> RagdollCommands.register(dispatcher));
    }
}
