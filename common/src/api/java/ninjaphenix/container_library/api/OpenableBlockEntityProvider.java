package ninjaphenix.container_library.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Should be implemented on blocks.
 */
public interface OpenableBlockEntityProvider {
    /**
     * Return the openable block entity, {@link ninjaphenix.container_library.api.helpers.OpenableBlockEntities} can be used to supply more than one inventory.
     */
    default OpenableBlockEntity getOpenableBlockEntity(Level world, BlockState state, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof OpenableBlockEntity entity) {
            return entity;
        }
        return null;
    }

    /**
     * Call back for running code when an inventory is initially opened, can be used to award opening stats.
     */
    default void onInitialOpen(ServerPlayer player) {

    }
}
