package com.moigferdsrte.ragdoll.mixin.client;

import com.moigferdsrte.ragdoll.fabric.client.RagdollSableCameraBypass;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(value = ActiveSableCompanion.class, remap = false)
public abstract class ActiveSableCompanionCameraMixin {
   @WrapMethod(method = "getTrackingOrVehicleSubLevel")
    private SubLevel sablePlayerRagdoll$ignoreRagdollSeatCameraSubLevel(Entity entity, Operation<SubLevel> original) {
      if (RagdollSableCameraBypass.detachSubLevelCameraPosition(entity)) {
         return null;
      }

        return original.call(entity);
    }
}
