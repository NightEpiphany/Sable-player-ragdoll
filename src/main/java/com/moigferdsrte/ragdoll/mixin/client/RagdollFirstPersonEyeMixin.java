package com.moigferdsrte.ragdoll.mixin.client;

import com.moigferdsrte.ragdoll.fabric.client.RagdollCameraHelper;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(Camera.class)
public abstract class RagdollFirstPersonEyeMixin {
   @Shadow
   private Vec3 position;

   @Shadow
   protected abstract void setPosition(double x, double y, double z);
   @WrapMethod(method = "alignWithEntity")
   private void sablePlayerRagdoll$anchorRagdollEye(float partialTick, Operation<Void> original) {
      original.call(partialTick);
      Vec3 target = RagdollCameraHelper.firstPersonHeadAnchor(this.position, partialTick);
      if (target != this.position) {
         this.setPosition(target.x, target.y, target.z);
      }
   }
}
