package com.github.salandora.rideablepolarbears.mixins.client;

import com.github.salandora.rideablepolarbears.client.renderer.entity.state.RideablePolarBearsRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.PolarBearRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PolarBearRenderState.class)
public class PolarBearRenderStateMixin extends LivingEntityRenderState implements RideablePolarBearsRenderState {
	@Unique
	private ItemStack rideablePolarBears$isSaddle = ItemStack.EMPTY;
	@Unique
	private boolean rideablePolarBears$isInSittingPose;


	@Override
	public ItemStack rideablePolarBears$getSaddle() {
		return rideablePolarBears$isSaddle;
	}

	@Override
	public void rideablePolarBears$setSaddle(ItemStack value) {
		rideablePolarBears$isSaddle = value;
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
