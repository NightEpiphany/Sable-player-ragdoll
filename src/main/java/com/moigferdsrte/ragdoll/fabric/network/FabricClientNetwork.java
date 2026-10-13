package com.moigferdsrte.ragdoll.fabric.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class FabricClientNetwork {
    private FabricClientNetwork() {
    }

    public static void register() {
        FabricNetwork.registerTypes();
        ClientPlayNetworking.registerGlobalReceiver(RagdollGrabSyncPacket.TYPE, (payload, context) ->
                RagdollGrabSyncPacket.handle(payload, new FabricPayloadContext(context.player())));
        ClientPlayNetworking.registerGlobalReceiver(RagdollRequestPosePacket.TYPE, (payload, context) ->
                RagdollRequestPosePacket.handle(payload, new FabricPayloadContext(context.player())));
        ClientPlayNetworking.registerGlobalReceiver(MobRagdollLaunchRequestPacket.TYPE, (payload, context) ->
                MobRagdollLaunchRequestPacket.handle(payload, new FabricPayloadContext(context.player())));
        ClientPlayNetworking.registerGlobalReceiver(MobRagdollSourceStatePacket.TYPE, (payload, context) ->
                MobRagdollSourceStatePacket.handle(payload, new FabricPayloadContext(context.player())));
    }

    public static void sendToServer(CustomPacketPayload payload) {
        if (ClientPlayNetworking.canSend(payload.type())) ClientPlayNetworking.send(payload);
    }
}
