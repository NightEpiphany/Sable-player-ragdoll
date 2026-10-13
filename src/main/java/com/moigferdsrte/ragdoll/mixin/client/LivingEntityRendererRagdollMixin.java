package com.moigferdsrte.ragdoll.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.moigferdsrte.ragdoll.mob.MobRagdollAssembly;
import com.moigferdsrte.ragdoll.mob.client.MobRagdollClientState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererRagdollMixin {

    @WrapOperation(
            method = "setupRotations",
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;deathTime:I")
    )
    private int splrmob$suppressPendingDeathRotation(LivingEntity entity, Operation<Integer> original) {
        return MobRagdollClientState.isDeathPending(entity) ? 0 : original.call(entity);
    }

    @WrapMethod(method = "render")
    private void splrmob$skipRagdollSourceRender(LivingEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, Operation<Void> original) {
        if (MobRagdollAssembly.isConverted(entity) || entity.getVehicle() instanceof com.moigferdsrte.ragdoll.entity.RagdollSeatEntity) {
            return;
        }

        original.call(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}
