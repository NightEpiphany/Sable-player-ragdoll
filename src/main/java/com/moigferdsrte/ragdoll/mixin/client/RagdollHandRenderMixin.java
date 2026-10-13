package com.moigferdsrte.ragdoll.mixin.client;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.moigferdsrte.ragdoll.fabric.client.RagdollCameraHelper;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(GameRenderer.class)
abstract class RagdollHandRenderMixin {
    @WrapMethod(method = "renderItemInHand")
    private void ragdoll$hideHands(CameraRenderState camera, PlayerRenderState player, GpuTextureView depthTexture, Operation<Void> original) {
        if (!RagdollCameraHelper.suppressHands()) original.call(camera, player, depthTexture);
    }
}
