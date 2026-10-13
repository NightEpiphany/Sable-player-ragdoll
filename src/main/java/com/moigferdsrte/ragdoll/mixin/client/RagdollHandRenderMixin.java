package com.moigferdsrte.ragdoll.mixin.client;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.moigferdsrte.ragdoll.fabric.client.RagdollCameraHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(GameRenderer.class)
abstract class RagdollHandRenderMixin {
    @WrapMethod(method = "renderItemInHand")
    private void ragdoll$hideHands(Camera camera, float partialTick, Matrix4f projection, Operation<Void> original) {
        if (!RagdollCameraHelper.suppressHands()) original.call(camera, partialTick, projection);
    }
}
