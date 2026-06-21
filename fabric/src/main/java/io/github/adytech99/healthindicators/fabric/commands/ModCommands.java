package io.github.adytech99.healthindicators.fabric.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.adytech99.healthindicators.HealthIndicatorsCommon;
import io.github.adytech99.healthindicators.RenderTracker;
import io.github.adytech99.healthindicators.config.ModConfig;
import io.github.adytech99.healthindicators.enums.HealthDisplayTypeEnum;
import io.github.adytech99.healthindicators.util.ConfigUtils;
import io.github.adytech99.healthindicators.util.Util;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;

public class ModCommands {
    @Environment(EnvType.CLIENT)
    public static void registerCommands(){
        ClientCommandRegistrationCallback.EVENT.register(ModCommands::configCommands);
        ClientCommandRegistrationCallback.EVENT.register(ModCommands::openModMenuCommand);
    }


    private static void configCommands(CommandDispatcher<FabricClientCommandSource> fabricClientCommandSourceCommandDispatcher, CommandBuildContext commandRegistryAccess) {
        fabricClientCommandSourceCommandDispatcher.register(ClientCommands.literal("healthindicators")
            .then(ClientCommands.literal("offset")
                .then(ClientCommands.argument("offset", DoubleArgumentType.doubleArg())
                    .executes(context -> {
                        ModConfig.HANDLER.instance().display_offset = Math.clamp(
                                DoubleArgumentType.getDouble(context, "offset"),
                                -5.0, 5.0);
                        ModConfig.HANDLER.save();
                        ConfigUtils.sendMessage(context.getSource().getPlayer(), Component.literal("Set heart offset to " + Util.truncate(ModConfig.getDisplayOffset(),2)));
                        return 1;
                    })))

            .then(ClientCommands.literal("indicator-type")
                .then(ClientCommands.argument("indicator_type", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        builder.suggest("heart");
                        builder.suggest("number");
                        return builder.buildFuture();
                    })
                        .executes(context -> {
                            HealthDisplayTypeEnum displayTypeEnum;
                            if (StringArgumentType.getString(context, "indicator_type").equals("heart")) {
                                displayTypeEnum = HealthDisplayTypeEnum.HEARTS;
                            } else if (StringArgumentType.getString(context, "indicator_type").equals("number")) {
                                displayTypeEnum = HealthDisplayTypeEnum.NUMBER;
                            } else {
                                ConfigUtils.sendMessage(context.getSource().getPlayer(), Component.literal("Unknown argument, please try again."));
                                return 1;
                            }

                            ModConfig.HANDLER.instance().indicator_type = displayTypeEnum;
                            ModConfig.HANDLER.save();
                            ConfigUtils.sendMessage(context.getSource().getPlayer(), Component.literal("Set display type to " + ModConfig.HANDLER.instance().indicator_type));
                            return 1;
                        })))


            .then(ClientCommands.literal("monitor")
                .then(ClientCommands.argument("entity_name", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            for(Entity entity : context.getSource().getLevel().entitiesForRendering()){
                                if(entity.hasCustomName()) builder.suggest(Objects.requireNonNull(entity.getCustomName()).getString());
                                if(entity instanceof Player) builder.suggest(Objects.requireNonNull(entity.getDisplayName()).getString());
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            if(Util.getEntityFromName(context.getSource().getLevel(), StringArgumentType.getString(context, "entity_name")) != null) {
                                ConfigUtils.sendMessage(context.getSource().getPlayer(), (Component.literal("Now monitoring " + StringArgumentType.getString(context, "entity_name"))));
                                RenderTracker.setTrackedEntity((LivingEntity) Util.getEntityFromName(context.getSource().getLevel(), StringArgumentType.getString(context, "entity_name")));
                            }
                            else ConfigUtils.sendMessage(context.getSource().getPlayer(), (Component.literal("There is no entity named " + StringArgumentType.getString(context, "entity_name") + " in the world. It may have died or gone out of render distance.")));
                            return 0;
                        })))

            .then(ClientCommands.literal("stop-monitoring")
                .executes(context -> {
                    RenderTracker.setTrackedEntity(null);
                    ConfigUtils.sendMessage(context.getSource().getPlayer(), (Component.literal("Stopped monitoring ")));
                    return 0;
                }))
        );
    }

    private static void IndicatorTypeCommand(CommandDispatcher<FabricClientCommandSource> fabricClientCommandSourceCommandDispatcher, CommandBuildContext commandRegistryAccess) {
        fabricClientCommandSourceCommandDispatcher.register(ClientCommands.literal("healthindicators")
                .then(ClientCommands.argument("operation", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            builder.suggest("indicator_type");
                            return builder.buildFuture();
                        })
                            .then(ClientCommands.argument("indicator_type", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    builder.suggest("heart");
                                    builder.suggest("number");
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    HealthDisplayTypeEnum displayTypeEnum;
                                    if(StringArgumentType.getString(context, "indicator_type").equals("heart")){
                                        displayTypeEnum = HealthDisplayTypeEnum.HEARTS;
                                    } else if (StringArgumentType.getString(context, "indicator_type").equals("number")) {
                                        displayTypeEnum = HealthDisplayTypeEnum.NUMBER;
                                    }
                                    else {
                                        ConfigUtils.sendMessage(context.getSource().getPlayer(), Component.literal("Unknown argument, please try again."));
                                        return 1;
                                    }

                                    ModConfig.HANDLER.instance().indicator_type = displayTypeEnum;
                                    ModConfig.HANDLER.save();
                                    ConfigUtils.sendMessage(context.getSource().getPlayer(), Component.literal("Set display type to " + ModConfig.HANDLER.instance().indicator_type));
                                    return 1;
                                }))));
    }

    private static void openModMenuCommand(CommandDispatcher<FabricClientCommandSource> fabricClientCommandSourceCommandDispatcher, CommandBuildContext commandRegistryAccess) {
        fabricClientCommandSourceCommandDispatcher.register(ClientCommands.literal("healthindicators")
            .executes(context -> {
                HealthIndicatorsCommon.openConfig();
                return 1;
        }));
    }
}
