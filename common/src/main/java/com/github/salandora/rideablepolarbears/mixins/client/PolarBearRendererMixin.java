package com.github.salandora.rideablepolarbears.mixins.client;

import com.github.salandora.rideablepolarbears.entity.Tamable;
import net.minecraft.client.model.PolarBearModel;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PolarBearRenderer;
import net.minecraft.client.renderer.entity.state.PolarBearRenderState;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.animal.PolarBear;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PolarBearRenderer.class)
public abstract class PolarBearRendererMixin extends AgeableMobRenderer<PolarBear, PolarBearRenderState, PolarBearModel> {
	public PolarBearRendererMixin(EntityRendererProvider.Context context, PolarBearModel entityModel, PolarBearModel entityModel2, float f) {
		super(context, entityModel, entityModel2, f);
	}

	@Inject(
			method = "extractRenderState(Lnet/minecraft/world/entity/animal/PolarBear;Lnet/minecraft/client/renderer/entity/state/PolarBearRenderState;F)V",
			at = @At(value = "RETURN")
	)
	public void rideablePolarBears$extractRenderState(PolarBear polarBear, PolarBearRenderState polarBearRenderState, float f, CallbackInfo ci) {
		polarBearRenderState.rideablePolarBears$setSaddled(((Saddleable)polarBear).isSaddled());
		polarBearRenderState.rideablePolarBears$setInSittingPose(((Tamable)polarBear).rideablePolarBears$isInSittingPose());
	}
}
