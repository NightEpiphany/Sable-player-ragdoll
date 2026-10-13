package com.moigferdsrte.ragdoll.mixin.client;

import com.moigferdsrte.ragdoll.entity.RagdollSeatEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(Hud.class)
public abstract class GuiOverlayRagdollMixin {
    @WrapMethod(method = "setOverlayMessage")
    private void spr$suppressDismountHint(Component message, boolean animateColor, Operation<Void> original) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.getVehicle() instanceof RagdollSeatEntity) {
            return;
        }

        original.call(message, animateColor);
    }
}
