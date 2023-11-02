package com.github.salandora.rideablepolarbears.entity.ai.goal;

import com.github.salandora.rideablepolarbears.entity.Tamable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.PolarBear;

import java.util.EnumSet;

public class PolarBearOwnerHurtByTargetGoal extends TargetGoal {
	private final Tamable tameAnimal;
	private LivingEntity ownerLastHurtBy;
	private int timestamp;

	public PolarBearOwnerHurtByTargetGoal(PolarBear tamableAnimal) {
		super(tamableAnimal, false);
		this.tameAnimal = (Tamable) tamableAnimal;
		this.setFlags(EnumSet.of(Goal.Flag.TARGET));
	}

	@Override
	public boolean canUse() {
		if (this.tameAnimal.rideablePolarBears$isTame() && !this.tameAnimal.rideablePolarBears$isOrderedToSit()) {
			LivingEntity livingEntity = this.tameAnimal.getOwner();
			if (livingEntity == null) {
				return false;
			} else {
				this.ownerLastHurtBy = livingEntity.getLastHurtByMob();
				int i = livingEntity.getLastHurtByMobTimestamp();
				return i != this.timestamp
						&& this.canAttack(this.ownerLastHurtBy, TargetingConditions.DEFAULT)
						&& this.tameAnimal.rideablePolarBears$wantsToAttack(this.ownerLastHurtBy, livingEntity);
			}
		} else {
			return false;
		}
	}

	@Override
	public void start() {
		this.mob.setTarget(this.ownerLastHurtBy);
		LivingEntity livingEntity = this.tameAnimal.getOwner();
		if (livingEntity != null) {
			this.timestamp = livingEntity.getLastHurtByMobTimestamp();
		}

		super.start();
	}
}
