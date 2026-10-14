package com.moigferdsrte.ragdoll.mixin;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Processes sub-level removals after Sable has applied holding-chunk changes.
 *
 * <p>{@code saveAll()} calls {@code processChanges()} before iterating the
 * container's live sub-levels. A failed holding-sub-level load can therefore
 * mark a newly allocated sub-level for removal during that call, leaving its
 * Rapier body invalid until the normal physics tick. Removing it immediately
 * after that call keeps the same save pass from serializing that stale body.</p>
 */
@Mixin(value = SubLevelHoldingChunkMap.class, remap = false)
public abstract class SableHoldingChunkMapMixin {
    @Shadow
    @Final
    private ServerSubLevelContainer container;

    @Inject(
            method = "saveAll",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/sublevel/storage/holding/SubLevelHoldingChunkMap;processChanges()V",
                    shift = At.Shift.AFTER
            )
    )
    private void sablePlayerRagdoll$processRemovedSubLevels(CallbackInfo callbackInfo) {
        this.container.processSubLevelRemovals();
    }
}
