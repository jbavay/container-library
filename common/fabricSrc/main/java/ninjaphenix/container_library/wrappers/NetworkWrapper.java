package ninjaphenix.container_library.wrappers;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import ninjaphenix.container_library.Utils;
import ninjaphenix.container_library.api.OpenableBlockEntity;
import ninjaphenix.container_library.api.OpenableBlockEntityProvider;
import ninjaphenix.container_library.api.client.gui.AbstractScreen;
import ninjaphenix.container_library.api.inventory.AbstractHandler;
import ninjaphenix.container_library.api.v2.OpenableBlockEntityV2;
import ninjaphenix.container_library.client.gui.PickScreen;
import ninjaphenix.container_library.inventory.ServerScreenHandlerFactory;

import java.util.function.Consumer;

public abstract class NetworkWrapper {
    private static NetworkWrapper INSTANCE;
    private static Client CLIENT_INSTANCE;

    public abstract void initialise();

    protected abstract void openScreenHandler(ServerPlayerEntity player, Inventory inventory, ServerScreenHandlerFactory factory, Text title);

    protected abstract boolean checkUsagePermission(ServerPlayerEntity player, BlockPos pos);

    public static NetworkWrapper getInstance() {
        if (NetworkWrapper.INSTANCE == null) {
            NetworkWrapper.INSTANCE = new NetworkWrapperImpl();
        }
        return NetworkWrapper.INSTANCE;
    }

    private static Client getClientClassInternal() {
        if (NetworkWrapper.CLIENT_INSTANCE == null) {
            NetworkWrapper.CLIENT_INSTANCE = new NetworkWrapperImpl.Client();
        }
        return NetworkWrapper.CLIENT_INSTANCE;
    }

    public final void c_openInventoryAt(BlockPos pos) {
        NetworkWrapper.getClientClassInternal().openInventoryAt(pos);
    }

    protected final void openScreenHandlerIfAllowed(BlockPos pos, ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof OpenableBlockEntityProvider block) {
            OpenableBlockEntity inventory = block.getOpenableBlockEntity(world, state, pos);
            if (inventory != null) {
                Text title = inventory.getInventoryTitle();
                if (inventory.canBeUsedBy(player)) {
                    if (this.checkUsagePermission(player, pos)) {
                        block.onInitialOpen(player);
                    } else {
                        return;
                    }
                } else {
                    player.sendMessage(new TranslatableText("container.isLocked", title), true);
                    player.playSound(SoundEvents.BLOCK_CHEST_LOCKED, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    return;
                }
                if (!inventory.canContinueUse(player)) {
                    return;
                }
                this.openScreenHandler(player, inventory.getInventory(), AbstractHandler::new, title);
            }
        }
    }

    public final NetworkWrapperImpl toInternal() {
        return (NetworkWrapperImpl) this;
    }

    public final void s_openInventory(ServerPlayerEntity player, OpenableBlockEntityV2 inventory, Consumer<ServerPlayerEntity> onInitialOpen) {
        Text title = inventory.getInventoryTitle();
        if (!inventory.canBeUsedBy(player)) {
            player.sendMessage(new TranslatableText("container.isLocked", title), true);
            player.playSound(SoundEvents.BLOCK_CHEST_LOCKED, SoundCategory.BLOCKS, 1.0F, 1.0F);
            return;
        }
        onInitialOpen.accept(player);
        this.openScreenHandler(player, inventory.getInventory(), AbstractHandler::new, title);
    }

    protected static abstract class Client {
        abstract void sendOpenInventoryPacket(BlockPos pos);
        abstract boolean canSendOpenInventoryPacket();

        public final void openInventoryAt(BlockPos pos) {
            Identifier preference = ConfigWrapper.getInstance().getPreferredScreenType();
            if (preference.equals(Utils.UNSET_SCREEN_TYPE) || !AbstractScreen.isScreenTypeDeclared(preference)) {
                MinecraftClient.getInstance().setScreen(new PickScreen(() -> this.openInventoryAt(pos)));
            } else {
                if (this.canSendOpenInventoryPacket()) {
                    PlayerEntity player = MinecraftClient.getInstance().player;
                    World world = player.getEntityWorld();
                    BlockState state = world.getBlockState(pos);
                    if (state.getBlock() instanceof OpenableBlockEntityProvider provider) {
                        int invSize = provider.getOpenableBlockEntity(world, state, pos).getInventory().size();
                        if (AbstractScreen.getScreenSize(preference, invSize, MinecraftClient.getInstance().getWindow().getScaledWidth(), MinecraftClient.getInstance().getWindow().getScaledHeight()) != null) {
                            this.sendOpenInventoryPacket(pos);
                        } else {
                            player.sendMessage(Utils.translation("generic.ninjaphenix_container_lib.label").formatted(Formatting.GOLD).append(Utils.translation("chat.ninjaphenix_container_lib.cannot_display_screen", Utils.translation("screen." + preference.getNamespace() + "." + preference.getPath() + "_screen")).formatted(Formatting.WHITE)), false);
                        }
                    }
                }
            }
        }
    }
}
