package com.github.salandora.rideablepolarbears.mixins.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.model.PolarBearModel;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.PolarBearRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PolarBearModel.class)
public class PolarBearModelMixin extends QuadrupedModel<PolarBearRenderState> {
	@Unique
	private ModelPart rideablePolarBears$saddle;

	protected PolarBearModelMixin(ModelPart modelPart) {
		super(modelPart);
	}

	@Inject(
			method = "<init>",
			at = @At("TAIL")
	)
	public void setRideablePolarBears$constructor(ModelPart modelPart, CallbackInfo ci) {
		 this.rideablePolarBears$saddle = this.body.getChild("saddle");
	}

	@WrapOperation(
			method = "createBodyLayer",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/model/geom/builders/LayerDefinition;create(Lnet/minecraft/client/model/geom/builders/MeshDefinition;II)Lnet/minecraft/client/model/geom/builders/LayerDefinition;"
			)
	)
	private static LayerDefinition setRideablePolarBears$injectParts(MeshDefinition arg, int i, int j, Operation<LayerDefinition> original, @Local PartDefinition partDefinition) {
		PartDefinition body = partDefinition.getChild("body");
		body.addOrReplaceChild(
				"saddle",
				CubeListBuilder.create()
						.texOffs(83, 14).addBox(-4.0f, -3.1F, -17.0F, 12.0F, 9.0F, 4.0F, new CubeDeformation(0.1F))
						.texOffs(83,  0).addBox(-5.0F, -4.1F, -13.0F, 14.0F, 9.0F, 5.0F, new CubeDeformation(0.1F)),
				PartPose.rotation((float)(-Math.PI / 2), 0, 0)
		);
		return original.call(arg, i, j);
	}

	@Inject(
			method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/PolarBearRenderState;)V",
			at = @At("HEAD"),
			cancellable = true
	)
	public void setupAnim(PolarBearRenderState polarBearRenderState, CallbackInfo ci) {
		this.rideablePolarBears$saddle.visible = !polarBearRenderState.rideablePolarBears$getSaddle().isEmpty();

		if (polarBearRenderState.rideablePolarBears$isInSittingPose()) {
			this.body.xRot = (float) (Math.PI * 1.8 / 5.0F);
			this.body.y = 17.0F;

			this.rightHindLeg.y = 18.0F;
			this.rightHindLeg.z = 7.0F;
			this.rightHindLeg.xRot = (float) (Math.PI * 3.0 / 2.0);

			this.leftHindLeg.y = this.rightHindLeg.y;
			this.leftHindLeg.z = this.rightHindLeg.z;
			this.leftHindLeg.xRot = (float) (Math.PI * 3.0 / 2.0);

			this.rightFrontLeg.y = 14.0F;
			this.rightFrontLeg.z = -8.0F;
			this.rightFrontLeg.xRot = (float) (Math.PI * 1.9);

			this.leftFrontLeg.y = this.rightFrontLeg.y;
			this.leftFrontLeg.z = this.rightFrontLeg.z;
			this.leftFrontLeg.xRot = (float) (Math.PI * 1.9);

			if (polarBearRenderState.isBaby) {
				this.head.y = 8.0F;
				this.head.z = -14.0F;
			} else {
				this.head.y = 7.0F;
				this.head.z = -14.0F;
			}

			ci.cancel();
		} else {
			this.rightHindLeg.setPos(-4.5F, 14.0F, 6.0F);
			this.rightHindLeg.xRot = (float) (Math.PI * 3.0 / 2.0);

			this.leftHindLeg.setPos(4.5F, 14.0F, 6.0F);
			this.leftHindLeg.xRot = (float) (Math.PI * 3.0 / 2.0);
		}
	}
}
