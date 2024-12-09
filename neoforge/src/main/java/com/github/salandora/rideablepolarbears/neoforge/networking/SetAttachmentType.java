package com.github.salandora.rideablepolarbears.neoforge.networking;

import com.github.salandora.rideablepolarbears.RideablePolarBears;
import com.github.salandora.rideablepolarbears.attachment.neoforge.EntityAttachmentImpl;
import com.github.salandora.rideablepolarbears.attachment.neoforge.NeoForgeAttachmentType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;
import java.util.Objects;

public record SetAttachmentType(int entityID, NeoForgeAttachmentType<?> attachmentType, byte[] data) implements CustomPacketPayload {
	public static final Type<SetAttachmentType> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RideablePolarBears.MODID, "setattachment"));

	public static final StreamCodec<ByteBuf, SetAttachmentType> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SetAttachmentType::entityID,
			ResourceLocation.STREAM_CODEC.map(
					id -> Objects.requireNonNull(EntityAttachmentImpl.fromId(id)),
					NeoForgeAttachmentType::id
			), SetAttachmentType::attachmentType,
			ByteBufCodecs.BYTE_ARRAY, SetAttachmentType::data,
			SetAttachmentType::new
	);

	public static <T> SetAttachmentType create(int entityID, com.github.salandora.rideablepolarbears.attachment.AttachmentType<T> attachmentType, @Nullable T data, RegistryAccess registryAccess) {
		return create(entityID, (NeoForgeAttachmentType<T>) attachmentType, data, registryAccess);
	}
	public static <T> SetAttachmentType create(int entityID, NeoForgeAttachmentType<T> attachmentType, @Nullable T data, RegistryAccess registryAccess) {
		StreamCodec<? super RegistryFriendlyByteBuf, T> codec = attachmentType.packetCodec();
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(new FriendlyByteBuf(Unpooled.buffer()), registryAccess);

		if (data != null) {
			buf.writeBoolean(true);
			codec.encode(buf, data);
		} else {
			buf.writeBoolean(false);
		}

		return new SetAttachmentType(entityID, attachmentType, buf.array());
	}

	public static void handlePayload(SetAttachmentType packet, IPayloadContext context) {
		Level level = context.player().level();
		packet.apply(level);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public void apply(Level level) {
		Entity target = target(level);
		if (target == null) {
			return;
		}

		Object data = data(level.registryAccess());
		target.setData((AttachmentType<Object>) attachmentType.attachmentType(), data);
	}

	private Entity target(Level level) {
		return level.getEntity(entityID);
	}

	private Object data(RegistryAccess registryAccess) {
		StreamCodec<? super RegistryFriendlyByteBuf, ?> codec = attachmentType.packetCodec();
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.copiedBuffer(data), registryAccess);

		if (!buf.readBoolean()) {
			return null;
		}

		return codec.decode(buf);
	}
}
