package com.moigferdsrte.ragdoll.fabric.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class FabricNetwork {
    private static boolean registered;

    private FabricNetwork() {
    }

    public static void register() {
        registerTypes();
        registerServer(RagdollTriggerPacket.TYPE, RagdollTriggerPacket::handle);
        registerServer(RagdollGrabPacket.TYPE, RagdollGrabPacket::handle);
        registerServer(RagdollInputPacket.TYPE, RagdollInputPacket::handle);
        registerServer(RagdollArmInputPacket.TYPE, RagdollArmInputPacket::handle);
        registerServer(RagdollPoseResponsePacket.TYPE, RagdollPoseResponsePacket::handle);
        registerServer(MobRagdollSpawnPacket.TYPE, MobRagdollSpawnPacket::handle);
        registerServer(MobRagdollDespawnPacket.TYPE, MobRagdollDespawnPacket::handle);
    }

    public static void registerTypes() {
        if (registered) return;
        registered = true;
        PayloadTypeRegistry.playC2S().register(RagdollTriggerPacket.TYPE, RagdollTriggerPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RagdollGrabPacket.TYPE, RagdollGrabPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RagdollInputPacket.TYPE, RagdollInputPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RagdollArmInputPacket.TYPE, RagdollArmInputPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RagdollPoseResponsePacket.TYPE, RagdollPoseResponsePacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(MobRagdollSpawnPacket.TYPE, MobRagdollSpawnPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(MobRagdollDespawnPacket.TYPE, MobRagdollDespawnPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(RagdollGrabSyncPacket.TYPE, RagdollGrabSyncPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(RagdollRequestPosePacket.TYPE, RagdollRequestPosePacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(MobRagdollLaunchRequestPacket.TYPE, MobRagdollLaunchRequestPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(MobRagdollSourceStatePacket.TYPE, MobRagdollSourceStatePacket.STREAM_CODEC);
    }

    private static <T extends CustomPacketPayload> void registerServer(CustomPacketPayload.Type<T> type,
                                                                        java.util.function.BiConsumer<T, FabricPayloadContext> handler) {
        ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> handler.accept(payload, new FabricPayloadContext(context.player())));
    }

    public static void send(ServerPlayer player, CustomPacketPayload payload) {
        if (ServerPlayNetworking.canSend(player, payload.type())) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    public static void sendToTracking(Entity entity, CustomPacketPayload payload) {
        if (!(entity.level() instanceof net.minecraft.server.level.ServerLevel)) return;
        for (ServerPlayer player : PlayerLookup.tracking(entity)) send(player, payload);
    }
}
