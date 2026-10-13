package com.moigferdsrte.ragdoll.physics;

import com.moigferdsrte.ragdoll.RagdollSeatCallbacks;
import com.moigferdsrte.ragdoll.SablePlayerRagdoll;
import com.moigferdsrte.ragdoll.api.RagdollEndEvent;
import com.moigferdsrte.ragdoll.entity.RagdollSeatEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import java.util.UUID;
import java.util.Set;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.moigferdsrte.ragdoll.fabric.RagdollFabricEvents;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public final class RagdollExpireHelper {
   private RagdollExpireHelper() {
   }

   static void unseatPlayerSilently(ServerLevel level, UUID playerId) {
      Entity entity = level.getEntity(playerId);
      if (!(entity instanceof LivingEntity livingEntity)) return;
      if (livingEntity.isPassenger()) livingEntity.stopRiding();
      RagdollSeatingHelper.restoreVisibility(livingEntity);
   }

   public static void expire(ServerLevel level, ServerSubLevel subLevel, String reason) {
      if (subLevel.isRemoved()) return;
      if (!RagdollSessionManager.isExpiring(subLevel)) {
         RagdollSessionManager.markExpiring(subLevel, reason);
         unseatRider(level, subLevel);
         discardSeatEntities(level, subLevel);
         SablePlayerRagdoll.LOGGER.info("[sable_player_ragdoll] expiring ragdoll {} ({})", RagdollRegistry.shortId(RagdollBlockOwnership.sessionId(subLevel)), reason);
      }
      RagdollSessionManager.unregister(subLevel);
      RagdollDeferredSync.cancel(RagdollBlockOwnership.sessionId(subLevel));
   }

   private static void unseatRider(ServerLevel level, ServerSubLevel subLevel) {
      UUID playerId = RagdollSessionManager.getPlayerId(subLevel);
      if (playerId != null) {
         releaseRider(level, subLevel, playerId);
      }
   }

   static void releaseFailedLaunch(ServerLevel level, ServerSubLevel subLevel, @Nullable UUID seatEntityId) {
      if (seatEntityId != null) {
         releaseRider(level, subLevel, seatEntityId);
      }
      discardSeatEntities(level, subLevel);
   }

   private static void releaseRider(ServerLevel level, ServerSubLevel subLevel, UUID riderId) {
      Entity entity = level.getEntity(riderId);
      if (entity instanceof LivingEntity livingEntity) {
         if (livingEntity.getVehicle() != null) {
            var seatTag = com.moigferdsrte.ragdoll.fabric.FabricEntityData.get(livingEntity.getVehicle());
            UUID sessionId = seatTag.read(RagdollBlockLifetime.SOURCE_SESSION, UUIDUtil.CODEC).orElse(null);
            if (sessionId != null && !RagdollBlockOwnership.sessionId(subLevel).equals(sessionId)) return;
         }
         Vec3 releasePosition = releasePosition(level, subLevel);
         Vec3 inheritedVelocity = sublevelVelocityAsBlocksPerTick(level, subLevel);
         Vec3 exitVelocity = inheritedVelocity == null ? Vec3.ZERO : inheritedVelocity;
         if (livingEntity.isPassenger()) {
            livingEntity.stopRiding();
         }
         if (releasePosition != null) {
            if (livingEntity instanceof ServerPlayer player) {
               player.teleportTo(level, releasePosition.x, releasePosition.y, releasePosition.z, Set.of(), player.getYRot(), player.getXRot(), false);
            } else {
               livingEntity.teleportTo(releasePosition.x, releasePosition.y, releasePosition.z);
            }
         }
         if (exitVelocity != Vec3.ZERO) {
            livingEntity.setDeltaMovement(exitVelocity);
         }
         RagdollSeatingHelper.restoreVisibility(livingEntity);
         if (livingEntity instanceof ServerPlayer player) {
            RagdollRegistry.suppressAfterRelease(player.getUUID(), level.getGameTime());
            RagdollSeatCallbacks.notifyReleased(player);
            RagdollFabricEvents.fire(new RagdollEndEvent(player, exitVelocity, endReason(subLevel)));
         }
      }
   }

   private static RagdollEndEvent.Reason endReason(ServerSubLevel subLevel) {
      String reason = RagdollSessionManager.getEndReason(subLevel);
      if ("player died".equals(reason)) return RagdollEndEvent.Reason.PLAYER_DEATH;
      if ("player disconnected".equals(reason)) return RagdollEndEvent.Reason.PLAYER_LOGOUT;
      if (reason != null && reason.startsWith("api")) return RagdollEndEvent.Reason.RELEASED;
      return RagdollEndEvent.Reason.EXPIRED;
   }

   @Nullable
   private static Vec3 releasePosition(ServerLevel level, ServerSubLevel rootSubLevel) {
      var torso = RagdollBlockOwnership.findLimb(level, RagdollBlockOwnership.sessionId(rootSubLevel));
      if (torso == null) return null;
      return Sable.HELPER.projectOutOfSubLevel(level, Vec3.atCenterOf(torso.getBlockPos())).add(0.0, 0.5, 0.0);
   }

   @Nullable
   private static Vec3 sublevelVelocityAsBlocksPerTick(ServerLevel level, ServerSubLevel rootSubLevel) {
      SubLevelPhysicsSystem physicsSystem = SubLevelPhysicsSystem.get(level);
      if (physicsSystem == null || rootSubLevel.isRemoved()) return null;
      RigidBodyHandle handle = physicsSystem.getPhysicsHandle(rootSubLevel);
      if (handle == null || !handle.isValid()) return null;
      Vector3d vel = handle.getLinearVelocity(new Vector3d());
      return new Vec3(vel.x / 20.0, vel.y / 20.0, vel.z / 20.0);
   }

   private static void discardSeatEntities(ServerLevel level, ServerSubLevel subLevel) {
      AABB bounds = plotBounds(subLevel);
      if (bounds != null) {
         for (RagdollSeatEntity seat : level.getEntitiesOfClass(RagdollSeatEntity.class, bounds)) {
            var seatTag = com.moigferdsrte.ragdoll.fabric.FabricEntityData.get(seat);
            if (!RagdollBlockOwnership.sessionId(subLevel).equals(
                  seatTag.read(RagdollBlockLifetime.SOURCE_SESSION, UUIDUtil.CODEC).orElse(null))) continue;
            seat.ejectPassengers();
            seat.discard();
         }
      }
   }

   @Nullable
   private static AABB plotBounds(ServerSubLevel subLevel) {
      ServerLevelPlot plot = subLevel.getPlot();
      if (plot == null) return null;
      BoundingBox3ic box = plot.getBoundingBox();
      return new AABB(
         (double) box.minX(), (double) box.minY(), (double) box.minZ(),
         (double) box.maxX() + 1.0, (double) box.maxY() + 1.0, (double) box.maxZ() + 1.0
      );
   }
}
