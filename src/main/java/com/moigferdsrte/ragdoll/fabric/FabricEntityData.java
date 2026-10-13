package com.moigferdsrte.ragdoll.fabric;
import com.moigferdsrte.ragdoll.fabric.core.PersistentEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
/** Entity-local storage: ordinary entities allocate no compound during ticks. */
public final class FabricEntityData {
    private FabricEntityData() {}
    public static CompoundTag get(Entity entity) {
        PersistentEntityData storage = (PersistentEntityData) entity;
        CompoundTag data = storage.ragdoll$data();
        if (data == null) { data = new CompoundTag(); storage.ragdoll$data(data); }
        return data;
    }
    public static CompoundTag peek(Entity entity) { return ((PersistentEntityData) entity).ragdoll$data(); }
    public static boolean wasLoaded(Entity entity) { return ((PersistentEntityData) entity).ragdoll$wasLoaded(); }
    public static void load(Entity entity, CompoundTag data) {
        ((PersistentEntityData) entity).ragdoll$data(data.isEmpty() ? null : data.copy());
    }
    public static void remove(Entity entity, String key) {
        CompoundTag data = peek(entity);
        if (data != null) data.remove(key);
    }
    public static void putLong(Entity entity, String key, long value) { get(entity).putLong(key, value); }
}
