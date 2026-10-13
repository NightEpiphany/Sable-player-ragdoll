package com.moigferdsrte.ragdoll.fabric;

import com.moigferdsrte.ragdoll.RagdollSoundEvents;
import com.moigferdsrte.ragdoll.block.RagdollBlocks;
import com.moigferdsrte.ragdoll.block.RagdollPartBlock;
import com.moigferdsrte.ragdoll.block.RagdollSeatBlock;
import com.moigferdsrte.ragdoll.block.entity.RagdollPartBlockEntities;
import com.moigferdsrte.ragdoll.block.entity.RagdollPartBlockEntity;
import com.moigferdsrte.ragdoll.entity.RagdollDollEntity;
import com.moigferdsrte.ragdoll.entity.RagdollSeatEntities;
import com.moigferdsrte.ragdoll.entity.RagdollSeatEntity;
import com.moigferdsrte.ragdoll.mob.MobRagdollBlocks;
import java.util.Set;
import java.util.function.Function;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class FabricRegistries {
    public static final RagdollSeatBlock RAGDOLL_SEAT = registerBlock("ragdoll_seat", RagdollSeatBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(2.0F).noOcclusion());
    public static final RagdollPartBlock RAGDOLL_PART = registerBlock("ragdoll_part", RagdollPartBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(-1.0F, 3600000.0F)
                    .sound(SoundType.EMPTY).noOcclusion());
    public static final BlockEntityType<RagdollPartBlockEntity> RAGDOLL_PART_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, id("ragdoll_part"),
            new BlockEntityType<>(RagdollPartBlockEntity::new, Set.of(RAGDOLL_PART)));
    public static final EntityType<RagdollSeatEntity> RAGDOLL_SEAT_ENTITY = Registry.register(
            BuiltInRegistries.ENTITY_TYPE, id("ragdoll_seat"), EntityType.Builder.<RagdollSeatEntity>of(RagdollSeatEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.35F).passengerAttachments(0.1F)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id("ragdoll_seat"))));
    public static final EntityType<RagdollDollEntity> RAGDOLL_DOLL_ENTITY = Registry.register(
            BuiltInRegistries.ENTITY_TYPE, id("ragdoll_doll"), EntityType.Builder.<RagdollDollEntity>of(RagdollDollEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F).clientTrackingRange(10).updateInterval(3)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id("ragdoll_doll"))));
    public static final SoundEvent RAGDOLL_IMPACT_SOUND = sound("ragdoll_impact");
    public static final SoundEvent RAGDOLL_SMALL_IMPACT_SOUND = sound("ragdoll_small_impact");

    private FabricRegistries() {
    }

    public static void register() {
        RagdollBlocks.bindSeat(RAGDOLL_SEAT);
        RagdollBlocks.bindPart(RAGDOLL_PART);
        RagdollPartBlockEntities.bindRagdollPart(RAGDOLL_PART_BLOCK_ENTITY);
        RagdollSeatEntities.bindRagdollSeat(RAGDOLL_SEAT_ENTITY);
        RagdollSeatEntities.bindRagdollDoll(RAGDOLL_DOLL_ENTITY);
        RagdollSoundEvents.bindRagdollImpact(RAGDOLL_IMPACT_SOUND);
        RagdollSoundEvents.bindRagdollSmallImpact(RAGDOLL_SMALL_IMPACT_SOUND);
        FabricDefaultAttributeRegistry.register(RAGDOLL_DOLL_ENTITY, RagdollDollEntity.createAttributes());
        MobRagdollBlocks.register();
    }

    private static SoundEvent sound(String path) {
        Identifier id = id(path);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("sable_player_ragdoll", path);
    }

    private static <T extends Block> T registerBlock(String path, Function<BlockBehaviour.Properties, T> factory,
                                                     BlockBehaviour.Properties properties) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(path));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
    }
}
