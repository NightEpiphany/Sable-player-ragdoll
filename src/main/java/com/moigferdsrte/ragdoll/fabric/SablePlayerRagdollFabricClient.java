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
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class SablePlayerRagdollFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricClientNetwork.register();
        RagdollClientConfig.load();
        RagdollCameraHelper.init();
        RagdollControlsHint.init();
        HudRenderCallback.EVENT.register(RagdollControlsHud::render);
        WorldRenderEvents.BLOCK_OUTLINE.register((context, outline) -> {
            var block = context.world().getBlockState(outline.blockPos()).getBlock();
            return !(block instanceof RagdollPartBlock)
                    && !(block instanceof MobRagdollPartBlock);
        });
        RagdollGrabClient.init();
        KeyMapping key = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.sable_player_ragdoll.ragdoll", GLFW.GLFW_KEY_H, "key.categories.sable_player_ragdoll"));
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
