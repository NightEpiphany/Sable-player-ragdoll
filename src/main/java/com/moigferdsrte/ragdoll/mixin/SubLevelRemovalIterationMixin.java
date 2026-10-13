package com.moigferdsrte.ragdoll.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import java.util.Iterator;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(value = SubLevelContainer.class, remap = false)
abstract class SubLevelRemovalIterationMixin {
    @WrapOperation(method = "processSubLevelRemovals", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    private Iterator<?> ragdoll$snapshotRemovals(List<?> levels, Operation<Iterator<?>> original) {
        return original.call(levels.isEmpty() ? levels : List.copyOf(levels));
    }
}
