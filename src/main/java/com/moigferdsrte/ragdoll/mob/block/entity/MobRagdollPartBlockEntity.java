package com.moigferdsrte.ragdoll.mob.block.entity;

import com.moigferdsrte.ragdoll.physics.RagdollBlockIdentity;
import com.moigferdsrte.ragdoll.physics.RagdollBlockOwnership;
import com.moigferdsrte.ragdoll.physics.RagdollOwnedBlock;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.moigferdsrte.ragdoll.RagdollGrabCallbacks;
import com.moigferdsrte.ragdoll.physics.RagdollAssemblyHelper;
import com.moigferdsrte.ragdoll.physics.SableConstraintCompat;
import com.moigferdsrte.ragdoll.mob.MobRagdollBlocks;
import com.moigferdsrte.ragdoll.mob.MobRagdollAssembly;
import com.moigferdsrte.ragdoll.mob.block.MobPartRole;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.Block;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public final class MobRagdollPartBlockEntity extends BlockEntity implements BlockEntitySubLevelActor, RagdollOwnedBlock {
    private static final Codec<Vertex> VERTEX_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("X", 0.0F).forGetter(Vertex::x),
            Codec.FLOAT.optionalFieldOf("Y", 0.0F).forGetter(Vertex::y),
            Codec.FLOAT.optionalFieldOf("Z", 0.0F).forGetter(Vertex::z),
            Codec.FLOAT.optionalFieldOf("U", 0.0F).forGetter(Vertex::u),
            Codec.FLOAT.optionalFieldOf("V", 0.0F).forGetter(Vertex::v)
    ).apply(instance, Vertex::new));
    private static final Codec<Quad> QUAD_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            VERTEX_CODEC.listOf().optionalFieldOf("Vertices", List.of()).forGetter(Quad::vertices),
            Codec.FLOAT.optionalFieldOf("NX", 0.0F).forGetter(Quad::normalX),
            Codec.FLOAT.optionalFieldOf("NY", 0.0F).forGetter(Quad::normalY),
            Codec.FLOAT.optionalFieldOf("NZ", 0.0F).forGetter(Quad::normalZ)
    ).apply(instance, Quad::new));

    private final RagdollBlockIdentity ragdollIdentity = new RagdollBlockIdentity();

    @Override
    public RagdollBlockIdentity ragdollIdentity() { return ragdollIdentity; }

    @Override
    public void setRagdollIdentity(UUID owner, UUID limb, String kind) {
        ragdollIdentity.assign(owner, limb, kind);
        setChanged();
    }

    @Override
    public void markSevered() {
        removeAllGrabbers();
        ragdollIdentity.sever(level == null ? 0 : level.getGameTime());
        sourceEntityId = null;
        sourceEntityNetworkId = -1;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private static final double GRAB_STIFFNESS = 500.0;
    private static final double GRAB_DAMPING = 50.0;
    private static final double GRAB_MAX_FORCE = 200.0;
    private static final double GRAB_HOLD_DISTANCE = 1.0;
    private static final double GRAB_ANCHOR_Y_OFFSET = -0.6;
    private static final double GRAB_MAX_DISTANCE = 4.0;
    private Identifier texture = Identifier.withDefaultNamespace("textures/block/light_blue_stained_glass.png");
    private Identifier entityType;
    private String partName = "";
    private List<String> keepPartNames = List.of();
    private List<Quad> quads = List.of();
    private CompoundTag variantData;
    private UUID sourceEntityId;
    private int sourceEntityNetworkId = -1;
    private boolean baby;
    private float renderScale = 1.0F;
    private float renderQx;
    private float renderQy;
    private float renderQz;
    private float renderQw = 1.0F;
    private MobPartRole role = MobPartRole.OTHER;
    private boolean renderAnchor = true;
    private float visualXSize = 8.0F;
    private float visualYSize = 8.0F;
    private float visualZSize = 8.0F;
    private final Map<UUID, GrabConstraint> grabbers = new HashMap<>();

    public MobRagdollPartBlockEntity(BlockPos pos, BlockState blockState) {
        super(MobRagdollBlocks.MOB_RAGDOLL_PART_ENTITY, pos, blockState);
    }

    public void configure(Identifier texture, List<Quad> quads, Identifier entityType, UUID sourceEntityId, int sourceEntityNetworkId, String partName, List<String> keepPartNames, CompoundTag variantData, boolean baby, float renderScale, float renderQx, float renderQy, float renderQz, float renderQw, MobPartRole role, float visualXSize, float visualYSize, float visualZSize) {
        this.texture = texture;
        this.quads = List.copyOf(quads);
        this.entityType = entityType;
        this.sourceEntityId = sourceEntityId;
        this.sourceEntityNetworkId = sourceEntityNetworkId;
        this.partName = partName == null ? "" : partName;
        this.keepPartNames = List.copyOf(keepPartNames == null ? List.of() : keepPartNames);
        this.variantData = sanitizedVariantData(variantData);
        this.baby = baby;
        this.renderScale = renderScale;
        this.renderQx = renderQx;
        this.renderQy = renderQy;
        this.renderQz = renderQz;
        this.renderQw = renderQw;
        this.role = role == null ? MobPartRole.OTHER : role;
        this.renderAnchor = true;
        this.visualXSize = Math.max(1.0F, visualXSize);
        this.visualYSize = Math.max(1.0F, visualYSize);
        this.visualZSize = Math.max(1.0F, visualZSize);
        this.setChanged();
    }

    public void configureCollisionOnly() {
        this.texture = Identifier.withDefaultNamespace("textures/block/light_blue_stained_glass.png");
        this.quads = List.of();
        this.entityType = null;
        this.sourceEntityId = null;
        this.sourceEntityNetworkId = -1;
        this.partName = "";
        this.keepPartNames = List.of();
        this.variantData = null;
        this.baby = false;
        this.renderScale = 1.0F;
        this.renderQx = 0.0F;
        this.renderQy = 0.0F;
        this.renderQz = 0.0F;
        this.renderQw = 1.0F;
        this.role = MobPartRole.OTHER;
        this.renderAnchor = false;
        this.visualXSize = 1.0F;
        this.visualYSize = 1.0F;
        this.visualZSize = 1.0F;
        this.setChanged();
    }

    public Identifier texture() {
        return this.texture;
    }

    public List<Quad> quads() {
        return this.quads;
    }

    public Identifier entityType() {
        return this.entityType;
    }

    public UUID sourceEntityId() {
        return this.sourceEntityId;
    }

    public int sourceEntityNetworkId() {
        return this.sourceEntityNetworkId;
    }

    public String partName() {
        return this.partName;
    }

    public List<String> keepPartNames() {
        return this.keepPartNames;
    }

    public CompoundTag variantData() {
        return this.variantData;
    }

    public boolean baby() {
        return this.baby;
    }

    public float renderScale() {
        return this.renderScale;
    }

    public float renderQx() {
        return this.renderQx;
    }

    public float renderQy() {
        return this.renderQy;
    }

    public float renderQz() {
        return this.renderQz;
    }

    public float renderQw() {
        return this.renderQw;
    }

    public MobPartRole role() {
        return this.role;
    }

    public boolean renderAnchor() {
        return this.renderAnchor;
    }

    public void startGrab(UUID playerId) {
        if (!this.grabbers.containsKey(playerId)) {
            this.grabbers.put(playerId, new GrabConstraint(playerId));
            this.markSourceGrabbed();
            this.setChanged();
        }
    }

    public void stopGrab(UUID playerId) {
        GrabConstraint constraint = this.grabbers.remove(playerId);
        if (constraint != null) {
            constraint.removeJoint();
            this.markSourceReleased();
            this.setChanged();
        }
    }

    public VoxelShape visualShape() {
        return Block.box(
                (16.0F - this.visualXSize) * 0.5F,
                (16.0F - this.visualYSize) * 0.5F,
                (16.0F - this.visualZSize) * 0.5F,
                (16.0F + this.visualXSize) * 0.5F,
                (16.0F + this.visualYSize) * 0.5F,
                (16.0F + this.visualZSize) * 0.5F
        );
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        if (!this.ragdollIdentity.severed() && this.renderAnchor
                && subLevel.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            com.moigferdsrte.ragdoll.physics.RagdollRelationships.withBlock(this, () ->
                    MobRagdollAssembly.tickPart(serverLevel, this, handle));
        }
        this.checkGrabbers();
        for (GrabConstraint constraint : this.grabbers.values()) {
            constraint.physicsTick(subLevel);
        }

    }

    private void checkGrabbers() {
        if (this.level == null || this.grabbers.isEmpty()) {
            return;
        }

        Vector3d center = this.grabCenter();
        for (Iterator<Map.Entry<UUID, GrabConstraint>> it = this.grabbers.entrySet().iterator(); it.hasNext();) {
            Map.Entry<UUID, GrabConstraint> entry = it.next();
            Player player = this.level.getPlayerByUUID(entry.getKey());
            boolean invalid = player == null || player.isDeadOrDying() || player.isSpectator();
            if (!invalid) {
                double distanceSq = Sable.HELPER.distanceSquaredWithSubLevels(
                        this.level, JOMLConversion.toJOML(player.getEyePosition()), center);
                invalid = distanceSq > GRAB_MAX_DISTANCE * GRAB_MAX_DISTANCE;
            }
            if (invalid) {
                entry.getValue().removeJoint();
                it.remove();
                this.markSourceReleased();
                this.notifyReleased(entry.getKey());
                this.setChanged();
            }
        }
    }

    private void removeAllGrabbers() {
        if (!this.grabbers.isEmpty()) {
            for (Map.Entry<UUID, GrabConstraint> entry : this.grabbers.entrySet()) {
                entry.getValue().removeJoint();
                this.markSourceReleased();
                this.notifyReleased(entry.getKey());
            }
            this.grabbers.clear();
            this.setChanged();
        }
    }

    private void notifyReleased(UUID playerId) {
        if (this.level != null && this.level.getPlayerByUUID(playerId) instanceof ServerPlayer serverPlayer) {
            RagdollGrabCallbacks.notifyReleased(serverPlayer);
        }
    }

    private void markSourceGrabbed() {
        if (this.ragdollIdentity.owner() != null && this.level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            MobRagdollAssembly.markGrabbed(serverLevel, this.ragdollIdentity.owner());
        }
    }

    private void markSourceReleased() {
        if (this.ragdollIdentity.owner() != null && this.level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            MobRagdollAssembly.markReleased(serverLevel, this.ragdollIdentity.owner());
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        com.moigferdsrte.ragdoll.physics.RagdollRelationships.forget(this);
        this.removeAllGrabbers();
    }

    private Vector3d grabCenter() {
        return JOMLConversion.atCenterOf(this.getBlockPos());
    }

    public float xSize() {
        return this.visualXSize / 16.0F;
    }

    public float ySize() {
        return this.visualYSize / 16.0F;
    }

    public float zSize() {
        return this.visualZSize / 16.0F;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ragdollIdentity.save(output);
        output.putString("Texture", this.texture.toString());
        if (this.entityType != null) {
            output.putString("EntityType", this.entityType.toString());
        }
        output.storeNullable("SourceEntityId", UUIDUtil.CODEC, this.sourceEntityId);
        output.putInt("SourceEntityNetworkId", this.sourceEntityNetworkId);
        output.putString("PartName", this.partName);
        if (this.variantData != null) {
            output.store("VariantData", CompoundTag.CODEC, this.variantData);
        }
        output.putBoolean("Baby", this.baby);
        output.putFloat("RenderScale", this.renderScale);
        output.putFloat("RenderQx", this.renderQx);
        output.putFloat("RenderQy", this.renderQy);
        output.putFloat("RenderQz", this.renderQz);
        output.putFloat("RenderQw", this.renderQw);
        output.putString("Role", this.role.getSerializedName());
        output.putBoolean("RenderAnchor", this.renderAnchor);
        output.putFloat("VisualXSize", this.visualXSize);
        output.putFloat("VisualYSize", this.visualYSize);
        output.putFloat("VisualZSize", this.visualZSize);
        output.store("KeepPartNames", Codec.STRING.listOf(), this.keepPartNames);
        output.store("Quads", QUAD_CODEC.listOf(), this.quads);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ragdollIdentity.load(input);
        Identifier parsed = Identifier.tryParse(input.getStringOr("Texture", ""));
        this.texture = parsed == null ? Identifier.withDefaultNamespace("textures/block/light_blue_stained_glass.png") : parsed;
        this.entityType = Identifier.tryParse(input.getStringOr("EntityType", ""));
        this.sourceEntityId = input.read("SourceEntityId", UUIDUtil.CODEC).orElse(null);
        this.sourceEntityNetworkId = input.getIntOr("SourceEntityNetworkId", -1);
        if (ragdollIdentity.severed()) {
            this.sourceEntityId = null;
            this.sourceEntityNetworkId = -1;
        }
        this.partName = input.getStringOr("PartName", "");
        this.variantData = input.read("VariantData", CompoundTag.CODEC).map(MobRagdollPartBlockEntity::sanitizedVariantData).orElse(null);
        this.baby = input.getBooleanOr("Baby", false);
        this.renderScale = input.getFloatOr("RenderScale", 1.0F);
        this.renderQx = input.getFloatOr("RenderQx", 0.0F);
        this.renderQy = input.getFloatOr("RenderQy", 0.0F);
        this.renderQz = input.getFloatOr("RenderQz", 0.0F);
        this.renderQw = input.getFloatOr("RenderQw", 1.0F);
        this.role = MobPartRole.byName(input.getStringOr("Role", ""));
        this.renderAnchor = input.getBooleanOr("RenderAnchor", true);
        this.visualXSize = Math.max(1.0F, input.getFloatOr("VisualXSize", 8.0F));
        this.visualYSize = Math.max(1.0F, input.getFloatOr("VisualYSize", 8.0F));
        this.visualZSize = Math.max(1.0F, input.getFloatOr("VisualZSize", 8.0F));
        this.keepPartNames = input.listOrEmpty("KeepPartNames", Codec.STRING).stream().toList();
        this.quads = input.listOrEmpty("Quads", QUAD_CODEC).stream()
                .filter(quad -> quad.vertices().size() == 4)
                .toList();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    public record Quad(List<Vertex> vertices, float normalX, float normalY, float normalZ) {
    }

    public record Vertex(float x, float y, float z, float u, float v) {
    }

    private static CompoundTag sanitizedVariantData(CompoundTag variantData) {
        if (variantData == null) {
            return null;
        }
        CompoundTag copy = variantData.copy();
        copy.remove("Age");
        copy.remove("ForcedAge");
        copy.remove("InLove");
        copy.remove("Health");
        copy.remove("HurtTime");
        copy.remove("DeathTime");
        return copy;
    }

    private final class GrabConstraint {
        private final UUID playerId;
        private PhysicsConstraintHandle constraintHandle;

        private GrabConstraint(UUID playerId) {
            this.playerId = playerId;
        }

        private void physicsTick(ServerSubLevel subLevel) {
            this.removeJoint();
            if (MobRagdollPartBlockEntity.this.level == null) {
                return;
            }

            Player player = MobRagdollPartBlockEntity.this.level.getPlayerByUUID(this.playerId);
            if (player == null || player.isDeadOrDying() || player.isSpectator()) {
                return;
            }

            SubLevel standingSubLevel = Sable.HELPER.getTrackingSubLevel(player);
            if (standingSubLevel != null
                    && (RagdollAssemblyHelper.isRagdollPart(subLevel.getLevel(), standingSubLevel.getUniqueId())
                    || MobRagdollAssembly.isRagdollPart(subLevel.getLevel(), standingSubLevel.getUniqueId()))) {
                return;
            }

            Vector3d constraintGoal = JOMLConversion.toJOML(player.getEyePosition().add(0, GRAB_ANCHOR_Y_OFFSET, 0).add(player.getLookAngle().scale(GRAB_HOLD_DISTANCE)));
            Vector3d constraintPosition = MobRagdollPartBlockEntity.this.grabCenter();
            double validRange = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).getValue() + 2.0;
            double currentDistance = Sable.HELPER.distanceSquaredWithSubLevels(MobRagdollPartBlockEntity.this.level, constraintGoal, constraintPosition);
            if (currentDistance > validRange * validRange) {
                return;
            }

            ServerSubLevelContainer container = SubLevelContainer.getContainer(subLevel.getLevel());
            if (container == null) {
                return;
            }

            SubLevelPhysicsSystem physicsSystem = container.physicsSystem();
            this.constraintHandle = SableConstraintCompat.addConstraint(
                    physicsSystem.getPipeline(), null, subLevel,
                    SableConstraintCompat.free(constraintGoal, constraintPosition, new Quaterniond()));

            for (ConstraintJointAxis axis : ConstraintJointAxis.LINEAR) {
                this.constraintHandle.setMotor(axis, 0.0, GRAB_STIFFNESS, GRAB_DAMPING, true, GRAB_MAX_FORCE);
            }
        }

        private void removeJoint() {
            if (this.constraintHandle != null) {
                this.constraintHandle.remove();
                this.constraintHandle = null;
            }
        }
    }
}
