package com.moigferdsrte.ragdoll.fabric.event;
import com.moigferdsrte.ragdoll.physics.RagdollBlockOwnership;
import com.moigferdsrte.ragdoll.DeletingStick;
import com.moigferdsrte.ragdoll.api.RagdollInteractEvent;
import com.moigferdsrte.ragdoll.block.RagdollPartBlock;
import com.moigferdsrte.ragdoll.block.entity.RagdollPartBlockEntity;
import com.moigferdsrte.ragdoll.mob.MobRagdollAssembly;
import com.moigferdsrte.ragdoll.mob.block.MobRagdollPartBlock;
import com.moigferdsrte.ragdoll.mob.block.entity.MobRagdollPartBlockEntity;
import com.moigferdsrte.ragdoll.entity.RagdollSeatEntity;
import com.moigferdsrte.ragdoll.physics.RagdollAssemblyHelper;
import com.moigferdsrte.ragdoll.physics.RagdollSessionManager;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;
import com.moigferdsrte.ragdoll.fabric.RagdollFabricEvents;
public final class RagdollInteractions {
 private RagdollInteractions() {}
    public static void register() {
        net.fabricmc.fabric.api.event.player.AttackBlockCallback.EVENT.register(RagdollInteractions::attackBlock);
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register(RagdollInteractions::useBlock);
        net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register((player, level, hand, target, hit) ->
                isRagdolled(player) ? InteractionResult.FAIL : InteractionResult.PASS);
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) ->
                !(state.getBlock() instanceof RagdollPartBlock) && !(state.getBlock() instanceof MobRagdollPartBlock)
                        && !isRagdolled(player));
        net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack held = player.getItemInHand(hand);
            boolean blocked = isRagdolled(player);
            if (!blocked && level instanceof ServerLevel serverLevel && held.getItem() instanceof BucketItem && !held.is(Items.BUCKET)
                    && player.pick(5.0, 0.0F, true) instanceof BlockHitResult hit) {
                blocked = inPlot(serverLevel, hit.getBlockPos()) || inPlot(serverLevel, hit.getBlockPos().relative(hit.getDirection()));
            }
            return blocked ? InteractionResult.FAIL : InteractionResult.PASS;
        });
    }

    public static Vec3 weaponLaunch(Vec3 look) {
        double length = Math.sqrt(look.x * look.x + look.z * look.z);
        return length > 1.0E-3 ? new Vec3(look.x * 6.0 / length, 8.0, look.z * 6.0 / length) : new Vec3(0.0, 8.0, 6.0);
    }

    private static boolean isRagdolled(net.minecraft.world.entity.player.Player player) {
        return player.getVehicle() instanceof RagdollSeatEntity
                || player instanceof ServerPlayer serverPlayer && RagdollSessionManager.isPlayerCurrentlyRagdolled(serverPlayer);
    }

    private static boolean inPlot(ServerLevel level, BlockPos pos) {
        SubLevel sub = Sable.HELPER.getContaining(level, pos);
        return sub != null && (RagdollAssemblyHelper.isRagdollPart(level, sub.getUniqueId())
                || level.getBlockEntity(pos) instanceof MobRagdollPartBlockEntity);
    }

    private static InteractionResult attackBlock(net.minecraft.world.entity.player.Player player,
            net.minecraft.world.level.Level level, InteractionHand hand, BlockPos pos, net.minecraft.core.Direction direction) {
        if (isRagdolled(player)) return InteractionResult.FAIL;
        Block block = level.getBlockState(pos).getBlock();
        if (!(block instanceof RagdollPartBlock) && !(block instanceof MobRagdollPartBlock)) return InteractionResult.PASS;
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer attacker) {
            SubLevel sub = Sable.HELPER.getContaining(level, pos);
            if (sub != null && DeletingStick.is(attacker.getMainHandItem())) {
                UUID limb = RagdollBlockOwnership.limbAt(serverLevel, pos, sub.getUniqueId());
                if (block instanceof MobRagdollPartBlock) DeletingStick.deleteMob(serverLevel, limb, attacker);
                else DeletingStick.delete(serverLevel, limb, attacker);
            } else if (level.getBlockEntity(pos) instanceof MobRagdollPartBlockEntity part) {
                MobRagdollAssembly.attackPart(serverLevel, part, attacker);
            } else if (sub != null) {
                UUID root = RagdollAssemblyHelper.linkedRoot(serverLevel, sub.getUniqueId());
                if (root != null) pipeAttack(serverLevel, root, attacker);
            }
        }
        // SUCCESS sends the attack packet from the client and prevents block breaking.
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult useBlock(net.minecraft.world.entity.player.Player player,
            net.minecraft.world.level.Level level, InteractionHand hand, BlockHitResult hit) {
        if (isRagdolled(player)) return InteractionResult.FAIL;
        BlockPos pos = hit.getBlockPos();
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer clicker)) {
            return level.getBlockEntity(pos) instanceof RagdollPartBlockEntity
                    || level.getBlockEntity(pos) instanceof MobRagdollPartBlockEntity ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        boolean deleting = DeletingStick.is(clicker.getItemInHand(hand));
        if (level.getBlockEntity(pos) instanceof MobRagdollPartBlockEntity part) {
            SubLevel sub = Sable.HELPER.getContaining(level, pos);
            if (deleting) {
                if (sub != null) DeletingStick.dismemberMob(serverLevel,
                        RagdollBlockOwnership.limbAt(serverLevel, pos, sub.getUniqueId()), clicker);
                return InteractionResult.SUCCESS;
            }
            return MobRagdollAssembly.interactWithPart(serverLevel, part, clicker, hand);
        }
        if (deleting && level.getBlockEntity(pos) instanceof RagdollPartBlockEntity part && part.ragdollIdentity().known()) {
            DeletingStick.dismember(serverLevel, part.ragdollIdentity().limb(), clicker);
            return InteractionResult.SUCCESS;
        }
        BlockPos target = pos.relative(hit.getDirection());
        boolean targetInPlot = inPlot(serverLevel, target);
        if (!targetInPlot && !inPlot(serverLevel, pos)) return InteractionResult.PASS;
        BlockPos ragdollPos = targetInPlot ? target : pos;
        SubLevel sub = Sable.HELPER.getContaining(level, ragdollPos);
        if (sub != null) {
            UUID root = RagdollAssemblyHelper.linkedRoot(serverLevel, sub.getUniqueId());
            if (root != null) {
                if (deleting) DeletingStick.dismember(serverLevel, sub.getUniqueId(), clicker);
                else if (!RagdollFabricEvents.fire(new RagdollInteractEvent(clicker, root, sub.getUniqueId(), ragdollPos, serverLevel)).isCanceled()) {
                    InteractionResult result = pipeInteract(serverLevel, root, clicker, hand);
                    if (result != null) return result;
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.FAIL;
    }
   @org.jetbrains.annotations.Nullable
   private static InteractionResult pipeInteract(ServerLevel level, UUID rootId, ServerPlayer clickingPlayer, InteractionHand hand) {
      SubLevel rootSubLevel = SubLevelContainer.getContainer(level).getSubLevel(rootId);
      if (!(rootSubLevel instanceof ServerSubLevel serverRoot)) return null;
      UUID ragdollPlayerId = RagdollSessionManager.getPlayerId(serverRoot);
      if (ragdollPlayerId == null || ragdollPlayerId.equals(clickingPlayer.getUUID())) return null;
      Entity ragdolledEntity = level.getEntity(ragdollPlayerId);
      if (!(ragdolledEntity instanceof ServerPlayer ragdolledPlayer) || !ragdolledPlayer.isAlive()) return null;
      RagdollSessionManager.RAGDOLL_PIPE_ACTIVE.set(true);
      try {
         return ragdolledPlayer.interact(clickingPlayer, hand, ragdolledPlayer.position());
      } finally {
         RagdollSessionManager.RAGDOLL_PIPE_ACTIVE.set(false);
      }
   }

   private static void pipeAttack(ServerLevel level, UUID rootId, ServerPlayer attacker) {
      SubLevel rootSubLevel = SubLevelContainer.getContainer(level).getSubLevel(rootId);
      if (!(rootSubLevel instanceof ServerSubLevel serverRoot)) return;
      UUID ragdollPlayerId = RagdollSessionManager.getPlayerId(serverRoot);
      // A player riding their own ragdoll can have its parts directly under their crosshair — without this guard,
      // their own clicks pipe through as a self-attack (damage + hurt sound) on themselves.
      if (ragdollPlayerId == null || ragdollPlayerId.equals(attacker.getUUID())) return;
      Entity ragdolledEntity = level.getEntity(ragdollPlayerId);
      if (!(ragdolledEntity instanceof ServerPlayer ragdolledPlayer) || !ragdolledPlayer.isAlive()) return;
      RagdollSessionManager.RAGDOLL_PIPE_ACTIVE.set(true);
      try {
         attacker.attack(ragdolledPlayer);
      } finally {
         RagdollSessionManager.RAGDOLL_PIPE_ACTIVE.set(false);
      }
   }

   public static boolean onProjectileImpact(Projectile projectile, HitResult result) {
      if (!(projectile.level() instanceof ServerLevel level)) return false;
      if (!(result instanceof BlockHitResult hit)) return false;
      BlockPos hitPos = hit.getBlockPos();
      Block hitBlock = level.getBlockState(hitPos).getBlock();
      if (hitBlock instanceof MobRagdollPartBlock) {
         if (pipeMobProjectile(level, hitPos, projectile)) {
            return true;
         }
         return false;
      }
      if (!(hitBlock instanceof RagdollPartBlock)) return false;

      SubLevel subLevel = Sable.HELPER.getContaining(level, hitPos);
      if (subLevel == null) return false;
      UUID rootId = RagdollAssemblyHelper.linkedRoot(level, subLevel.getUniqueId());
      if (rootId == null) return false;

      return pipeProjectile(level, rootId, projectile);
   }

   private static boolean pipeMobProjectile(ServerLevel level, BlockPos hitPos, Projectile projectile) {
      if (!(level.getBlockEntity(hitPos) instanceof MobRagdollPartBlockEntity part)) return false;
      UUID sourceId = part.sourceEntityId();
      if (sourceId == null) return false;
      Entity owner = projectile.getOwner();
      if (owner != null && sourceId.equals(owner.getUUID())) return false;
      Entity sourceEntity = MobRagdollAssembly.sourceEntityForPart(level, hitPos);
      if (!(sourceEntity instanceof LivingEntity living) || !living.isAlive()) return false;

      DamageSource source = projectileDamageSource(level, projectile);
      float damage = projectileDamageAmount(projectile);

      MobRagdollAssembly.RAGDOLL_PIPE_ACTIVE.set(true);
      try {
         boolean hurt = living.hurtServer(level, source, damage);
         if (hurt) {
            MobRagdollAssembly.applyKnockup(level, part.ragdollIdentity().owner());
         }
         return hurt;
      } finally {
         MobRagdollAssembly.RAGDOLL_PIPE_ACTIVE.set(false);
      }
   }

   private static boolean pipeProjectile(ServerLevel level, UUID rootId, Projectile projectile) {
      SubLevel rootSubLevel = SubLevelContainer.getContainer(level).getSubLevel(rootId);
      if (!(rootSubLevel instanceof ServerSubLevel serverRoot)) return false;
      UUID ragdollPlayerId = RagdollSessionManager.getPlayerId(serverRoot);
      if (ragdollPlayerId == null) return false;
      Entity owner = projectile.getOwner();
      if (owner != null && ragdollPlayerId.equals(owner.getUUID())) return false;
      Entity ragdolledEntity = level.getEntity(ragdollPlayerId);
      if (!(ragdolledEntity instanceof ServerPlayer ragdolledPlayer) || !ragdolledPlayer.isAlive()) return false;

      DamageSource source = projectileDamageSource(level, projectile);
      float damage = projectileDamageAmount(projectile);

      RagdollSessionManager.RAGDOLL_PIPE_ACTIVE.set(true);
      try {
         return ragdolledPlayer.hurtServer(level, source, damage);
      } finally {
         RagdollSessionManager.RAGDOLL_PIPE_ACTIVE.set(false);
      }
   }

   private static DamageSource projectileDamageSource(ServerLevel level, Projectile projectile) {
      Entity owner = projectile.getOwner();
      if (projectile instanceof ThrownTrident trident) return level.damageSources().trident(trident, owner);
      if (projectile instanceof AbstractArrow arrow) return level.damageSources().arrow(arrow, owner);
      if (projectile instanceof WitherSkull skull) return level.damageSources().witherSkull(skull, owner);
      if (projectile instanceof Fireball fireball) return level.damageSources().fireball(fireball, owner);
      LivingEntity livingOwner = owner instanceof LivingEntity le ? le : null;
      return level.damageSources().mobProjectile(projectile, livingOwner);
   }

   private static float projectileDamageAmount(Projectile projectile) {
      // Use vanilla's default base arrow damage (2.0) scaled by the current arrow speed.
      if (projectile instanceof AbstractArrow arrow) return (float) Math.ceil(arrow.getDeltaMovement().length() * 2.0);
      if (projectile instanceof WitherSkull) return 8.0F;
      if (projectile instanceof LargeFireball) return 6.0F;
      if (projectile instanceof Fireball) return 5.0F; // SmallFireball, DragonFireball, etc.
      if (projectile instanceof ShulkerBullet) return 4.0F;
      return 0.0F; // snowball, egg, wind charge, etc. — no direct damage in vanilla
   }

}
