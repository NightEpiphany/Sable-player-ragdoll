package com.moigferdsrte.ragdoll.mixin.client;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.moigferdsrte.ragdoll.fabric.client.RagdollGrabClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(MouseHandler.class)
abstract class RagdollMouseMixin {
    @WrapMethod(method = "onScroll")
    private void ragdoll$keepGrabSlot(long window, double horizontal, double vertical, Operation<Void> original) {
        if (Minecraft.getInstance().screen != null || !RagdollGrabClient.isGrabbing()) original.call(window, horizontal, vertical);
    }
}
