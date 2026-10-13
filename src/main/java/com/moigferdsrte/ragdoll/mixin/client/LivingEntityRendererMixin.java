package com.moigferdsrte.ragdoll.mixin.client;

import com.moigferdsrte.ragdoll.fabric.client.RagdollPartBlockEntityRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
   @WrapMethod(method = "getModel")
   private EntityModel<?> ragdoll$activeModel(Operation<EntityModel<?>> original) {
      EntityModel<?> activeModel = RagdollPartBlockEntityRenderer.currentModel();
      return activeModel != null ? activeModel : original.call();
   }
}
