package ninjaphenix.container_library;

import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import ninjaphenix.container_library.api.client.NCL_ClientApi;
import ninjaphenix.container_library.api.inventory.AbstractHandler;
import ninjaphenix.container_library.client.gui.PageScreen;
import ninjaphenix.container_library.client.gui.ScrollScreen;
import ninjaphenix.container_library.client.gui.SingleScreen;
import ninjaphenix.container_library.inventory.ClientScreenHandlerFactory;
import ninjaphenix.container_library.wrappers.ConfigWrapper;
import ninjaphenix.container_library.wrappers.NetworkWrapper;
import ninjaphenix.container_library.wrappers.PlatformUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.FormattedMessage;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiFunction;

public final class CommonMain {
    public static final Logger LOGGER = LogManager.getLogger(Utils.MOD_ID);
    private static ScreenHandlerType<AbstractHandler> screenHandlerType;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void initialize(BiFunction<Identifier, ClientScreenHandlerFactory, ScreenHandlerType> handlerTypeFunction, Path configPath, Path oldConfigPath) {
        screenHandlerType = handlerTypeFunction.apply(Utils.HANDLER_TYPE_ID, AbstractHandler::createClientMenu);

        if (PlatformUtils.isClient()) {
            ConfigWrapper.getInstance().initialise(configPath, oldConfigPath);
            NCL_ClientApi.registerScreenButton(Utils.PAGE_SCREEN_TYPE,
                    Utils.id("textures/gui/page_button.png"),
                    Utils.translation("screen.expandedstorage.page_screen"));
            NCL_ClientApi.registerScreenButton(Utils.SCROLL_SCREEN_TYPE,
                    Utils.id("textures/gui/scroll_button.png"),
                    Utils.translation("screen.expandedstorage.scroll_screen"));
            NCL_ClientApi.registerScreenButton(Utils.SINGLE_SCREEN_TYPE,
                    Utils.id("textures/gui/single_button.png"),
                    Utils.translation("screen.expandedstorage.single_screen"),
                    (scaledWidth, scaledHeight) -> scaledWidth < 370 || scaledHeight < 386, // Smallest possible resolution a double netherite chest fits on.
                    List.of(
                            Utils.translation("screen.ninjaphenix_container_lib.off_screen_warning_1").formatted(Formatting.GRAY),
                            Utils.translation("screen.ninjaphenix_container_lib.off_screen_warning_2").formatted(Formatting.GRAY)
                    ));

            NCL_ClientApi.registerScreenType(Utils.PAGE_SCREEN_TYPE, PageScreen::new);
            NCL_ClientApi.registerScreenType(Utils.SCROLL_SCREEN_TYPE, ScrollScreen::new);
            NCL_ClientApi.registerScreenType(Utils.SINGLE_SCREEN_TYPE, SingleScreen::new);

            // todo: these settings leave no room for rei/jei should we take those into consideration for minimum screen width
            NCL_ClientApi.registerDefaultScreenSize(Utils.PAGE_SCREEN_TYPE, PageScreen::retrieveScreenSize);
            NCL_ClientApi.registerDefaultScreenSize(Utils.SCROLL_SCREEN_TYPE, ScrollScreen::retrieveScreenSize);
            NCL_ClientApi.registerDefaultScreenSize(Utils.SINGLE_SCREEN_TYPE, SingleScreen::retrieveScreenSize);

            NCL_ClientApi.setPrefersSingleScreen(Utils.PAGE_SCREEN_TYPE);
            NCL_ClientApi.setPrefersSingleScreen(Utils.SCROLL_SCREEN_TYPE);
        }
        NetworkWrapper.getInstance().initialise();
    }

    public static void warnThrowableMessage(String message, Throwable throwable, Object... values) {
        CommonMain.LOGGER.warn(new FormattedMessage(message, values, throwable));
    }

    public static ScreenHandlerType<AbstractHandler> getScreenHandlerType() {
        return screenHandlerType;
    }
}
