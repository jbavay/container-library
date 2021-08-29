package ninjaphenix.container_library;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.fabricmc.loader.api.FabricLoader;
import ninjaphenix.container_library.client.gui.PageScreen;
import ninjaphenix.container_library.client.gui.ScrollScreen;
import ninjaphenix.container_library.client.gui.SingleScreen;
import ninjaphenix.container_library.client.AmecsKeyHandler;
import ninjaphenix.container_library.client.FabricKeyHandler;
import ninjaphenix.container_library.wrappers.PlatformUtils;

public class Main implements ModInitializer {

    @Override
    public void onInitialize() {
        new PlatformUtils(FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT
                ? FabricLoader.getInstance().isModLoaded("amecs") ? new AmecsKeyHandler() : new FabricKeyHandler()
                : null, FabricLoader.getInstance()::isModLoaded);

        CommonMain.initialize((menuType, factory) -> ScreenHandlerRegistry.registerExtended(menuType, factory::create));

        if (PlatformUtils.getInstance().isClient()) {
            ScreenRegistry.register(CommonMain.getScrollMenuType(), ScrollScreen::new);
            ScreenRegistry.register(CommonMain.getPageMenuType(), PageScreen::new);
            ScreenRegistry.register(CommonMain.getSingleMenuType(), SingleScreen::new);
        }
    }
}
