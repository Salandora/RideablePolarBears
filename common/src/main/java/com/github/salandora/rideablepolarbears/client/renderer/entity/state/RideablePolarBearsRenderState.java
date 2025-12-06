package com.github.salandora.rideablepolarbears.client.renderer.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface RideablePolarBearsRenderState {
	default boolean rideablePolarBears$isSaddled() {
		throw new AssertionError("This shouldn't happen!");
	}
	default void rideablePolarBears$setSaddled(boolean value){
		throw new AssertionError("This shouldn't happen!");
	}

	default boolean rideablePolarBears$isInSittingPose(){
		throw new AssertionError("This shouldn't happen!");
	}
	default void rideablePolarBears$setInSittingPose(boolean value){
		throw new AssertionError("This shouldn't happen!");
	}
}
