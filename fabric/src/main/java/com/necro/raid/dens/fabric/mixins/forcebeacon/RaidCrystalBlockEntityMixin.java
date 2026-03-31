package com.necro.raid.dens.fabric.mixins.forcebeacon;

import com.necro.raid.dens.common.blocks.block.RaidCrystalBlock;
import com.necro.raid.dens.common.blocks.entity.RaidCrystalBlockEntity;
import com.necro.raid.dens.common.data.raid.RaidTier;
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
 * Mixin into RaidCrystalBlockEntity to register active crystals
 * as phantom beacons with force-beacon-load.
 */
@Mixin(RaidCrystalBlockEntity.class)
public class RaidCrystalBlockEntityMixin {

    @Inject(method = "tick", at = @At("RETURN"))
    private void raiddens$registerWithForceBeacon(Level level, BlockPos blockPos, BlockState blockState, CallbackInfo ci) {
        if (level.isClientSide()) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!blockState.getValue(RaidCrystalBlock.ACTIVE)) return;

        RaidTier tier = blockState.getValue(RaidCrystalBlock.RAID_TIER);
        ForceBeaconCompat.registerCrystal(serverLevel, blockPos, tier);
    }
}
