package ninjaphenix.container_library.api;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public interface OpenableBlockEntity {
    default boolean canBeUsedBy(ServerPlayer player) {
        return ((BaseContainerBlockEntity) this).canOpen(player);
    }

    default boolean canContinueUse(ServerPlayer player) {
        BlockEntity self = (BlockEntity) this;
        return self.getLevel().getBlockEntity(self.getBlockPos()) == self && player.distanceToSqr(Vec3.atCenterOf(self.getBlockPos())) <= 64;
    }

    default Container getInventory() {
        return (Container) this;
    }

    default Component getInventoryName() {
        return ((Nameable) this).getDisplayName();
    }
}
