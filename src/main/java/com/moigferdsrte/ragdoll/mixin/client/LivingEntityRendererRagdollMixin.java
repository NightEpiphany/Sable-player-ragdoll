package com.moigferdsrte.ragdoll.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.moigferdsrte.ragdoll.entity.RagdollSeatEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererRagdollMixin {
   @WrapMethod(method = "submit")
   private void ragdoll$hideMountedAvatar(
         net.minecraft.client.renderer.entity.state.LivingEntityRenderState state,
         PoseStack poseStack,
         SubmitNodeCollector collector,
         CameraRenderState camera,
         Operation<Void> original
   ) {
      if (state instanceof AvatarRenderState avatarState) {
         Minecraft minecraft = Minecraft.getInstance();
         Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(avatarState.id);
         if (entity != null && entity.getVehicle() instanceof RagdollSeatEntity) return;
      }
      original.call(state, poseStack, collector, camera);
   }
}
