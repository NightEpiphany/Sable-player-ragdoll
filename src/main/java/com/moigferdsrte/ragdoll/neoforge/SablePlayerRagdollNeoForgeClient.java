package com.moigferdsrte.ragdoll.neoforge;

import com.moigferdsrte.ragdoll.block.RagdollPartBlock;
import com.moigferdsrte.ragdoll.block.entity.RagdollPartBlockEntity;
import com.moigferdsrte.ragdoll.entity.RagdollDollEntity;
import com.moigferdsrte.ragdoll.entity.RagdollSeatEntity;
import com.moigferdsrte.ragdoll.neoforge.client.RagdollCameraHelper;
import com.moigferdsrte.ragdoll.neoforge.client.RagdollDollEntityRenderer;
import com.moigferdsrte.ragdoll.neoforge.client.RagdollControlsHint;
import com.moigferdsrte.ragdoll.neoforge.client.RagdollControlsHud;
import com.moigferdsrte.ragdoll.neoforge.client.RagdollInputClient;
import com.moigferdsrte.ragdoll.neoforge.client.RagdollKeybinds;
import com.moigferdsrte.ragdoll.neoforge.client.RagdollPartBlockEntityRenderer;
import com.moigferdsrte.ragdoll.neoforge.client.RagdollSeatEntityRenderer;
import com.moigferdsrte.ragdoll.neoforge.client.RagdollGrabClient;
import com.moigferdsrte.ragdoll.neoforge.client.RagdollBlockInteractClient;
import com.moigferdsrte.ragdoll.neoforge.config.RagdollClientConfig;
import com.moigferdsrte.ragdoll.mob.MobRagdollBlocks;
import com.moigferdsrte.ragdoll.mob.block.MobRagdollPartBlock;
import com.moigferdsrte.ragdoll.mob.client.MobRagdollPartBlockEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = "sable_player_ragdoll", dist = {Dist.CLIENT})
public final class SablePlayerRagdollNeoForgeClient {
   @SuppressWarnings("unchecked")
   public SablePlayerRagdollNeoForgeClient(ModContainer container, IEventBus modBus) {
      container.registerExtensionPoint(IConfigScreenFactory.class, (IConfigScreenFactory) ConfigurationScreen::new);
      RagdollClientConfig.register(container);
      RagdollCameraHelper.init();
      RagdollKeybinds.init(modBus);
      RagdollInputClient.init();
      RagdollControlsHint.init();
      RagdollGrabClient.init();
      RagdollBlockInteractClient.init();
      modBus.addListener(SablePlayerRagdollNeoForgeClient::registerEntityRenderers);
      modBus.addListener(SablePlayerRagdollNeoForgeClient::registerGuiLayers);
      NeoForge.EVENT_BUS.addListener(SablePlayerRagdollNeoForgeClient::onRenderHighlight);
   }

   private static void onRenderHighlight(RenderHighlightEvent.Block event) {
      Level level = Minecraft.getInstance().level;
      if (level == null) {
         return;
      }
      var block = level.getBlockState(event.getTarget().getBlockPos()).getBlock();
      if (block instanceof RagdollPartBlock || block instanceof MobRagdollPartBlock) {
         event.setCanceled(true);
      }
   }

   private static void registerGuiLayers(RegisterGuiLayersEvent event) {
      event.registerAboveAll(
         ResourceLocation.fromNamespaceAndPath("sable_player_ragdoll", "ragdoll_controls"),
         RagdollControlsHud::render
      );
   }

   @SuppressWarnings("unchecked")
   private static void registerEntityRenderers(RegisterRenderers event) {
      event.registerEntityRenderer((EntityType<RagdollSeatEntity>) RagdollBlockRegistration.RAGDOLL_SEAT_ENTITY.get(), RagdollSeatEntityRenderer::new);
      event.registerEntityRenderer((EntityType<RagdollDollEntity>) RagdollBlockRegistration.RAGDOLL_DOLL_ENTITY.get(), RagdollDollEntityRenderer::new);
      event.registerBlockEntityRenderer((BlockEntityType<RagdollPartBlockEntity>) RagdollBlockRegistration.RAGDOLL_PART_BLOCK_ENTITY.get(), RagdollPartBlockEntityRenderer::new);
      event.registerBlockEntityRenderer(MobRagdollBlocks.MOB_RAGDOLL_PART_ENTITY.get(), MobRagdollPartBlockEntityRenderer::new);
   }
}
