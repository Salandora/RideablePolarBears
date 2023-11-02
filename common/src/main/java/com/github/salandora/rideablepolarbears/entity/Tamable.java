package com.github.salandora.rideablepolarbears.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;

public interface Tamable extends OwnableEntity {
	boolean rideablePolarBears$isTame();

	boolean rideablePolarBears$wantsToAttack(LivingEntity livingEntity, LivingEntity livingEntity2);

	boolean rideablePolarBears$isOrderedToSit();

	void rideablePolarBears$setOrderedToSit(boolean bl);

	boolean rideablePolarBears$isInSittingPose();

	void rideablePolarBears$setInSittingPose(boolean bl);
}
