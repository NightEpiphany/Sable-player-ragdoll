package com.moigferdsrte.ragdoll.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.moigferdsrte.ragdoll.mob.MobRagdollAssembly;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(Explosion.class)
abstract class ExplosionRagdollMixin {
    @WrapMethod(method = "explode")
    private void ragdoll$explodeParts(Operation<Void> original) {
        original.call();
        Explosion explosion = (Explosion) (Object) this;
        if (explosion.getDirectSourceEntity() instanceof Creeper creeper && creeper.level() instanceof ServerLevel level
                && MobRagdollAssembly.isConverted(creeper)) {
            MobRagdollAssembly.explodeCreeperRagdoll(level, creeper, explosion.center(), explosion.radius());
            explosion.getToBlow().clear();
        }
    }
}
