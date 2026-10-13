package com.moigferdsrte.ragdoll.mixin;

import com.moigferdsrte.ragdoll.mob.MobRagdollAssembly;
import com.moigferdsrte.ragdoll.mob.client.MobRagdollClientState;
import com.moigferdsrte.ragdoll.physics.RagdollSessionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(LivingEntity.class)
public abstract class LivingEntityRagdollDamageMixin {
    @WrapMethod(method = "hurtServer")
    private boolean sablePlayerRagdoll$suppressRagdollSuffocation(ServerLevel level, DamageSource source, float damage, Operation<Boolean> original) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (source.is(DamageTypes.IN_WALL) && sablePlayerRagdoll$isHiddenRagdollSource(entity)) {
            return false;
        }

        return original.call(level, source, damage);
    }

    @WrapMethod(method = "travel")
    private void splrmob$freezeRagdolledMovement(Vec3 travelVector, Operation<Void> original) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (MobRagdollAssembly.isConverted(entity)) {
            return;
        }

        original.call(travelVector);
    }

    @WrapMethod(method = "handleEntityEvent")
    private void splrmob$suppressRagdollDeathPoof(byte id, Operation<Void> original) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (id == 60 && entity.level().isClientSide()
                && (MobRagdollAssembly.isConverted(entity) || MobRagdollClientState.isDeathPending(entity))) {
            return;
        }

        original.call(id);
    }

    @WrapMethod(method = "isPushable")
    private boolean splrmob$hideRagdolledEntityFromPushChecks(Operation<Boolean> original) {
        if (sablePlayerRagdoll$isHiddenRagdollSource((Entity) (Object) this)) {
            return false;
        }

        return original.call();
    }

    @WrapMethod(method = "pushEntities")
    private void splrmob$stopRagdolledEntityPushingNeighbors(Operation<Void> original) {
        if (sablePlayerRagdoll$isHiddenRagdollSource((Entity) (Object) this)) {
            return;
        }

        original.call();
    }

    @WrapMethod(method = "doPush")
    private void splrmob$stopHiddenRagdollPushPair(Entity entity, Operation<Void> original) {
        if (sablePlayerRagdoll$isHiddenRagdollSource((Entity) (Object) this) || sablePlayerRagdoll$isHiddenRagdollSource(entity)) {
            return;
        }

        original.call(entity);
    }

    private static boolean sablePlayerRagdoll$isHiddenRagdollSource(Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;
        if (entity instanceof ServerPlayer serverPlayer && RagdollSessionManager.isPlayerCurrentlyRagdolled(serverPlayer)) {
            return true;
        }
        return MobRagdollAssembly.isConverted(entity);
    }
}
