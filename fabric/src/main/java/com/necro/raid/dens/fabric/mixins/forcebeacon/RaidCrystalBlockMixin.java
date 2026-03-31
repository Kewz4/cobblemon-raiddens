package com.necro.raid.dens.fabric.mixins.forcebeacon;

import com.necro.raid.dens.common.blocks.block.RaidCrystalBlock;
import com.necro.raid.dens.fabric.compat.forcebeacon.ForceBeaconCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into RaidCrystalBlock to unregister crystals from
 * force-beacon-load when they are removed.
 */
@Mixin(RaidCrystalBlock.class)
public class RaidCrystalBlockMixin {

    @Inject(method = "onRemove", at = @At("HEAD"))
    private void raiddens$unregisterFromForceBeacon(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState2, boolean bl, CallbackInfo ci) {
        if (level.isClientSide()) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        ForceBeaconCompat.unregisterCrystal(serverLevel, blockPos);
    }
}
