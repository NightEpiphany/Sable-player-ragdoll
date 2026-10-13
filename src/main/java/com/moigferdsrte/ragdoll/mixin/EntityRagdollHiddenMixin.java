package com.moigferdsrte.ragdoll.mixin;

import com.moigferdsrte.ragdoll.entity.RagdollSeatEntity;
import com.moigferdsrte.ragdoll.mob.MobRagdollAssembly;
import com.moigferdsrte.ragdoll.mob.client.MobRagdollClientState;
import com.moigferdsrte.ragdoll.physics.RagdollSessionManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(Entity.class)
public abstract class EntityRagdollHiddenMixin {
    @Unique private AABB ragdoll$bounds;
    @Unique private double ragdoll$boundsX, ragdoll$boundsY, ragdoll$boundsZ;
    @Unique
    private static final EntityDimensions RAGDOLL_SOURCE_DIMENSIONS = EntityDimensions.fixed(0.01F, 0.01F).withEyeHeight(0.0F);
    @Unique
    private static final double RAGDOLL_SOURCE_BB_HALF_SIZE = 0.0005;

    @Unique
    private boolean isHiddenRagdollSource() {
        return isHiddenRagdollSource((Entity) (Object) this);
    }

    @Unique
    private static boolean isHiddenRagdollSource(Entity self) {
        if (self.level().isClientSide()) {
            if (self instanceof Player && self.isInvisible() && self.getVehicle() instanceof RagdollSeatEntity) {
                return true;
            }
            return MobRagdollClientState.isHidden(self);
        }
        if (self instanceof ServerPlayer serverPlayer && RagdollSessionManager.isPlayerCurrentlyRagdolled(serverPlayer)) {
            return true;
        }
        return self instanceof net.minecraft.world.entity.LivingEntity living
                && com.moigferdsrte.ragdoll.mob.MobRagdollSourceRecovery.active(living);
    }

    @Unique
    private static boolean ragdollPipeActive() {
        return RagdollSessionManager.RAGDOLL_PIPE_ACTIVE.get() || MobRagdollAssembly.RAGDOLL_PIPE_ACTIVE.get();
    }

    @WrapMethod(method = "isPickable")
    private boolean hideFromPicking(Operation<Boolean> original) {
        if (isHiddenRagdollSource()) return false;

        return original.call();
    }

    @WrapMethod(method = "isAttackable")
    private boolean hideFromDirectAttacks(Operation<Boolean> original) {
        if (!ragdollPipeActive() && isHiddenRagdollSource()) {
            return false;
        }

        return original.call();
    }

    @WrapMethod(method = "interact")
    private InteractionResult blockDirectInteract(Player player, InteractionHand hand, Vec3 location, Operation<InteractionResult> original) {
        if (!ragdollPipeActive() && isHiddenRagdollSource()) {
            return InteractionResult.FAIL;
        }

        return original.call(player, hand, location);
    }

    @WrapMethod(method = "canBeCollidedWith")
    private boolean hideFromCollisionPicking(Entity other, Operation<Boolean> original) {
        if (isHiddenRagdollSource()) return false;

        return original.call(other);
    }

    @WrapMethod(method = "canCollideWith")
    private boolean hideFromCollisionPartners(Entity other, Operation<Boolean> original) {
        if (isHiddenRagdollSource() || isHiddenRagdollSource(other)) return false;

        return original.call(other);
    }

    @WrapMethod(method = "isPushable")
    private boolean hideFromPushes(Operation<Boolean> original) {
        if (isHiddenRagdollSource()) return false;

        return original.call();
    }

    @WrapMethod(method = "push(Lnet/minecraft/world/entity/Entity;)V")
    private void ignorePush(Entity entity, Operation<Void> original) {
        if (isHiddenRagdollSource()) return;

        original.call(entity);
    }

    @WrapMethod(method = "canBeHitByProjectile")
    private boolean hideFromProjectiles(Operation<Boolean> original) {
        if (isHiddenRagdollSource()) return false;

        return original.call();
    }

    @WrapMethod(method = "getDimensions")
    private EntityDimensions shrinkDimensions(Pose pose, Operation<EntityDimensions> original) {
        if (isHiddenRagdollSource()) return RAGDOLL_SOURCE_DIMENSIONS;

        return original.call(pose);
    }

    @WrapMethod(method = "getBoundingBox")
    private AABB shrinkBoundingBox(Operation<AABB> original) {
        if (isHiddenRagdollSource()) {
            Entity self = (Entity) (Object) this;
            double half = RAGDOLL_SOURCE_BB_HALF_SIZE;
            if (ragdoll$bounds == null || ragdoll$boundsX != self.getX() || ragdoll$boundsY != self.getY() || ragdoll$boundsZ != self.getZ()) {
                ragdoll$boundsX = self.getX(); ragdoll$boundsY = self.getY(); ragdoll$boundsZ = self.getZ();
                ragdoll$bounds = new AABB(ragdoll$boundsX - half, ragdoll$boundsY, ragdoll$boundsZ - half,
                        ragdoll$boundsX + half, ragdoll$boundsY + half * 2, ragdoll$boundsZ + half);
            }
            return ragdoll$bounds;
        }

        return original.call();
    }
}
