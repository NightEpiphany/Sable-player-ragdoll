package com.moigferdsrte.ragdoll.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelSerializer;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Prevents a save from crashing if a stale Sable sub-level survives until
 * serialization with its native Rapier body already removed.
 */
@Mixin(value = SubLevelSerializer.class, remap = false)
public abstract class SableSubLevelSerializerMixin {
    @WrapOperation(
            method = "serialize",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/api/physics/handle/RigidBodyHandle;getLinearVelocity(Lorg/joml/Vector3d;)Lorg/joml/Vector3d;"
            )
    )
    private static Vector3d sablePlayerRagdoll$guardLinearVelocity(
            RigidBodyHandle handle,
            Vector3d destination,
            Operation<Vector3d> original
    ) {
        return handle.isValid() ? original.call(handle, destination) : destination.zero();
    }

    @WrapOperation(
            method = "serialize",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/api/physics/handle/RigidBodyHandle;getAngularVelocity(Lorg/joml/Vector3d;)Lorg/joml/Vector3d;"
            )
    )
    private static Vector3d sablePlayerRagdoll$guardAngularVelocity(
            RigidBodyHandle handle,
            Vector3d destination,
            Operation<Vector3d> original
    ) {
        return handle.isValid() ? original.call(handle, destination) : destination.zero();
    }
}
