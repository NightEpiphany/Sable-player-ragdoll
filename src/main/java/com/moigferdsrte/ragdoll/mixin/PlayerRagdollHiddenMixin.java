package com.moigferdsrte.ragdoll.mixin;

import com.moigferdsrte.ragdoll.entity.RagdollSeatEntity;
import com.moigferdsrte.ragdoll.mob.MobRagdollAssembly;
import com.moigferdsrte.ragdoll.mob.client.MobRagdollClientState;
import com.moigferdsrte.ragdoll.physics.RagdollSessionManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(LivingEntity.class)
public abstract class PlayerRagdollHiddenMixin {

    private boolean isHiddenRagdollSource() {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) {
            if (self instanceof Player && self.isInvisible() && self.getVehicle() instanceof RagdollSeatEntity) {
                return true;
            }
            return MobRagdollClientState.isHidden(self);
        }
        if (self instanceof ServerPlayer serverPlayer
                && RagdollSessionManager.isPlayerCurrentlyRagdolled(serverPlayer)) {
            return true;
        }
        return MobRagdollAssembly.isConverted(self);
    }

    @WrapMethod(method = "isPickable")
    private boolean hideFromPicking(Operation<Boolean> original) {
        if (isHiddenRagdollSource()) return false;

        return original.call();
    }

    @WrapMethod(method = "isPushable")
    private boolean hideFromPushes(Operation<Boolean> original) {
        if (isHiddenRagdollSource()) return false;

        return original.call();
    }

    @WrapMethod(method = "push(Lnet/minecraft/world/entity/Entity;)V")
    private void cancelPush(Entity other, Operation<Void> original) {
        if (isHiddenRagdollSource()) return;

        original.call(other);
    }

    @org.spongepowered.asm.mixin.Unique
    private static final EntityDimensions RAGDOLL_ANCHOR_DIMENSIONS = EntityDimensions.fixed(0.01F, 0.01F).withEyeHeight(1.62F);

    @WrapMethod(method = "getDimensions")
    private EntityDimensions shrinkDimensions(Pose pose, Operation<EntityDimensions> original) {
        if (isHiddenRagdollSource()) {
            return RAGDOLL_ANCHOR_DIMENSIONS;
        }

        return original.call(pose);
    }

    @WrapMethod(method = "pushEntities")
    private void stopPushingNeighbors(Operation<Void> original) {
        if (isHiddenRagdollSource()) return;

        original.call();
    }

    @WrapMethod(method = "doPush")
    private void stopPushPair(Entity other, Operation<Void> original) {
        if (isHiddenRagdollSource()) return;

        original.call(other);
    }
}
