package com.github.salandora.rideablepolarbears.mixins;

import com.github.salandora.rideablepolarbears.entity.Tamable;
import com.github.salandora.rideablepolarbears.entity.ai.goal.IPolarBearAttackPlayersGoal;
import com.github.salandora.rideablepolarbears.entity.ai.goal.PolarBearOwnerHurtByTargetGoal;
import com.github.salandora.rideablepolarbears.entity.ai.goal.PolarBearOwnerHurtTargetGoal;
import com.github.salandora.rideablepolarbears.entity.ai.goal.PolarBearSitWhenOrderedToGoal;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ItemBasedSteering;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("WrongEntityDataParameterClass")
@Mixin(PolarBear.class)
public abstract class PolarBearsMixin extends Animal implements NeutralMob, Tamable, PlayerRideableJumping, Saddleable {
	@Shadow public abstract boolean isStanding();

	@Shadow public abstract void setStanding(boolean bl);

	@Shadow
	private float clientSideStandAnimationO;

	@Unique
	private static final EntityDataAccessor<Byte> rideablePolarBears$DATA_FLAGS_ID = SynchedEntityData.defineId(PolarBear.class, EntityDataSerializers.BYTE);
	@Unique
	private static final EntityDataAccessor<Optional<UUID>> rideablePolarBears$DATA_OWNERUUID_ID = SynchedEntityData.defineId(PolarBear.class, EntityDataSerializers.OPTIONAL_UUID);
	@Unique
	private static final EntityDataAccessor<Integer> rideablePolarBears$BOOST_TIME = SynchedEntityData.defineId(PolarBear.class, EntityDataSerializers.INT);
	@Unique
	private static final EntityDataAccessor<Boolean> rideablePolarBears$SADDLED = SynchedEntityData.defineId(PolarBear.class, EntityDataSerializers.BOOLEAN);
	@Unique
	private ItemBasedSteering rideablePolarBears$saddledComponent;
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

