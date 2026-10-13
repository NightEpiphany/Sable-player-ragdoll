package com.moigferdsrte.ragdoll.mixin.client;

import com.moigferdsrte.ragdoll.fabric.client.RagdollSableCameraBypass;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinhelpers.camera.camera_rotation.EntitySubLevelRotationHelper;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.function.Function;
import net.minecraft.world.entity.Entity;
import org.joml.Quaterniond;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(value = EntitySubLevelRotationHelper.class, remap = false)
public abstract class EntitySubLevelRotationHelperMixin {
   @WrapMethod(method = "getSubLevelInheritedOrientation")
    private static Quaterniond sablePlayerRagdoll$ignoreRagdollSeatCameraRotation(Entity cameraEntity, Function<SubLevel, Pose3dc> poseProvider, EntitySubLevelRotationHelper.Type type, Operation<Quaterniond> original) {
      if (type == EntitySubLevelRotationHelper.Type.CAMERA && RagdollSableCameraBypass.suppressSubLevelCameraRotation(cameraEntity)) {
         return null;
      }

        return original.call(cameraEntity, poseProvider, type);
    }
}
