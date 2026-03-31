package com.necro.raid.dens.fabric.mixins.forcebeacon;

import com.necro.raid.dens.fabric.compat.forcebeacon.ForceBeaconCompat;
import name.forcebeaconload.WorldBeaconData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into force-beacon-load's WorldBeaconData to prevent removal of
 * phantom beacon entries that represent active raid crystals.
 *
 * After check() removes entries where the block entity isn't a BeaconBlockEntity,
 * we re-add phantoms for positions that have a RaidCrystalBlockEntity.
 */
@Mixin(value = WorldBeaconData.class, remap = false)
public class WorldBeaconDataMixin {

    @Inject(method = "check", at = @At("RETURN"))
    private void raiddens$reAddPhantomBeacons(Level world, CallbackInfo ci) {
        if (world instanceof ServerLevel serverLevel) {
            ForceBeaconCompat.cleanupPhantoms(serverLevel);
            ForceBeaconCompat.reRegisterPhantoms(serverLevel);
        }
    }
}