	@Override
	public boolean isFood(@NotNull ItemStack itemStack) {
		return itemStack.is(ItemTags.FISHES);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void rideablePolarBears$constructor(EntityType<? extends Animal> entityType, Level world, CallbackInfo ci) {
		this.rideablePolarBears$saddledComponent = new ItemBasedSteering(this.entityData, rideablePolarBears$BOOST_TIME, rideablePolarBears$SADDLED);
		this.rideablePolarBears$reassessTameGoals();
	}

	@SuppressWarnings("DataFlowIssue")
	@Inject(method = "registerGoals", at = @At("TAIL"))
	private void rideablePolarBears$initGoals(CallbackInfo ci) {
		this.goalSelector.addGoal(2, new PolarBearSitWhenOrderedToGoal((PolarBear) (Object) this));
		this.goalSelector.addGoal(3, new BreedGoal(this, 1.0, PolarBear.class));
		this.goalSelector.addGoal(3, new TemptGoal(this, 1.25, Ingredient.of(ItemTags.FISHES), false));
		this.targetSelector.addGoal(1, new PolarBearOwnerHurtByTargetGoal((PolarBear) (Object) this));
		this.targetSelector.addGoal(2, new PolarBearOwnerHurtTargetGoal((PolarBear) (Object) this));
	}

	@Unique
	protected void rideablePolarBears$reassessTameGoals() {
		if (rideablePolarBears$isTame()) {
			this.targetSelector.getAvailableGoals().removeIf(goal -> goal instanceof IPolarBearAttackPlayersGoal);
		}
	}

	@Inject(method = "defineSynchedData", at = @At("TAIL"))
	private void rideablePolarBears$defineSynchedData(CallbackInfo ci) {
		this.entityData.define(rideablePolarBears$DATA_FLAGS_ID, (byte)0);
		this.entityData.define(rideablePolarBears$DATA_OWNERUUID_ID, Optional.empty());
		this.entityData.define(rideablePolarBears$BOOST_TIME, 0);
		this.entityData.define(rideablePolarBears$SADDLED, false);
	}

	@Override
	public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> data) {
		if (rideablePolarBears$BOOST_TIME.equals(data) && this.level.isClientSide) {
			this.rideablePolarBears$saddledComponent.onSynced();
		}

		super.onSyncedDataUpdated(data);
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
		if (this.rideablePolarBears$isTame()) {
			if (foodItem && this.getHealth() < this.getMaxHealth()) {
				if (!player.getAbilities().instabuild) {
					itemStack.shrink(1);
				}

				//noinspection DataFlowIssue
				this.heal((float) itemStack.getItem().getFoodProperties().getNutrition());
				return InteractionResult.SUCCESS;
			} else if (!foodItem && this.isSaddled() && !this.isVehicle() && !this.isBaby() && this.rideablePolarBears$isOwnedBy(player) && !player.isSecondaryUseActive()) {
				this.rideablePolarBears$doPlayerRide(player);
				return InteractionResult.sidedSuccess(this.level.isClientSide);
			} else {
				InteractionResult actionResult = super.mobInteract(player, hand);
				if (!actionResult.consumesAction()) {
					if (!this.isBaby() && itemStack.is(Items.SADDLE)) {
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
				this.level.broadcastEntityEvent(this, (byte)7);
			} else {
				this.level.broadcastEntityEvent(this, (byte)6);
			}

			this.setPersistenceRequired();
			return InteractionResult.CONSUME;
		}

		return super.mobInteract(player, hand);
	}

	@Unique
	protected void rideablePolarBears$doPlayerRide(Player player) {
		this.setStanding(false);
		this.rideablePolarBears$setOrderedToSit(false);
		if (!this.level.isClientSide) {
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
	public boolean isSaddled() {
		return this.rideablePolarBears$saddledComponent.hasSaddle();
	}

	@Override
	public boolean isSaddleable() {
		return this.isAlive() && !this.isBaby();
	}

	@Override
	public void equipSaddle(@Nullable SoundSource sound) {
		this.rideablePolarBears$saddledComponent.setSaddle(true);
		if (sound != null) {
			this.level.playSound(null, this, SoundEvents.POLAR_BEAR_AMBIENT, sound, 0.5F, 1.0F);
		}
	}

	@Override
	protected void dropEquipment() {
		super.dropEquipment();
		if (this.isSaddled()) {
			this.spawnAtLocation(Items.SADDLE);
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
	public void travel(Vec3 vec3) {
		if (this.isAlive()) {
			LivingEntity livingEntity = this.getControllingPassenger();
			if (this.isVehicle() && livingEntity != null) {
				this.setYRot(livingEntity.getYRot());
				this.yRotO = this.getYRot();
				this.setXRot(livingEntity.getXRot() * 0.5F);
				this.setRot(this.getYRot(), this.getXRot());
				this.yBodyRot = this.getYRot();
				this.yHeadRot = this.yBodyRot;
				float f = livingEntity.xxa * 0.5F;
				float g = livingEntity.zza;
				if (g <= 0.0F) {
					g *= 0.25F;
				}

				if (this.onGround && this.rideablePolarBears$playerJumpPendingScale == 0.0F && this.isStanding() && !this.rideablePolarBears$allowStandSliding) {
					f = 0.0F;
					g = 0.0F;
				}

				if (this.rideablePolarBears$playerJumpPendingScale > 0.0F && !this.rideablePolarBears$isJumping() && this.onGround) {
					double d = this.rideablePolarBears$getCustomJump() * (double) this.rideablePolarBears$playerJumpPendingScale * (double) this.getBlockJumpFactor();
					double e = d + this.getJumpBoostPower();
					Vec3 vec32 = this.getDeltaMovement();
					this.setDeltaMovement(vec32.x, e, vec32.z);
					this.rideablePolarBears$setIsJumping(true);
					this.hasImpulse = true;
					if (g > 0.0F) {
						float h = Mth.sin(this.getYRot() * 0.017453292F);
						float i = Mth.cos(this.getYRot() * 0.017453292F);
						this.setDeltaMovement(this.getDeltaMovement().add(-0.4F * h * this.rideablePolarBears$playerJumpPendingScale, 0.0, 0.4F * i * this.rideablePolarBears$playerJumpPendingScale));
					}

					this.rideablePolarBears$playerJumpPendingScale = 0.0F;
				}

				this.flyingSpeed = this.getSpeed() * 0.1F;
				if (this.isControlledByLocalInstance()) {
					this.setSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED));
					super.travel(new Vec3(f, vec3.y, g));
				} else if (livingEntity instanceof Player) {
					this.setDeltaMovement(Vec3.ZERO);
				}

				if (this.onGround) {
					this.rideablePolarBears$playerJumpPendingScale = 0.0F;
					this.rideablePolarBears$setIsJumping(false);
				}

				this.calculateEntityAnimation(this, false);
				this.tryCheckInsideBlocks();
			} else {
				this.flyingSpeed = 0.02F;
				super.travel(vec3);
			}
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
					double d = this.level.getBlockFloorHeight(mutableBlockPos);
					if (DismountHelper.isBlockFloorValid(d)) {
						Vec3 vec3 = Vec3.upFromBottomCenterOf(mutableBlockPos, d);
						if (DismountHelper.canDismountTo(this.level, livingEntity, aABB.move(vec3))) {
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
	public void positionRider(@NotNull Entity entity) {
		super.positionRider(entity);
		if (this.clientSideStandAnimationO > 0.0F) {
			float f = Mth.sin(this.yBodyRot * (float) (Math.PI / 180.0));
			float g = Mth.cos(this.yBodyRot * (float) (Math.PI / 180.0));
			float h = (0.8F / 6.0F) * this.clientSideStandAnimationO;
			float i = -(0.5F / 6.0F) * this.clientSideStandAnimationO;
			entity.setPos(
					this.getX() + (double)(h * f),
					this.getY() + this.getPassengersRidingOffset() + entity.getMyRidingOffset() + (double)i,
					this.getZ() - (double)(h * g)
			);
			if (entity instanceof LivingEntity) {
				((LivingEntity)entity).yBodyRot = this.yBodyRot;
			}
		}
	}

	@Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
	private void rideablePolarBears$writeCustomDataToNbt(@NotNull CompoundTag compoundTag, CallbackInfo ci) {
		compoundTag.putBoolean("Tame", this.rideablePolarBears$isTame());
		if (this.getOwnerUUID() != null) {
			compoundTag.putUUID("Owner", this.getOwnerUUID());
		}

		this.rideablePolarBears$saddledComponent.addAdditionalSaveData(compoundTag);
		compoundTag.putBoolean("Sitting", this.rideablePolarBears$orderedToSit);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
	private void rideablePolarBears$readCustomDataToNbt(@NotNull CompoundTag compoundTag, CallbackInfo ci) {
		this.rideablePolarBears$setTame(compoundTag.getBoolean("Tame"));
		UUID uUID = null;
		if (compoundTag.hasUUID("Owner")) {
			uUID = compoundTag.getUUID("Owner");
		}

		if (uUID != null) {
			try {
				this.rideablePolarBears$setOwnerUUID(uUID);
				this.rideablePolarBears$setTame(true);
			} catch (Throwable var4) {
				this.rideablePolarBears$setTame(false);
			}
		}

		this.rideablePolarBears$saddledComponent.readAdditionalSaveData(compoundTag);
		this.rideablePolarBears$orderedToSit = compoundTag.getBoolean("Sitting");
		this.rideablePolarBears$setInSittingPose(this.rideablePolarBears$orderedToSit);
	}

	@Override
	public boolean canBeLeashed(@NotNull Player player) {
		return !this.isAngry() && super.canBeLeashed(player);
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
			this.level.addParticle(particleOptions, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), d, e, f);
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
		return (this.entityData.get(rideablePolarBears$DATA_FLAGS_ID) & 1) != 0;
	}

	@Unique
	public void rideablePolarBears$setInSittingPose(boolean bl) {
		byte b = this.entityData.get(rideablePolarBears$DATA_FLAGS_ID);
		if (bl) {
			this.entityData.set(rideablePolarBears$DATA_FLAGS_ID, (byte)(b | 1));
		} else {
			this.entityData.set(rideablePolarBears$DATA_FLAGS_ID, (byte)(b & -2));
		}
	}

	@Unique
	@Override
	public boolean rideablePolarBears$isTame() {
		return (this.entityData.get(rideablePolarBears$DATA_FLAGS_ID) & 4) != 0;
	}

	@Unique
	public void rideablePolarBears$setTame(boolean bl) {
		byte b = this.entityData.get(rideablePolarBears$DATA_FLAGS_ID);
		if (bl) {
			this.entityData.set(rideablePolarBears$DATA_FLAGS_ID, (byte)(b | 4));
		} else {
			this.entityData.set(rideablePolarBears$DATA_FLAGS_ID, (byte)(b & -5));
		}

		this.rideablePolarBears$reassessTameGoals();
	}

	@Nullable
	@Override
	public UUID getOwnerUUID() {
		return this.entityData.get(rideablePolarBears$DATA_OWNERUUID_ID).orElse(null);
	}
	@Unique
	public void rideablePolarBears$setOwnerUUID(@Nullable UUID uUID) {
		this.entityData.set(rideablePolarBears$DATA_OWNERUUID_ID, Optional.ofNullable(uUID));
	}

	@Nullable
	public LivingEntity getOwner() {
		try {
			UUID uUID = this.getOwnerUUID();
			return uUID == null ? null : this.level.getPlayerByUUID(uUID);
		} catch (IllegalArgumentException var2) {
			return null;
		}
	}

	@Unique
	public void rideablePolarBears$tame(@NotNull Player player) {
		this.rideablePolarBears$setTame(true);
		this.rideablePolarBears$setOwnerUUID(player.getUUID());
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
	public Team getTeam() {
		if (this.rideablePolarBears$isTame()) {
			Entity entity = this.getOwner();
			if (entity != null) {
				return entity.getTeam();
			}
		}

		return super.getTeam();
	}

	@Override
	public boolean isAlliedTo(@NotNull Entity entity) {
		if (this.rideablePolarBears$isTame()) {
			Entity owner = this.getOwner();
			if (entity == owner) {
				return true;
			}

			if (owner != null) {
				return owner.isAlliedTo(entity);
			}
		}

		return super.isAlliedTo(entity);
	}

	@Override
	public void die(@NotNull DamageSource damageSource) {
		if (!this.level.isClientSide && this.level.getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES) && this.getOwner() instanceof ServerPlayer) {
			this.getOwner().sendSystemMessage(this.getCombatTracker().getDeathMessage());
		}

		super.die(damageSource);
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
