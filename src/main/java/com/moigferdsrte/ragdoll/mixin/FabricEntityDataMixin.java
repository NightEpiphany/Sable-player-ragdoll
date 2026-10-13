package com.moigferdsrte.ragdoll.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.moigferdsrte.ragdoll.fabric.core.PersistentEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
@Mixin(Entity.class)
abstract class FabricEntityDataMixin implements PersistentEntityData {
    @Unique private CompoundTag ragdoll$data;
    @Unique private boolean ragdoll$loaded;
    public CompoundTag ragdoll$data() { return ragdoll$data; }
    public void ragdoll$data(CompoundTag data) { ragdoll$data = data; }
    public boolean ragdoll$wasLoaded() { return ragdoll$loaded; }
    @WrapMethod(method = "saveWithoutId")
    private CompoundTag ragdoll$save(CompoundTag tag, Operation<CompoundTag> original) {
        CompoundTag result = original.call(tag);
        if (ragdoll$data != null && !ragdoll$data.isEmpty()) result.put("sable_player_ragdoll", ragdoll$data.copy());
        return result;
    }
    @WrapMethod(method = "load")
    private void ragdoll$load(CompoundTag tag, Operation<Void> original) {
        CompoundTag data = tag.getCompound("sable_player_ragdoll");
        ragdoll$data = data.isEmpty() ? null : data.copy();
        ragdoll$loaded = true;
        original.call(tag);
    }
}
