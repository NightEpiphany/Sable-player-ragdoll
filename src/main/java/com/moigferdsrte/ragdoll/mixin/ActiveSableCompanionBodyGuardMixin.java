package com.moigferdsrte.ragdoll.mixin;

import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(value = ActiveSableCompanion.class, remap = false)
public abstract class ActiveSableCompanionBodyGuardMixin {

    @WrapMethod(method = "getVelocity(Lnet/minecraft/world/level/Level;Ldev/ryanhcode/sable/companion/SubLevelAccess;Lorg/joml/Vector3dc;Lorg/joml/Vector3d;)Lorg/joml/Vector3d;")
    private Vector3d sablePlayerRagdoll$guardRemovedSubLevel(Level level, SubLevelAccess subLevel, Vector3dc pos, Vector3d dest, Operation<Vector3d> original) {
        if (subLevel instanceof ServerSubLevel serverSubLevel && serverSubLevel.isRemoved()) {
            return dest.zero();
        }

        return original.call(level, subLevel, pos, dest);
    }
}
