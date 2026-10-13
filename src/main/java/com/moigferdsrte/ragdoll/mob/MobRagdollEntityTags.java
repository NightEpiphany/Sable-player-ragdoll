package com.moigferdsrte.ragdoll.mob;

import com.moigferdsrte.ragdoll.SablePlayerRagdoll;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class MobRagdollEntityTags {
    public static final TagKey<EntityType<?>> FALL_APART_ON_DEATH = TagKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(SablePlayerRagdoll.MOD_ID, "fall_apart_on_death"));

    private MobRagdollEntityTags() {
    }
}
