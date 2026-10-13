package com.moigferdsrte.ragdoll.mixin.client;

import com.moigferdsrte.ragdoll.fabric.client.RagdollGrabState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> {
   @Shadow public ModelPart leftArm;
   @Shadow public ModelPart rightArm;
   @Shadow public ModelPart head;

   @WrapMethod(method = "setupAnim")
    private void onSetupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, Operation<Void> original) {
        original.call(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
      if (!(entity instanceof Player player)) return;
      if (!RagdollGrabState.isGrabbing(player.getUUID())) return;
      if (Minecraft.getInstance().isPaused()) return;

      leftArm.zRot = 0.0f;
      rightArm.zRot = 0.0f;
      leftArm.xRot = (float) Math.toRadians(-80.0) + head.xRot;
      rightArm.xRot = (float) Math.toRadians(-80.0) + head.xRot;
      leftArm.yRot = (float) Math.toRadians(15.0);
      rightArm.yRot = (float) Math.toRadians(-15.0);
   }
}
