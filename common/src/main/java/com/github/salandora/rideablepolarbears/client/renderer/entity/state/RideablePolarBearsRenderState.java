package com.github.salandora.rideablepolarbears.client.renderer.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public interface RideablePolarBearsRenderState {
	default ItemStack rideablePolarBears$getSaddle() {
		throw new AssertionError("This shouldn't happen!");
	}
	default void rideablePolarBears$setSaddle(ItemStack value){
		throw new AssertionError("This shouldn't happen!");
	}

	default boolean rideablePolarBears$isInSittingPose(){
		throw new AssertionError("This shouldn't happen!");
	}
	default void rideablePolarBears$setInSittingPose(boolean value){
		throw new AssertionError("This shouldn't happen!");
	}
}
