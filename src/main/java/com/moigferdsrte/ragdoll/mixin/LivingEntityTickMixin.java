package com.moigferdsrte.ragdoll.mixin;

import com.moigferdsrte.ragdoll.mob.MobRagdollAssembly;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(LivingEntity.class)
public abstract class LivingEntityTickMixin {
    @WrapMethod(method = "tick")
    private void sable_player_ragdoll$tickMobRagdollSource(Operation<Void> original) {
        original.call();
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level() instanceof ServerLevel level) {
            MobRagdollAssembly.tickSource(level, entity);
        }
    }
}
