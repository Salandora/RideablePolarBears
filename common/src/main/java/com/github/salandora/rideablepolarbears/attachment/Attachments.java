package com.github.salandora.rideablepolarbears.attachment;

import com.github.salandora.rideablepolarbears.RideablePolarBears;
import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;
import java.util.UUID;

public class Attachments {
	public static final AttachmentType<Byte> POLARBEAR_FLAGS = EntityAttachment.INSTANCE.create(ResourceLocation.fromNamespaceAndPath(RideablePolarBears.MODID, "flags"),
			builder -> builder.initializer(() -> (byte) 0).persistent(Codec.BYTE).synchronize(ByteBufCodecs.BYTE));

	public static final AttachmentType<Optional<EntityReference<LivingEntity>>> POLARBEAR_OWNER = EntityAttachment.INSTANCE.create(ResourceLocation.fromNamespaceAndPath(RideablePolarBears.MODID, "owner"),
			builder -> builder.initializer(Optional::empty).synchronize(EntityReference.<LivingEntity>streamCodec().apply(ByteBufCodecs::optional)));

	public static void init() {
	}
}
