package com.moigferdsrte.ragdoll.fabric;

import com.moigferdsrte.ragdoll.block.RagdollPartBlock;
import com.moigferdsrte.ragdoll.entity.RagdollSeatEntities;
import com.moigferdsrte.ragdoll.fabric.client.*;
import com.moigferdsrte.ragdoll.fabric.network.FabricClientNetwork;
import com.moigferdsrte.ragdoll.block.entity.RagdollPartBlockEntities;
import com.moigferdsrte.ragdoll.fabric.network.RagdollTriggerPacket;
import com.moigferdsrte.ragdoll.mob.MobRagdollBlocks;
import com.moigferdsrte.ragdoll.mob.block.MobRagdollPartBlock;
import com.moigferdsrte.ragdoll.mob.client.MobRagdollPartBlockEntityRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.phys.BlockHitResult;

@Environment(EnvType.CLIENT)
public class SablePlayerRagdollFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricClientNetwork.register();
        RagdollClientConfig.load();
        RagdollCameraHelper.init();
        RagdollControlsHint.init();
        HudElementRegistry.addLast(FabricRegistries.id("controls_hud"), RagdollControlsHud::render);
        LevelExtractionEvents.AFTER_BLOCK_OUTLINE_EXTRACTION.register((context, hit) -> {
            if (hit instanceof BlockHitResult blockHit) {
                var block = context.level().getBlockState(blockHit.getBlockPos()).getBlock();
                if (block instanceof RagdollPartBlock || block instanceof MobRagdollPartBlock) {
                    context.levelState().blockOutlineRenderState = null;
                }
            }
        });
        RagdollGrabClient.init();
        KeyMapping key = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.sable_player_ragdoll.ragdoll", InputConstants.Type.KEYBOARD, InputConstants.KEY_H,
                KeyMapping.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            RagdollClientConfig.tick();
            RagdollInputClient.tick(client);
            RagdollGrabClient.tick(client);
            while (key.consumeClick() && client.player != null) {
                FabricClientNetwork.sendToServer(new RagdollTriggerPacket(
                        RagdollClientPoseCapture.capture(), client.player.yBodyRot));
            }
        });
        EntityRendererRegistry.register(RagdollSeatEntities.ragdollSeat(), RagdollSeatEntityRenderer::new);
        EntityRendererRegistry.register(RagdollSeatEntities.ragdollDoll(), RagdollDollEntityRenderer::new);
        BlockEntityRenderers.register(RagdollPartBlockEntities.ragdollPart(), RagdollPartBlockEntityRenderer::new);
        BlockEntityRenderers.register(MobRagdollBlocks.MOB_RAGDOLL_PART_ENTITY, MobRagdollPartBlockEntityRenderer::new);
    }
}
