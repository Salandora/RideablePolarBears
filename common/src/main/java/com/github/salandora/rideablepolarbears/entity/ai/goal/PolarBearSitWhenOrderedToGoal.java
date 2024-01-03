package com.github.salandora.rideablepolarbears.entity.ai.goal;

import com.github.salandora.rideablepolarbears.entity.Tamable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.PolarBear;

import java.util.EnumSet;

public class PolarBearSitWhenOrderedToGoal extends Goal {
	private final PolarBear bear;
	private final Tamable mob;

	public PolarBearSitWhenOrderedToGoal(PolarBear tamableAnimal) {
		this.bear = tamableAnimal;
		this.mob = (Tamable)tamableAnimal;
		this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
	}

	@Override
	public boolean canContinueToUse() {
		return this.mob.rideablePolarBears$isOrderedToSit();
	}

	@Override
	public boolean canUse() {
		if (!this.mob.rideablePolarBears$isTame()) {
			return false;
		} else if (this.bear.isInWaterOrBubble()) {
			return false;
		} else if (!this.bear.isOnGround()) {
			return false;
		} else {
			Entity entity = this.mob.getOwner();
			if (entity instanceof LivingEntity livingEntity) {
				return (this.bear.distanceToSqr(livingEntity) >= 144.0 || livingEntity.getLastHurtByMob() == null) && this.mob.rideablePolarBears$isOrderedToSit();
			}
		}
		return false;
	}

	@Override
	public void start() {
		this.bear.getNavigation().stop();
		this.mob.rideablePolarBears$setInSittingPose(true);
	}

	@Override
	public void stop() {
		this.mob.rideablePolarBears$setInSittingPose(false);
	}
}

