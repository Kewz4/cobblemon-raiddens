package com.necro.raid.dens.fabric.mixins.forcebeacon;

import com.necro.raid.dens.fabric.compat.forcebeacon.ForceBeaconCompat;
import name.forcebeaconload.WorldBeaconData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Mixin into force-beacon-load's WorldBeaconData to re-inject phantom beacon
 * entries after check() removes them (since check() only accepts real BeaconBlockEntity
 * blocks in the world, not our RaidCrystalBlockEntity).
 *
 * We put directly into the beacons map instead of calling add() to avoid
 * the add() -> sendToAllPlayer() -> check() infinite recursion.
 */
@Mixin(value = WorldBeaconData.class, remap = false)
public class WorldBeaconDataMixin {

    @Shadow
    private Map<BlockPos, BeaconBlockEntity> beacons;

    @Inject(method = "check", at = @At("RETURN"))
    private void raiddens$reAddPhantomBeacons(Level world, CallbackInfo ci) {
        if (world instanceof ServerLevel serverLevel) {
            ForceBeaconCompat.cleanupPhantoms(serverLevel);
            ForceBeaconCompat.reRegisterPhantomsDirect(serverLevel, this.beacons);
        }
    }
}
