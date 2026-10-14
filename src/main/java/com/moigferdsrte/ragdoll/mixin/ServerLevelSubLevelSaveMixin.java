package com.moigferdsrte.ragdoll.mixin;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Removes sub-levels marked by Sable as invalid before its save hook serializes them.
 *
 * <p>Sable invokes its holding-chunk save hook from {@code ServerLevel.save}.  A
 * sub-level can be marked removed after its Rapier body has already been detached,
 * but before the normal container tick processes the removal.  Processing that queue
 * at the beginning of save keeps the serializer from seeing a stale body.</p>
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelSubLevelSaveMixin {
    @Inject(method = "save", at = @At("HEAD"))
    private void sablePlayerRagdoll$processRemovedSubLevels(CallbackInfo callbackInfo) {
        ServerSubLevelContainer container = SubLevelContainer.getContainer((ServerLevel) (Object) this);
        if (container != null) {
            container.processSubLevelRemovals();
        }
    }
}
