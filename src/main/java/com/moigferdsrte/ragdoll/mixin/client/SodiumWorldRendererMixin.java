package com.moigferdsrte.ragdoll.mixin.client;

import dev.ryanhcode.sable.mixinterface.sublevel_render.SubLevelBlockEntityRenderExtension;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.SortedSet;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.server.level.BlockDestructionProgress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Bridges Sable's sub-level block-entity extraction into Sodium's replacement
 * for vanilla {@code LevelExtractor#extractVisibleBlockEntities}.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer", remap = false)
public abstract class SodiumWorldRendererMixin {
    @Inject(method = "extractBlockEntities", at = @At("TAIL"))
    private void sablePlayerRagdoll$extractSubLevelBlockEntities(
            Camera camera,
            float tickDelta,
            Long2ObjectMap<SortedSet<BlockDestructionProgress>> progression,
            LevelRenderState levelRenderState,
            CallbackInfo callbackInfo
    ) {
        if (Minecraft.getInstance().levelExtractor instanceof SubLevelBlockEntityRenderExtension extension) {
            extension.sable$extractSubLevelBlockEntities(camera, tickDelta, levelRenderState);
        }
    }
}
