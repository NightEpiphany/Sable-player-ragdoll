package com.moigferdsrte.ragdoll.mob;

import com.moigferdsrte.ragdoll.SablePlayerRagdoll;
import com.moigferdsrte.ragdoll.mob.block.MobRagdollPartBlock;
import com.moigferdsrte.ragdoll.mob.block.entity.MobRagdollPartBlockEntity;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class MobRagdollBlocks {
    public static final MobRagdollPartBlock MOB_RAGDOLL_PART = registerBlock("mob_ragdoll_part",
            MobRagdollPartBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(-1.0F, 0.0F)
                    .sound(SoundType.EMPTY)
                    .noOcclusion()
                    .noLootTable()
                    .noTerrainParticles());
    public static final BlockEntityType<MobRagdollPartBlockEntity> MOB_RAGDOLL_PART_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, id("mob_ragdoll_part"),
            new BlockEntityType<>(MobRagdollPartBlockEntity::new, Set.of(MOB_RAGDOLL_PART))
    );

    private MobRagdollBlocks() {
    }

    public static void register() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SablePlayerRagdoll.MOD_ID, path);
    }

    private static <T extends Block> T registerBlock(String path, Function<BlockBehaviour.Properties, T> factory,
                                                     BlockBehaviour.Properties properties) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(path));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
    }
}
