package io.github.adytech99.healthindicators;

import io.github.adytech99.healthindicators.config.Config;
import io.github.adytech99.healthindicators.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RenderTracker {
    private static final Minecraft client = Minecraft.getInstance();
    private static final ConcurrentHashMap<UUID, Integer> UUIDS = new ConcurrentHashMap<>();
    public static boolean after_attack = ModConfig.HANDLER.instance().after_attack;

    private static LivingEntity trackedEntity;

    public static LivingEntity getTrackedEntity() {
        return trackedEntity;
    }

    public static void setTrackedEntity(LivingEntity trackedEntity) {
        RenderTracker.trackedEntity = trackedEntity;
    }


    public static void tick(Minecraft client){
        if(client.player == null || client.level == null) return;
        if(Config.getRenderingEnabled()) {
            for (Entity entity : client.level.entitiesForRendering()) {
                if (entity instanceof LivingEntity livingEntity && satisfiesAdvancedCriteria(client.player, livingEntity) && satisfiesList(client.player, livingEntity)) {
                    addToUUIDS(livingEntity);
                } else removeFromUUIDS(entity.getUUID());
            }
        }
        trimEntities(client.level);
        if(ModConfig.HANDLER.instance().after_attack != after_attack) {
            UUIDS.clear();
            after_attack = ModConfig.HANDLER.instance().after_attack;
        }
        if(getTrackedEntity() == null || getTrackedEntity().isDeadOrDying() || getTrackedEntity().isRemoved()) setTrackedEntity(null);
    }

    public static void onDamage(DamageSource damageSource, LivingEntity livingEntity) {
        // TODO[26.2-verify]: DamageSource.getAttacker() -> getEntity() (causing entity).
        if(damageSource.getEntity() instanceof Player){
            assert client.level != null;
            if(ModConfig.HANDLER.instance().after_attack && livingEntity instanceof LivingEntity && RenderTracker.isEntityTypeAllowed(livingEntity, client.player) && satisfiesList(client.player, livingEntity)) {
                if (!addToUUIDS(livingEntity)) {
                    UUIDS.replace(livingEntity.getUUID(), (ModConfig.HANDLER.instance().time_after_hit * 20));
                }
            }
        }
    }


    public static void trimEntities(ClientLevel world) {
        Iterator<Map.Entry<UUID, Integer>> iterator = UUIDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            entry.setValue(entry.getValue() - 1);
            if (entry.getValue() <= 0) {
                iterator.remove();
            }
        }

        UUIDS.entrySet().removeIf(entry -> isInvalid(getEntityFromUUID(entry.getKey(), world))|| !Config.getRenderingEnabled() );
        if(UUIDS.size() >= 1536) UUIDS.clear();
    }


    public static void removeFromUUIDS(Entity entity){
        UUIDS.remove(entity.getUUID());
    }
    public static void removeFromUUIDS(UUID uuid){
        UUIDS.remove(uuid);
    }

    public static boolean addToUUIDS(LivingEntity livingEntity){
        if(!UUIDS.containsKey(livingEntity.getUUID())){
            UUIDS.put(livingEntity.getUUID(), ModConfig.HANDLER.instance().after_attack ? (ModConfig.HANDLER.instance().time_after_hit * 20) : 2400);
            return true;
        }
        else return false;
    }

    public static boolean isInUUIDS(LivingEntity livingEntity){
        return UUIDS.containsKey(livingEntity.getUUID());
    }

    public static boolean overridePlayers(LocalPlayer playerEntity, LivingEntity livingEntity){
        return (ModConfig.HANDLER.instance().override_players && livingEntity instanceof Player && livingEntity != client.player)
                || (livingEntity == client.player && ModConfig.HANDLER.instance().self);
    }

    public static boolean isEntityTypeAllowed(LivingEntity livingEntity, Player self){
        // TODO[26.2-verify]: Yarn PassiveEntity -> Mojmap AgeableMob; HostileEntity -> Monster.
        if(!ModConfig.HANDLER.instance().passive_mobs && livingEntity instanceof AgeableMob) return false;
        if(!ModConfig.HANDLER.instance().hostile_mobs && livingEntity instanceof Monster) return false;
        if(!ModConfig.HANDLER.instance().players && livingEntity instanceof Player) return false;
        if(!ModConfig.HANDLER.instance().self && livingEntity == self) return false;
        return true;
    }

    public static boolean satisfiesAdvancedCriteria(LocalPlayer player, LivingEntity livingEntity){
        if(overridePlayers(player, livingEntity)) return true;

        if(!isEntityTypeAllowed(livingEntity, player)) return false;
        if(ModConfig.HANDLER.instance().after_attack && !UUIDS.containsKey(livingEntity.getUUID())) return false;
        if(ModConfig.HANDLER.instance().damaged_only && (livingEntity.getHealth() == livingEntity.getMaxHealth() || livingEntity.getHealth() > livingEntity.getMaxHealth()*((float) ModConfig.HANDLER.instance().max_health_percentage / 100)) && livingEntity.getAbsorptionAmount() <= 0) return false;
        if(ModConfig.HANDLER.instance().looking_at && !isTargeted(livingEntity)) return false;
        if(ModConfig.HANDLER.instance().within_distance && livingEntity.distanceTo(player) > ModConfig.HANDLER.instance().distance) return false;

        return !isInvalid(livingEntity);
    }

    public static boolean satisfiesList(LocalPlayer player, LivingEntity livingEntity){
        if(!ModConfig.HANDLER.instance().blacklistOrWhitelist){
            if(ModConfig.HANDLER.instance().list.isEmpty()) return true;
            if(ModConfig.HANDLER.instance().list.contains("minecraft:player") && livingEntity instanceof Player) return true;
        }

        String[] blacklist1 = new String[ModConfig.HANDLER.instance().list.size()];
        for(int i = 0; i < ModConfig.HANDLER.instance().list.size(); i++){
            blacklist1[i] = ModConfig.HANDLER.instance().list.get(i);
        }

        if(ModConfig.HANDLER.instance().blacklistOrWhitelist) return Arrays.stream(blacklist1).noneMatch(s -> {
            if(livingEntity instanceof Player) return Component.literal(s).equals(Objects.requireNonNull(livingEntity.getName()));
            else return s.equals(EntityType.getKey(livingEntity.getType()).toString());
        });
        else return Arrays.stream(blacklist1).anyMatch(s -> {
            if(livingEntity instanceof Player) return Component.literal(s).equals(Objects.requireNonNull(livingEntity.getName()));
            else return s.equals(EntityType.getKey(livingEntity.getType()).toString());
        });
    }

    public static boolean isTargeted(LivingEntity livingEntity){
        Entity camera = client.getCameraEntity();
        double d = ModConfig.HANDLER.instance().reach;
        double e = Mth.square(d);
        // TODO[26.2-verify]: getCameraPosVec(t) -> getEyePosition(t); raycast(d,t,f) -> pick(d,t,f).
        Vec3 vec3d = camera.getEyePosition(0.0F);
        HitResult hitResult = camera.pick(d, 0.0F, false);
        double f = hitResult.getLocation().distanceToSqr(vec3d);
        if (hitResult.getType() != HitResult.Type.MISS) {
            e = f;
            d = Math.sqrt(e);
        }
        Vec3 vec3d2 = camera.getViewVector(0.0F);
        Vec3 vec3d3 = vec3d.add(vec3d2.x * d, vec3d2.y * d, vec3d2.z * d);
        AABB box = camera.getBoundingBox().expandTowards(vec3d2.scale(d)).inflate(1.0, 1.0, 1.0);
        assert client.getCameraEntity() != null;
        // TODO[26.2-verify]: ProjectileUtil.raycast -> getEntityHitResult; canHit() -> isPickable().
        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(client.getCameraEntity(), vec3d, vec3d3, box, entity -> !entity.isSpectator() && entity.isPickable(), e);

        if (entityHitResult != null && entityHitResult.getEntity() instanceof LivingEntity livingEntity1){
            return livingEntity1 == livingEntity;
        }
        return false;
    }

    public static boolean isOkayToRenderThroughWalls(LivingEntity livingEntity){
        return isTargeted(livingEntity) && !(livingEntity instanceof Player);
    }

    public static boolean isInvalid(Entity entity){
        // TODO[26.2-verify]: Yarn isLiving() -> (entity instanceof LivingEntity); isRegionUnloaded() has
        // no obvious Mojmap equal — confirm/replace (kept as-is so the local build surfaces it).
        return (entity == null
                || !entity.isAlive()
                || !(entity instanceof LivingEntity)
                || entity.isRegionUnloaded()
                || client.player == null
                || client.player.getVehicle() == entity
                || (!ServerPermissions.allowsInvisiblePlayers() && entity.isInvisibleTo(client.player)));
    }
    private static Entity getEntityFromUUID(UUID uuid, ClientLevel world) {
        for (Entity entity : world.entitiesForRendering()) {
            if (entity.getUUID().equals(uuid)) {
                return entity;
            }
        }
        return null;
    }
}
