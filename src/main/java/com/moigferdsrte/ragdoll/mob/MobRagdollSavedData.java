package com.moigferdsrte.ragdoll.mob;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class MobRagdollSavedData extends SavedData {
    private static final String DATA_NAME = "sable_player_ragdoll_mob_ragdolls";
    private static final Codec<MobRagdollSavedData> CODEC = CompoundTag.CODEC.listOf()
            .optionalFieldOf("Entries", List.of())
            .xmap(MobRagdollSavedData::new, data -> data.entries)
            .codec();
    private static final SavedDataType<MobRagdollSavedData> TYPE = new SavedDataType<>(
            Identifier.withDefaultNamespace(DATA_NAME), MobRagdollSavedData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final List<CompoundTag> entries;

    public MobRagdollSavedData() {
        this(List.of());
    }

    private MobRagdollSavedData(List<CompoundTag> entries) {
        this.entries = entries.stream().map(CompoundTag::copy).toList();
    }

    public static ListTag legacyEntries(ServerLevel level) {
        MobRagdollSavedData data = level.getDataStorage().computeIfAbsent(TYPE);
        ListTag entries = new ListTag();
        data.entries.forEach(entry -> entries.add(entry.copy()));
        return entries;
    }
}
