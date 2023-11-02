package com.github.salandora.rideablepolarbears.mixins;

import com.github.salandora.rideablepolarbears.entity.ai.goal.IPolarBearAttackPlayersGoal;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = { "net.minecraft.world.entity.animal.PolarBear$PolarBearAttackPlayersGoal" })
public class PolarBearAttackPlayersGoalMixin implements IPolarBearAttackPlayersGoal {

}
