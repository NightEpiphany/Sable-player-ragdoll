package com.moigferdsrte.ragdoll.mixin.client;

import com.moigferdsrte.ragdoll.fabric.client.RagdollPartBlockEntityRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {
   @WrapMethod(method = "getModel")
    private M onGetModel(Operation<M> original) {
      if (RagdollPartBlockEntityRenderer.currentModel() != null) {
         return (M) RagdollPartBlockEntityRenderer.currentModel();
      }

        return original.call();
    }
}
