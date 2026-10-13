package com.moigferdsrte.ragdoll.mob;

import com.moigferdsrte.ragdoll.SablePlayerRagdoll;
import com.moigferdsrte.ragdoll.mob.block.MobRagdollPartBlock;
import com.moigferdsrte.ragdoll.mob.block.entity.MobRagdollPartBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class MobRagdollBlocks {
    public static final MobRagdollPartBlock MOB_RAGDOLL_PART = Registry.register(
            BuiltInRegistries.BLOCK, id("mob_ragdoll_part"), new MobRagdollPartBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(-1.0F, 0.0F)
                    .sound(SoundType.EMPTY)
                    .noOcclusion()
                    .noLootTable()
                    .noTerrainParticles())
    );
    public static final BlockEntityType<MobRagdollPartBlockEntity> MOB_RAGDOLL_PART_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, id("mob_ragdoll_part"),
            BlockEntityType.Builder.of(MobRagdollPartBlockEntity::new, MOB_RAGDOLL_PART).build(null)
    );

    private MobRagdollBlocks() {
    }

    public static void register() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(SablePlayerRagdoll.MOD_ID, path);
    }
}
