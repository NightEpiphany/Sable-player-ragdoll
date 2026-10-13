package com.moigferdsrte.ragdoll.physics;

import com.moigferdsrte.ragdoll.api.RagdollEquipmentScope;
import com.moigferdsrte.ragdoll.api.RagdollEquipmentSnapshot;
import com.moigferdsrte.ragdoll.block.entity.RagdollPartBlockEntity;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

final class RagdollEquipmentHelper {
   private RagdollEquipmentHelper() {
   }

   static void applyFrom(ServerLevel level, UUID rootId, Player player) {
      applySnapshot(level, rootId, capture(player, RagdollEquipmentScope.ALL));
   }

   static void applyExtraFrom(ServerLevel level, UUID rootId, Player player) {
      applySnapshot(level, rootId, capture(player, RagdollEquipmentScope.OPTIONAL_MODS));
   }

   static RagdollEquipmentSnapshot capture(Player player, RagdollEquipmentScope scope) {
      RagdollEquipmentScope resolved = scope == null ? RagdollEquipmentScope.ALL : scope;
      Map<EquipmentSlot, ItemStack> vanillaItems = Map.of();
      Map<String, List<ItemStack>> curioItems = Map.of();
      Map<String, List<ItemStack>> curioCosmeticItems = Map.of();
      Map<String, List<Boolean>> curioRenderOptions = Map.of();
      // Accessories integration is disabled on 26.3 until a compatible API is available.
      Map<String, List<ItemStack>> accessoriesItems = Map.of();
      Map<String, List<ItemStack>> accessoriesCosmeticItems = Map.of();
      Map<String, List<Boolean>> accessoriesRenderOptions = Map.of();

      if (resolved == RagdollEquipmentScope.ALL || resolved == RagdollEquipmentScope.VANILLA) {
         EnumMap<EquipmentSlot, ItemStack> vanilla = new EnumMap<>(EquipmentSlot.class);
         for (EquipmentSlot slot : EquipmentSlot.values()) {
            vanilla.put(slot, player.getItemBySlot(slot).copy());
         }
         vanillaItems = vanilla;
      }

      return new RagdollEquipmentSnapshot(vanillaItems, curioItems, curioCosmeticItems, curioRenderOptions, accessoriesItems, accessoriesCosmeticItems, accessoriesRenderOptions);
   }

   static void applySnapshot(ServerLevel level, UUID rootId, RagdollEquipmentSnapshot snapshot) {
      if (snapshot == null || snapshot.isEmpty()) return;

      applyToAllParts(level, rootId, be -> {
         snapshot.vanillaItems().forEach(be::setItemForSlot);
         snapshot.curioItems().forEach(be::setCurioItems);
         snapshot.curioCosmeticItems().forEach(be::setCurioCosmeticItems);
         snapshot.curioRenderOptions().forEach(be::setCurioRenderOptions);
         snapshot.accessoriesItems().forEach(be::setAccessoriesItems);
         snapshot.accessoriesCosmeticItems().forEach(be::setAccessoriesCosmeticItems);
         snapshot.accessoriesRenderOptions().forEach(be::setAccessoriesRenderOptions);
      });
      sendPartUpdates(level, rootId);
   }

   static void applyToAllParts(ServerLevel level, UUID rootId, Consumer<RagdollPartBlockEntity> action) {
      var container = SubLevelContainer.getContainer(level);
      for (UUID partId : RagdollAssemblyHelper.linkedParts(level, rootId)) {
         SubLevel subLevel = container.getSubLevel(partId);
         if (subLevel == null || subLevel.getPlot() == null) continue;
         BlockPos center = subLevel.getPlot().getCenterBlock();
         if (subLevel.getLevel().getBlockEntity(center) instanceof RagdollPartBlockEntity part) {
            action.accept(part);
         }
      }
   }

   static void sendPartUpdates(ServerLevel level, UUID rootId) {
      var container = SubLevelContainer.getContainer(level);
      for (UUID partId : RagdollAssemblyHelper.linkedParts(level, rootId)) {
         SubLevel subLevel = container.getSubLevel(partId);
         if (subLevel == null || subLevel.getPlot() == null) continue;
         BlockPos center = subLevel.getPlot().getCenterBlock();
         Level partLevel = subLevel.getLevel();
         partLevel.sendBlockUpdated(center, partLevel.getBlockState(center), partLevel.getBlockState(center), 3);
      }
   }
}
