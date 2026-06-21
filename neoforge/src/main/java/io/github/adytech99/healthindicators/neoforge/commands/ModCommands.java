package io.github.adytech99.healthindicators.neoforge.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.adytech99.healthindicators.HealthIndicatorsCommon;
import io.github.adytech99.healthindicators.RenderTracker;
import io.github.adytech99.healthindicators.enums.HealthDisplayTypeEnum;
import io.github.adytech99.healthindicators.config.ModConfig;
import io.github.adytech99.healthindicators.util.ConfigUtils;
import io.github.adytech99.healthindicators.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.Objects;

public class ModCommands {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterClientCommandsEvent event) {
        registerConfigCommands(event.getDispatcher());
        registerOpenConfigCommand(event.getDispatcher());
    }
    public static void registerConfigCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("healthindicators")
                .then(Commands.literal("offset")
                        .then(Commands.argument("offset", DoubleArgumentType.doubleArg())
                                .executes(context -> {
                                    ModConfig.HANDLER.instance().display_offset = DoubleArgumentType.getDouble(context, "offset");
                                    ModConfig.HANDLER.save();
                                    ConfigUtils.sendMessage(Minecraft.getInstance().player, Component.literal("Set heart offset to " + Util.truncate(ModConfig.HANDLER.instance().display_offset, 2)));
                                    return 1;
                                })))

                .then(Commands.literal("indicator-type")
                        .then(Commands.argument("indicator_type", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    builder.suggest("heart");
                                    builder.suggest("number");
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    HealthDisplayTypeEnum displayTypeEnum;
                                    String type = StringArgumentType.getString(context, "indicator_type");
                                    if ("heart".equals(type)) {
                                        displayTypeEnum = HealthDisplayTypeEnum.HEARTS;
                                    } else if ("number".equals(type)) {
                                        displayTypeEnum = HealthDisplayTypeEnum.NUMBER;
                                    } else {
                                        ConfigUtils.sendMessage(Minecraft.getInstance().player, Component.literal("Unknown argument, please try again."));
                                        return 1;
                                    }
                                    ModConfig.HANDLER.instance().indicator_type = displayTypeEnum;
                                    ModConfig.HANDLER.save();
                                    ConfigUtils.sendMessage(Minecraft.getInstance().player, Component.literal("Set display type to " + ModConfig.HANDLER.instance().indicator_type));
                                    return 1;
                                })))

                .then(Commands.literal("monitor")
                        .then(Commands.argument("entity_name", StringArgumentType.string())
                                .suggests((context, builder) -> {
                            for(Entity entity : Minecraft.getInstance().level.entitiesForRendering()){
                                if(entity.hasCustomName()) builder.suggest(Objects.requireNonNull(entity.getCustomName()).getString());
                                if(entity instanceof Player) builder.suggest(Objects.requireNonNull(entity.getDisplayName()).getString());
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            assert Minecraft.getInstance().level != null;
                            if(Util.getEntityFromName(Minecraft.getInstance().level, StringArgumentType.getString(context, "entity_name")) != null) {
                                ConfigUtils.sendMessage(Minecraft.getInstance().player, (Component.literal("Now monitoring " + StringArgumentType.getString(context, "entity_name"))));
                                RenderTracker.setTrackedEntity((LivingEntity) Util.getEntityFromName(Minecraft.getInstance().level, StringArgumentType.getString(context, "entity_name")));
                            }
                            else ConfigUtils.sendMessage(Minecraft.getInstance().player, (Component.literal("There is no entity named " + StringArgumentType.getString(context, "entity_name") + " in the world. It may have died or gone out of render distance.")));
                            return 0;
                        })))

                .then(Commands.literal("stop-monitoring")
                        .executes(context -> {
                            RenderTracker.setTrackedEntity(null);
                            ConfigUtils.sendMessage(Minecraft.getInstance().player, (Component.literal("Stopped monitoring ")));
                            return 0;
                        }))
        );
    }

    public static void registerOpenConfigCommand(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("healthindicators")
                .executes(context -> {
                    HealthIndicatorsCommon.openConfig();
                    return 1;
                })
        );
    }
}