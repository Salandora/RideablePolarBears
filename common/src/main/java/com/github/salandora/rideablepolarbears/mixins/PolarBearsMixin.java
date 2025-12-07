package com.github.salandora.rideablepolarbears.mixins;

import com.github.salandora.rideablepolarbears.attachment.Attachments;
import com.github.salandora.rideablepolarbears.attachment.EntityAttachment;
import com.github.salandora.rideablepolarbears.entity.Tamable;
import com.github.salandora.rideablepolarbears.entity.ai.goal.IPolarBearAttackPlayersGoal;
import com.github.salandora.rideablepolarbears.entity.ai.goal.PolarBearOwnerHurtByTargetGoal;
import com.github.salandora.rideablepolarbears.entity.ai.goal.PolarBearOwnerHurtTargetGoal;
import com.github.salandora.rideablepolarbears.entity.ai.goal.PolarBearSitWhenOrderedToGoal;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(PolarBear.class)
public abstract class PolarBearsMixin extends Animal implements NeutralMob, Tamable, OwnableEntity, PlayerRideableJumping {
	@Shadow
	public abstract boolean isStanding();

	@Shadow
	public abstract void setStanding(boolean bl);

	@Shadow
	private float clientSideStandAnimationO;

	@Unique
	private static final int rideablePolarBears$SITTING_FLAG = 2;
	@Unique
	private static final int rideablePolarBears$TAMED_FLAG = 4;

	@Unique
	protected float rideablePolarBears$playerJumpPendingScale;
	@Unique
	protected boolean rideablePolarBears$isJumping;
	@Unique
	protected boolean rideablePolarBears$allowStandSliding;
	@Unique
	private int rideablePolarBears$standCounter;
	@Unique
	private boolean rideablePolarBears$orderedToSit;

	protected PolarBearsMixin(EntityType<? extends Animal> entityType, Level level) {
		super(entityType, level);
	}

	@Unique
	protected boolean rideablePolarBears$getFlag(int bitmask) {
		return (EntityAttachment.INSTANCE.getData(this, Attachments.POLARBEAR_FLAGS) & bitmask) != 0;
	}
	@Unique
	protected void rideablePolarBears$setFlag(int bitmask, boolean flag) {
		byte b = EntityAttachment.INSTANCE.getData(this, Attachments.POLARBEAR_FLAGS);
		if (flag) {
			EntityAttachment.INSTANCE.setData(this, Attachments.POLARBEAR_FLAGS, (byte)(b | bitmask));
		} else {
			EntityAttachment.INSTANCE.setData(this, Attachments.POLARBEAR_FLAGS, (byte)(b & ~bitmask));
		}
	}

	@Override
	public boolean isFood(@NotNull ItemStack itemStack) {
		return itemStack.is(ItemTags.FISHES);
	}

	@Nullable
	public EntityReference<LivingEntity> getOwnerReference() {
		return EntityAttachment.INSTANCE.getData(this, Attachments.POLARBEAR_OWNER).orElse(null);
	}

