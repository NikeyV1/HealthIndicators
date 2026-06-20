package io.github.adytech99.healthindicators.neoforge;

import io.github.adytech99.healthindicators.config.Config;
import io.github.adytech99.healthindicators.config.ModConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import io.github.adytech99.healthindicators.HealthIndicatorsCommon;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.Lazy;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import static io.github.adytech99.healthindicators.HealthIndicatorsCommon.HEALTH_INDICATORS_CATEGORY;

// TODO[26.2-verify]: NeoForge 26.2 APIs (mod ctor signature, RegisterKeyMappingsEvent,
// ClientTickEvent.Post, IConfigScreenFactory, ModLoadingContext) may have changed — confirm.
@Mod(HealthIndicatorsCommon.MOD_ID)
@EventBusSubscriber(value = Dist.CLIENT, modid = HealthIndicatorsCommon.MOD_ID)
public final class HealthIndicatorsNeoForge {

    public static Minecraft client = Minecraft.getInstance();

    public static final Lazy<KeyMapping> HEARTS_RENDERING_ENABLED = Lazy.of(() -> new KeyMapping(
            "key." + HealthIndicatorsCommon.MOD_ID + ".renderingEnabled",
            GLFW.GLFW_KEY_LEFT,
            HEALTH_INDICATORS_CATEGORY
    ));

    public static final Lazy<KeyMapping> ARMOR_RENDERING_ENABLED = Lazy.of(() -> new KeyMapping(
            "key." + HealthIndicatorsCommon.MOD_ID + ".armorRenderingEnabled",
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            HEALTH_INDICATORS_CATEGORY
    ));

    public static final Lazy<KeyMapping> OVERRIDE_ALL_FILTERS = Lazy.of(() -> new KeyMapping(
            "key." + HealthIndicatorsCommon.MOD_ID + ".overrideAllFilters",
            GLFW.GLFW_KEY_RIGHT,
            HEALTH_INDICATORS_CATEGORY
    ));
    public static final Lazy<KeyMapping> INCREASE_HEART_OFFSET = Lazy.of(() -> new KeyMapping(
            "key." + HealthIndicatorsCommon.MOD_ID + ".increaseHeartOffset",
            GLFW.GLFW_KEY_UP,
            HEALTH_INDICATORS_CATEGORY
    ));

    public static final Lazy<KeyMapping> DECREASE_HEART_OFFSET = Lazy.of(() -> new KeyMapping(
            "key." + HealthIndicatorsCommon.MOD_ID + ".decreaseHeartOffset",
            GLFW.GLFW_KEY_DOWN,
            HEALTH_INDICATORS_CATEGORY
    ));

    public static final Lazy<KeyMapping> OPEN_CONFIG_SCREEN = Lazy.of(() -> new KeyMapping(
            "key." + HealthIndicatorsCommon.MOD_ID + ".openModMenuConfig",
            GLFW.GLFW_KEY_I,
            HEALTH_INDICATORS_CATEGORY
    ));



    public HealthIndicatorsNeoForge() {
        HealthIndicatorsCommon.init();
        HealthIndicatorsCommon.client = client;
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
    }

    @SubscribeEvent
    public static void registerBindings(RegisterKeyMappingsEvent event){
        event.register(HEARTS_RENDERING_ENABLED.get());
        event.register(INCREASE_HEART_OFFSET.get());
        event.register(DECREASE_HEART_OFFSET.get());
        event.register(OVERRIDE_ALL_FILTERS.get());
        event.register(ARMOR_RENDERING_ENABLED.get());
        event.register(OPEN_CONFIG_SCREEN.get());
    }


    public void onClientTick(ClientTickEvent.Post event){
        HealthIndicatorsCommon.tick();

        while (HEARTS_RENDERING_ENABLED.get().consumeClick()) {
            HealthIndicatorsCommon.enableHeartsRendering();
        }

        while (ARMOR_RENDERING_ENABLED.get().consumeClick()) {
            HealthIndicatorsCommon.enableArmorRendering();
        }

        while (INCREASE_HEART_OFFSET.get().consumeClick()) {
            HealthIndicatorsCommon.increaseOffset();
        }

        while (DECREASE_HEART_OFFSET.get().consumeClick()) {
            HealthIndicatorsCommon.decreaseOffset();
        }
        if (OVERRIDE_ALL_FILTERS.get().isDown()) {
            HealthIndicatorsCommon.overrideFilters();
        }
        else if(Config.getOverrideAllFiltersEnabled()) {
            HealthIndicatorsCommon.disableOverrideFilters();
        }

        if(OPEN_CONFIG_SCREEN.get().consumeClick()) HealthIndicatorsCommon.openConfigScreen();
    }

    @SubscribeEvent
    public static void constructMod(FMLConstructModEvent event){
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class, () -> new IConfigScreenFactory() {
            @Override
            public @NotNull Screen createScreen(@NotNull ModContainer arg, @NotNull Screen arg2) {
                return ModConfig.createScreen(arg2);
            }
        });
    }
}
