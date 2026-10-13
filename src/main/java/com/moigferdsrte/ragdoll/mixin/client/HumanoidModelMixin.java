package com.moigferdsrte.ragdoll.mixin.client;

import com.moigferdsrte.ragdoll.fabric.client.RagdollGrabState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
   @Shadow public ModelPart leftArm;
   @Shadow public ModelPart rightArm;
   @Shadow public ModelPart head;

   @WrapMethod(method = "setupAnim")
   private void ragdoll$setGrabPose(HumanoidRenderState state, Operation<Void> original) {
      original.call(state);
      Minecraft minecraft = Minecraft.getInstance();
      if (!(state instanceof AvatarRenderState avatarState)
            || minecraft.player == null
            || avatarState.id != minecraft.player.getId()
            || !RagdollGrabState.isGrabbing(minecraft.player.getUUID())
            || minecraft.isPaused()) {
         return;
      }

      this.leftArm.zRot = 0.0F;
      this.rightArm.zRot = 0.0F;
      this.leftArm.xRot = (float) Math.toRadians(-80.0) + this.head.xRot;
      this.rightArm.xRot = (float) Math.toRadians(-80.0) + this.head.xRot;
      this.leftArm.yRot = (float) Math.toRadians(15.0);
      this.rightArm.yRot = (float) Math.toRadians(-15.0);
   }
}
