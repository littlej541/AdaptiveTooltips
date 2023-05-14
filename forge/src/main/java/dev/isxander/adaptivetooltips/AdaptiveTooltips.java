package dev.isxander.adaptivetooltips;

import dev.isxander.adaptivetooltips.config.AdaptiveTooltipConfig;
import dev.isxander.adaptivetooltips.utils.Constants;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Constants.MOD_ID)
public class AdaptiveTooltips {
    public AdaptiveTooltips() {
        ModLoadingContext modLoadingContext = ModLoadingContext.get();
        IEventBus fmlEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modLoadingContext.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parentScreen) -> AdaptiveTooltipConfig.makeScreen(parentScreen)));

        fmlEventBus.addListener(this::clientSetup);
    }

    public void clientSetup(FMLClientSetupEvent event) {
        AdaptiveTooltipConfig.INSTANCE.load();
    }
}
