package com.moigferdsrte.ragdoll.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.moigferdsrte.ragdoll.fabric.event.RagdollInteractions;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(Projectile.class)
abstract class ProjectileRagdollMixin {
    @WrapMethod(method = "hitTargetOrDeflectSelf")
    private ProjectileDeflection ragdoll$pipeImpact(HitResult hit, Operation<ProjectileDeflection> original) {
        return RagdollInteractions.onProjectileImpact((Projectile) (Object) this, hit) ? ProjectileDeflection.NONE : original.call(hit);
    }
}
