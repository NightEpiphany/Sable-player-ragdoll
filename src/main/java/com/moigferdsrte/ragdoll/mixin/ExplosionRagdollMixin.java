package com.moigferdsrte.ragdoll.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.moigferdsrte.ragdoll.mob.MobRagdollAssembly;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerExplosion.class)
abstract class ExplosionRagdollMixin {
    @WrapOperation(
            method = "explode",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerExplosion;calculateExplodedPositions()Ljava/util/List;")
    )
    private List<BlockPos> ragdoll$explodeParts(ServerExplosion explosion, Operation<List<BlockPos>> original) {
        if (explosion.getDirectSourceEntity() instanceof Creeper creeper
                && creeper.level() instanceof ServerLevel level
                && MobRagdollAssembly.isConverted(creeper)) {
            MobRagdollAssembly.explodeCreeperRagdoll(level, creeper, explosion.center(), explosion.radius());
            // ServerExplosion.explode still applies entity damage and knockback, but its
            // block interaction and fire phases consume this list, so keep both disabled.
            return List.of();
        }
        return original.call(explosion);
    }
}
