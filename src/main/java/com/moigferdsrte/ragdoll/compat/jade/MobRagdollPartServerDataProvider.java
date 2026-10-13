package com.moigferdsrte.ragdoll.compat.jade;

import com.moigferdsrte.ragdoll.mob.block.MobPartRole;
import com.moigferdsrte.ragdoll.mob.block.entity.MobRagdollPartBlockEntity;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

public final class MobRagdollPartServerDataProvider implements IServerDataProvider<BlockAccessor> {
    public static final MobRagdollPartServerDataProvider INSTANCE = new MobRagdollPartServerDataProvider();

    private static final Identifier UID = Identifier.fromNamespaceAndPath("sable_player_ragdoll", "mob_ragdoll_part_data");

    private MobRagdollPartServerDataProvider() {
    }

    @Override
    public Identifier getUid() {
        return UID;
    }

    @Override
    public void appendServerData(final CompoundTag data, final BlockAccessor accessor) {
        final BlockEntity be = RagdollPartServerDataProvider.resolveBlockEntity(accessor);
        if (!(be instanceof MobRagdollPartBlockEntity mob)) {
            return;
        }
        final Identifier entityType = mob.entityType();
        if (entityType != null) {
            data.putString("EntityType", entityType.toString());
        }
        final MobPartRole role = mob.role();
        if (role != null) {
            data.putString("Role", role.getSerializedName());
        }
        data.putBoolean("Baby", mob.baby());

        final UUID sourceId = mob.sourceEntityId();
        if (sourceId != null && accessor.getLevel() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(sourceId) instanceof LivingEntity living) {
            RagdollPartServerDataProvider.putLivingStats(data, living);
            return;
        }
        data.putFloat("Health", 0f);
        data.putFloat("MaxHealth", 20f);
    }
}
