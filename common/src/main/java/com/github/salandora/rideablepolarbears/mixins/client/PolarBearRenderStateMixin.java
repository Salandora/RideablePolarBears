package com.github.salandora.rideablepolarbears.mixins.client;

import com.github.salandora.rideablepolarbears.client.renderer.entity.state.RideablePolarBearsRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.PolarBearRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PolarBearRenderState.class)
public class PolarBearRenderStateMixin extends LivingEntityRenderState implements RideablePolarBearsRenderState {
	@Unique
	private boolean rideablePolarBears$isSaddled;
	@Unique
	private boolean rideablePolarBears$isInSittingPose;


	@Override
	public boolean rideablePolarBears$isSaddled() {
		return rideablePolarBears$isSaddled;
	}

	@Override
	public void rideablePolarBears$setSaddled(boolean value) {
		rideablePolarBears$isSaddled = value;
	}

	@Override
	public boolean rideablePolarBears$isInSittingPose() {
		return rideablePolarBears$isInSittingPose;
	}

	@Override
	public void rideablePolarBears$setInSittingPose(boolean value) {
		rideablePolarBears$isInSittingPose = value;
	}
}
