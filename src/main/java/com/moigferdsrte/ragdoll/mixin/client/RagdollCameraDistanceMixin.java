package com.moigferdsrte.ragdoll.mixin.client;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.moigferdsrte.ragdoll.fabric.client.RagdollClientConfig;
import com.moigferdsrte.ragdoll.entity.RagdollSeatEntity;
import dev.ryanhcode.sable.mixinhelpers.camera.new_camera_types.SableCameraTypes;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(Camera.class)
abstract class RagdollCameraDistanceMixin {
    @WrapMethod(method = "getMaxZoom")
    private float ragdoll$cameraDistance(float distance, Operation<Float> original) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.player.getVehicle() instanceof RagdollSeatEntity
                && client.options.getCameraType() == SableCameraTypes.SUB_LEVEL_VIEW_UNLOCKED) {
            distance = (float) RagdollClientConfig.subLevelCameraDistance();
        }
        return original.call(distance);
    }
}
