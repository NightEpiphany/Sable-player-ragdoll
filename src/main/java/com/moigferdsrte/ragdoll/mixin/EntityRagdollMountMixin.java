package com.moigferdsrte.ragdoll.mixin;

import com.moigferdsrte.ragdoll.mob.MobRagdollAssembly;
import com.moigferdsrte.ragdoll.physics.RagdollBlockOwnership;
import com.moigferdsrte.ragdoll.physics.RagdollExpireHelper;
import com.moigferdsrte.ragdoll.physics.RagdollSeatingHelper;
import com.moigferdsrte.ragdoll.physics.RagdollSessionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(Entity.class)
public abstract class EntityRagdollMountMixin {
    @WrapMethod(method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z")
    private boolean sable_player_ragdoll$denyMountingConvertedMob(Entity vehicle, boolean force, boolean sendEventAndTriggers, Operation<Boolean> original) {
        Entity self = (Entity) (Object) this;
        if (vehicle instanceof LivingEntity living && MobRagdollAssembly.isPendingOrConverted(living)) {
            return false;
        }
        return original.call(vehicle, force, sendEventAndTriggers);
    }

    @WrapMethod(method = "stopRiding")
    private void sable_player_ragdoll$controlRagdollDismount(Operation<Void> original) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)
                || RagdollSeatingHelper.isUnseating(player)) { original.call(); return; }
        var ragdoll = RagdollSessionManager.activeRagdollForPlayer(level, player.getUUID());
        if (ragdoll == null || RagdollSessionManager.isExpiring(ragdoll)) { original.call(); return; }
        RagdollBlockOwnership.withOwner(RagdollBlockOwnership.ownerForPlayer(level, player.getUUID()), () -> {
            if (RagdollSessionManager.canManualDismount(level, ragdoll)) {
                RagdollExpireHelper.expire(level, ragdoll, "manual dismount");
            }
        });
    }
}
