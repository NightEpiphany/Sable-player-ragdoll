package com.moigferdsrte.ragdoll.mob.client;

import com.moigferdsrte.ragdoll.mob.block.entity.MobRagdollPartBlockEntity;
import java.util.List;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.resources.Identifier;

public final class MobRagdollPartRenderState extends BlockEntityRenderState {
    final BlockModelRenderState fallbackModel = new BlockModelRenderState();
    Identifier texture;
    List<MobRagdollPartBlockEntity.Quad> quads = List.of();
    float xSize;
    float ySize;
    float zSize;
    float renderQx;
    float renderQy;
    float renderQz;
    float renderQw = 1.0F;
    boolean renderAnchor;

    void extract(MobRagdollPartBlockEntity blockEntity) {
        this.texture = blockEntity.texture();
        this.quads = blockEntity.quads().stream()
                .map(quad -> new MobRagdollPartBlockEntity.Quad(List.copyOf(quad.vertices()), quad.normalX(), quad.normalY(), quad.normalZ()))
                .toList();
        this.xSize = blockEntity.xSize();
        this.ySize = blockEntity.ySize();
        this.zSize = blockEntity.zSize();
        this.renderQx = blockEntity.renderQx();
        this.renderQy = blockEntity.renderQy();
        this.renderQz = blockEntity.renderQz();
        this.renderQw = blockEntity.renderQw();
        this.renderAnchor = blockEntity.renderAnchor();
    }
}