	public void rideablePolarBears$setOwner(@Nullable LivingEntity livingEntity) {
		EntityAttachment.INSTANCE.setData(this, Attachments.POLARBEAR_OWNER, Optional.ofNullable(livingEntity).map(EntityReference::new));
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void rideablePolarBears$constructor(EntityType<? extends Animal> entityType, Level world, CallbackInfo ci) {
		this.rideablePolarBears$reassessTameGoals();
	}

	@SuppressWarnings("DataFlowIssue")
	@Inject(method = "registerGoals", at = @At("TAIL"))
	private void rideablePolarBears$initGoals(CallbackInfo ci) {
		this.goalSelector.addGoal(2, new PolarBearSitWhenOrderedToGoal((PolarBear) (Object) this));
		this.goalSelector.addGoal(3, new BreedGoal(this, 1.0, PolarBear.class));
		this.goalSelector.addGoal(3, new TemptGoal(this, 1.25, stack -> stack.is(ItemTags.FISHES), false));
		this.targetSelector.addGoal(1, new PolarBearOwnerHurtByTargetGoal((PolarBear) (Object) this));
		this.targetSelector.addGoal(2, new PolarBearOwnerHurtTargetGoal((PolarBear) (Object) this));
	}

	@Unique
	protected void rideablePolarBears$reassessTameGoals() {
		if (rideablePolarBears$isTamed()) {
			this.targetSelector.removeAllGoals(goal -> goal instanceof IPolarBearAttackPlayersGoal);
		}
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void rideablePolarBears$tick(CallbackInfo ci) {
		if (this.isEffectiveAi() && this.rideablePolarBears$standCounter > 0 && ++this.rideablePolarBears$standCounter > 20) {
			this.rideablePolarBears$standCounter = 0;
			this.setStanding(false);
		}

		if (!this.isStanding()) {
			this.rideablePolarBears$allowStandSliding = false;
		}
	}

	@Override
	@NotNull
	public InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
		if (this.isVehicle() || this.isBaby()) {
			return super.mobInteract(player, hand);
		}

		ItemStack itemStack = player.getItemInHand(hand);
		boolean foodItem = this.isFood(itemStack);
		if (this.rideablePolarBears$isTamed()) {
			if (foodItem && this.getHealth() < this.getMaxHealth()) {
				if (!player.getAbilities().instabuild) {
					itemStack.shrink(1);
				}

				//noinspection DataFlowIssue
				this.heal((float) itemStack.get(DataComponents.FOOD).nutrition());
				return InteractionResult.SUCCESS;
			} else if (!foodItem && this.isSaddled() && !this.isVehicle() && !this.isBaby() && this.rideablePolarBears$isOwnedBy(player) && !player.isSecondaryUseActive()) {
				this.rideablePolarBears$doPlayerRide(player);
				return InteractionResult.SUCCESS;
			} else {
				InteractionResult actionResult = super.mobInteract(player, hand);
				if (!actionResult.consumesAction()) {
					if (!this.isBaby() && this.isEquippableInSlot(itemStack, EquipmentSlot.SADDLE)) {
						return itemStack.interactLivingEntity(player, this, hand);
					}

					if (rideablePolarBears$isOwnedBy(player)) {
						this.rideablePolarBears$setOrderedToSit(!this.rideablePolarBears$isOrderedToSit());
						this.jumping = false;
						this.navigation.stop();
						this.setTarget(null);
						return InteractionResult.SUCCESS;
					}
				}

				return actionResult;
			}
		} else if (foodItem) {
			this.usePlayerItem(player, hand, itemStack);
			if (this.random.nextInt(3) == 0) {
				this.rideablePolarBears$tame(player);
				this.level().broadcastEntityEvent(this, (byte)7);
			} else {
				this.level().broadcastEntityEvent(this, (byte)6);
			}

			this.setPersistenceRequired();
			return InteractionResult.CONSUME;
		}

		return super.mobInteract(player, hand);
	}

	@Override
	public boolean canUseSlot(EquipmentSlot equipmentSlot) {
		return equipmentSlot != EquipmentSlot.SADDLE ? super.canUseSlot(equipmentSlot) : this.isAlive() && !this.isBaby() && this.rideablePolarBears$isTamed();
	}

	@Override
	protected boolean canDispenserEquipIntoSlot(EquipmentSlot equipmentSlot) {
		return (equipmentSlot == EquipmentSlot.SADDLE) && this.rideablePolarBears$isTamed() || super.canDispenserEquipIntoSlot(equipmentSlot);
	}

	@Unique
	protected void rideablePolarBears$doPlayerRide(Player player) {
		this.setStanding(false);
		this.rideablePolarBears$setOrderedToSit(false);
		if (!this.level().isClientSide) {
			player.setYRot(this.getYRot());
			player.setXRot(this.getXRot());
			player.startRiding(this);
		}
	}

	@Unique
	public boolean rideablePolarBears$isJumping() {
		return this.rideablePolarBears$isJumping;
	}
	@Unique
	public void rideablePolarBears$setIsJumping(boolean bl) {
		this.rideablePolarBears$isJumping = bl;
	}

	@Override
	protected void dropEquipment(ServerLevel serverLevel) {
		super.dropEquipment(serverLevel);
		if (!this.getItemBySlot(EquipmentSlot.SADDLE).isEmpty()) {
			this.spawnAtLocation(serverLevel, this.getItemBySlot(EquipmentSlot.SADDLE));
		}
	}

	@Nullable
	@Override
	public LivingEntity getControllingPassenger() {
		Entity var3 = this.getFirstPassenger();
		if (var3 instanceof Mob) {
			return (Mob)var3;
		} else {
			if (this.isSaddled()) {
				var3 = this.getFirstPassenger();
				if (var3 instanceof Player) {
					return (Player)var3;
				}
			}

			return null;
		}
	}

	@Unique
	protected Vec2 rideablePolarBears$getRiddenRotation(@NotNull LivingEntity livingEntity) {
		return new Vec2(livingEntity.getXRot() * 0.5F, livingEntity.getYRot());
	}

	@Override
	protected void tickRidden(@NotNull Player player, @NotNull Vec3 vec3) {
		super.tickRidden(player, vec3);
		Vec2 vec2 = this.rideablePolarBears$getRiddenRotation(player);
		this.setRot(vec2.y, vec2.x);
		this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
		if (this.isLocalInstanceAuthoritative()) {
			if (this.onGround()) {
				this.rideablePolarBears$setIsJumping(false);
				if (this.rideablePolarBears$playerJumpPendingScale > 0.0F && !this.rideablePolarBears$isJumping()) {
					this.rideablePolarBears$executeRidersJump(this.rideablePolarBears$playerJumpPendingScale, vec3);
				}

				this.rideablePolarBears$playerJumpPendingScale = 0.0F;
			}
		}
	}

	@Override
	@NotNull
	protected Vec3 getRiddenInput(@NotNull Player player, @NotNull Vec3 vec3) {
		if (this.onGround() && this.rideablePolarBears$playerJumpPendingScale == 0.0F && this.isStanding() && !this.rideablePolarBears$allowStandSliding) {
			return Vec3.ZERO;
		} else {
			float f = player.xxa * 0.5F;
			float f1 = player.zza;
			if (f1 <= 0.0F) {
				f1 *= 0.25F;
			}

			return new Vec3(f, 0.0, f1);
		}
	}

	@Override
	protected float getRiddenSpeed(@NotNull Player player) {
		return (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED);
	}

	@Unique
	protected void rideablePolarBears$executeRidersJump(float g, @NotNull Vec3 arg) {
		double d0 = this.rideablePolarBears$getCustomJump() * (double)g * (double)this.getBlockJumpFactor();
		double d1 = d0 + (double)this.getJumpBoostPower();
		Vec3 vec3 = this.getDeltaMovement();
		this.setDeltaMovement(vec3.x, d1, vec3.z);
		this.rideablePolarBears$setIsJumping(true);
		this.hasImpulse = true;
		if (arg.z > 0.0) {
			float f = Mth.sin(this.getYRot() * (float) (Math.PI / 180.0));
			float f1 = Mth.cos(this.getYRot() * (float) (Math.PI / 180.0));
			this.setDeltaMovement(this.getDeltaMovement().add((-0.4F * f * g), 0.0, (0.4F * f1 * g)));
		}
	}

	@Unique
	public double rideablePolarBears$getCustomJump() {
		return 0.5f;
	}

	@Unique
	protected void rideablePolarBears$playJumpSound() {
		this.playSound(SoundEvents.POLAR_BEAR_STEP, 0.4F, 1.0F);
	}

	@Override
	@NotNull
	public Vec3 getDismountLocationForPassenger(@NotNull LivingEntity livingEntity) {
		Direction direction = this.getMotionDirection();
		if (direction.getAxis() == Direction.Axis.Y) {
			return super.getDismountLocationForPassenger(livingEntity);
		} else {
			int[][] is = DismountHelper.offsetsForDirection(direction);
			BlockPos blockPos = this.blockPosition();
			BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

			for (Pose pose : livingEntity.getDismountPoses()) {
				AABB aABB = livingEntity.getLocalBoundsForPose(pose);

				for (int[] js : is) {
					mutableBlockPos.set(blockPos.getX() + (js[0]*1.5), blockPos.getY(), blockPos.getZ() + (js[1]*1.5));
					double d = this.level().getBlockFloorHeight(mutableBlockPos);
					if (DismountHelper.isBlockFloorValid(d)) {
						Vec3 vec3 = Vec3.upFromBottomCenterOf(mutableBlockPos, d);
						if (DismountHelper.canDismountTo(this.level(), livingEntity, aABB.move(vec3))) {
							livingEntity.setPose(pose);
							return vec3;
						}
					}
				}
			}

			return super.getDismountLocationForPassenger(livingEntity);
		}
	}

	@Override
	public void onPlayerJump(int i) {
		if (this.isSaddled()) {
			if (i < 0) {
				i = 0;
			} else {
				this.rideablePolarBears$allowStandSliding = true;
				this.rideablePolarBears$standIfPossible();
			}

			if (i >= 90) {
				this.rideablePolarBears$playerJumpPendingScale = 1.0F;
			} else {
				this.rideablePolarBears$playerJumpPendingScale = 0.4F + 0.4F * (float)i / 90.0F;
			}
		}
	}

	@Override
	public boolean canJump() {
		return this.isSaddled();
	}

	@Override
	public void handleStartJump(int i) {
		this.rideablePolarBears$allowStandSliding = true;
		this.rideablePolarBears$standIfPossible();
		this.rideablePolarBears$playJumpSound();
	}

	@Unique
	public void rideablePolarBears$standIfPossible() {
		if (this.isEffectiveAi()) {
			this.rideablePolarBears$standCounter = 1;
			this.setStanding(true);
		}
	}

	@Override
	protected void positionRider(Entity entity, Entity.MoveFunction moveFunction) {
		super.positionRider(entity, moveFunction);
		if (entity instanceof LivingEntity) {
			((LivingEntity)entity).yBodyRot = this.yBodyRot;
		}

	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions entityDimensions, float f) {
		return super.getPassengerAttachmentPoint(entity, entityDimensions, f)
				.add(new Vec3(
						(double)0.0F,
						-(0.8F / 6.0F) * (double)this.clientSideStandAnimationO * (double)f,
						-(0.8F / 6.0F) * (double)this.clientSideStandAnimationO * (double)f)
					.yRot(-this.getYRot() * ((float)Math.PI / 180F))
				);
	}

	@Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
	private void rideablePolarBears$writeCustomDataToNbt(ValueOutput valueOutput, CallbackInfo ci) {
		EntityReference<LivingEntity> entityReference = this.getOwnerReference();
		EntityReference.store(entityReference, valueOutput, "Owner");
		valueOutput.putBoolean("Sitting", this.rideablePolarBears$orderedToSit);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
	private void rideablePolarBears$readCustomDataToNbt(ValueInput valueInput, CallbackInfo ci) {
		EntityReference<LivingEntity> entityReference = EntityReference.readWithOldOwnerConversion(valueInput, "Owner", this.level());
		if (entityReference != null) {
			EntityAttachment.INSTANCE.setData(this, Attachments.POLARBEAR_OWNER, Optional.of(entityReference));
			this.rideablePolarBears$setTame(true);
		} else {
			EntityAttachment.INSTANCE.setData(this, Attachments.POLARBEAR_OWNER, Optional.empty());
			this.rideablePolarBears$setTame(false);
		}
		this.rideablePolarBears$orderedToSit = valueInput.getBooleanOr("Sitting", false);
		this.rideablePolarBears$setInSittingPose(this.rideablePolarBears$orderedToSit);
	}

	@Override
	public boolean canBeLeashed() {
		return !this.isAngry() && super.canBeLeashed();
	}

	@Unique
	protected void rideablePolarBears$spawnTamingParticles(boolean bl) {
		ParticleOptions particleOptions = ParticleTypes.HEART;
		if (!bl) {
			particleOptions = ParticleTypes.SMOKE;
		}

		for(int i = 0; i < 7; ++i) {
			double d = this.random.nextGaussian() * 0.02;
			double e = this.random.nextGaussian() * 0.02;
			double f = this.random.nextGaussian() * 0.02;
			this.level().addParticle(particleOptions, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), d, e, f);
		}

	}

	@Override
	public void handleEntityEvent(byte b) {
		if (b == 7) {
			this.rideablePolarBears$spawnTamingParticles(true);
		} else if (b == 6) {
			this.rideablePolarBears$spawnTamingParticles(false);
		} else {
			super.handleEntityEvent(b);
		}

	}

	@Unique
	public boolean rideablePolarBears$isInSittingPose() {
		return this.rideablePolarBears$getFlag(rideablePolarBears$SITTING_FLAG);
	}

	@Unique
	public void rideablePolarBears$setInSittingPose(boolean sitting) {
		this.rideablePolarBears$setFlag(rideablePolarBears$SITTING_FLAG, sitting);
	}

	@Unique
	@Override
	public boolean rideablePolarBears$isTamed() {
		return this.rideablePolarBears$getFlag(rideablePolarBears$TAMED_FLAG);
	}

	@Unique
	public void rideablePolarBears$setTame(boolean tame) {
		this.rideablePolarBears$setFlag(rideablePolarBears$TAMED_FLAG, tame);
		this.rideablePolarBears$reassessTameGoals();
	}

	@Unique
	public void rideablePolarBears$tame(@NotNull Player player) {
		this.rideablePolarBears$setTame(true);
		this.rideablePolarBears$setOwner(player);
		if (player instanceof ServerPlayer) {
			CriteriaTriggers.TAME_ANIMAL.trigger((ServerPlayer)player, this);
		}

	}

	@Override
	public boolean canAttack(@NotNull LivingEntity livingEntity) {
		return !this.rideablePolarBears$isOwnedBy(livingEntity) && super.canAttack(livingEntity);
	}

	@Unique
	@Override
	public boolean rideablePolarBears$wantsToAttack(LivingEntity livingEntity, LivingEntity livingEntity2) {
		if (livingEntity instanceof Creeper || livingEntity instanceof Ghast) {
			return false;
		} else if (livingEntity instanceof Wolf wolf) {
			return !wolf.isTame() || wolf.getOwner() != livingEntity2;
		} else if (livingEntity instanceof Player && livingEntity2 instanceof Player && !((Player)livingEntity2).canHarmPlayer((Player)livingEntity)) {
			return false;
		} else if (livingEntity instanceof AbstractHorse && ((AbstractHorse)livingEntity).isTamed()) {
			return false;
		} else {
			return !(livingEntity instanceof TamableAnimal) || !((TamableAnimal)livingEntity).isTame();
		}
	}

	@Unique
	public boolean rideablePolarBears$isOwnedBy(LivingEntity livingEntity) {
		return livingEntity == this.getOwner();
	}

	@Override
	public PlayerTeam getTeam() {
		if (this.rideablePolarBears$isTamed()) {
			LivingEntity livingEntity = this.getOwner();
			if (livingEntity != null) {
				return livingEntity.getTeam();
			}
		}

		return super.getTeam();
	}

	@Override
	public boolean considersEntityAsAlly(@NotNull Entity entity) {
		if (this.rideablePolarBears$isTamed()) {
			LivingEntity livingEntity = this.getOwner();
			if (entity == livingEntity) {
				return true;
			}
		}

		return super.considersEntityAsAlly(entity);
	}

	@Override
	public void die(@NotNull DamageSource damageSource) {
		super.die(damageSource);
		if (this.dead
				&& this.level() instanceof ServerLevel serverLevel
				&& serverLevel.getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES)
				&& this.getOwner() instanceof ServerPlayer serverPlayer) {
			serverPlayer.sendSystemMessage(this.getCombatTracker().getDeathMessage());
		}
	}

	@Unique
	@Override
	public boolean rideablePolarBears$isOrderedToSit() {
		return this.rideablePolarBears$orderedToSit;
	}

	@Unique
	@Override
	public void rideablePolarBears$setOrderedToSit(boolean bl) {
		this.rideablePolarBears$orderedToSit = bl;
	}
}
