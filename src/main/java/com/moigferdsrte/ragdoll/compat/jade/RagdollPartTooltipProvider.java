package com.moigferdsrte.ragdoll.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.impl.ui.ArmorElement;

public final class RagdollPartTooltipProvider implements IBlockComponentProvider {
    public static final RagdollPartTooltipProvider INSTANCE = new RagdollPartTooltipProvider();

    private static final Identifier UID = Identifier.fromNamespaceAndPath("sable_player_ragdoll", "ragdoll_part");

    private RagdollPartTooltipProvider() {
    }

    @Override
    public Identifier getUid() {
        return UID;
    }

    @Override
    public boolean isRequired() {
        return true;
    }

    @Override
    public void appendTooltip(final ITooltip tooltip, final BlockAccessor accessor, final IPluginConfig config) {
        final CompoundTag data = accessor.getServerData();
        if (!data.contains("BodyPart")) {
            return;
        }

        tooltip.clear();

        final String skinName = data.getStringOr("SkinName", "");
        if (!skinName.isBlank() && !"Player".equals(skinName)) {
            tooltip.add(Component.literal(skinName));
        }
        tooltip.add(Component.literal(formatPartName(data.getStringOr("BodyPart", ""))));

        appendStats(tooltip, data);
    }

    static void appendStats(final ITooltip tooltip, final CompoundTag data) {
        if (data.contains("MaxHealth")) {
            final float max = data.getFloatOr("MaxHealth", 0.0F);
            if (max > 0) {
                final float health = data.getFloatOr("Health", 0.0F);
                tooltip.add(Component.literal("Health: " + health + " / " + max),
                        Identifier.fromNamespaceAndPath("sable_player_ragdoll", "ragdoll_health"));
            }
        }
        if (data.contains("Armor")) {
            final float armor = data.getFloatOr("Armor", 0.0F);
            if (armor > 0) {
                tooltip.add(new ArmorElement(armor).tag(
                        Identifier.fromNamespaceAndPath("sable_player_ragdoll", "ragdoll_armor")));
            }
        }
    }

    static String formatPartName(final String serializedName) {
        final StringBuilder sb = new StringBuilder();
        for (final String word : serializedName.split("_")) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                sb.append(word.substring(1));
            }
        }
        return sb.toString();
    }
}
