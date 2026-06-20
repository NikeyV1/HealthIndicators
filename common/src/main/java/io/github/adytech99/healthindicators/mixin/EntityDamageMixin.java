package io.github.adytech99.healthindicators.mixin;

import io.github.adytech99.healthindicators.DamageDirectionIndicatorRenderer;
import io.github.adytech99.healthindicators.HealthIndicatorsCommon;
import io.github.adytech99.healthindicators.RenderTracker;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// TODO[26.2-verify]: target method name "onDamaged" and the @Inject descriptor are Yarn-derived.
// Under non-obfuscated 26.2/Mojmap, re-derive the actual method name + descriptor from MC sources
// (the param package is already updated to world/damagesource/DamageSource). Reassess remap=false.
@Mixin(value = LivingEntity.class, remap = false)
public class EntityDamageMixin {
    @Inject(method = "onDamaged(Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At("TAIL"))
    private void onEntityDamage(DamageSource damageSource, CallbackInfo callbackInfo) {
        RenderTracker.onDamage(damageSource, ((LivingEntity) (Object) this));

        if (damageSource.getEntity() instanceof LivingEntity attacker && attacker != null && (Object) this == HealthIndicatorsCommon.client.player) {
            DamageDirectionIndicatorRenderer.markDamageToPlayer(attacker);
        }
    }
}
