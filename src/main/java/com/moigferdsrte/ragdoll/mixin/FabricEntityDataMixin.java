package com.moigferdsrte.ragdoll.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.moigferdsrte.ragdoll.fabric.core.PersistentEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
    private void ragdoll$save(ValueOutput output, Operation<Void> original) {
        original.call(output);
        if (ragdoll$data != null && !ragdoll$data.isEmpty()) {
            output.store("sable_player_ragdoll", CompoundTag.CODEC, ragdoll$data.copy());
        }
    }

    @WrapMethod(method = "load")
    private void ragdoll$load(ValueInput input, Operation<Void> original) {
        original.call(input);
        ragdoll$data = input.read("sable_player_ragdoll", CompoundTag.CODEC)
                .filter(data -> !data.isEmpty())
                .map(CompoundTag::copy)
                .orElse(null);
        ragdoll$loaded = true;
    }
}
