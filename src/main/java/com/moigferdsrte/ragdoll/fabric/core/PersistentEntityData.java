package com.moigferdsrte.ragdoll.fabric.core;
import net.minecraft.nbt.CompoundTag;
public interface PersistentEntityData {

    CompoundTag ragdoll$data();

    void ragdoll$data(CompoundTag data);

    boolean ragdoll$wasLoaded();
}
