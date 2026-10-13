package com.moigferdsrte.ragdoll.mixin.client;

import com.moigferdsrte.ragdoll.entity.RagdollSeatEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiOverlayRagdollMixin {
    @Inject(method = "setOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void spr$suppressDismountHint(Component message, boolean animateColor, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.getVehicle() instanceof RagdollSeatEntity) {
            ci.cancel();
        }
    }
}
