package com.github.salandora.rideablepolarbears.attachment.neoforge;

import com.github.salandora.rideablepolarbears.attachment.AttachmentType;
import com.github.salandora.rideablepolarbears.attachment.EntityAttachment;
import com.github.salandora.rideablepolarbears.neoforge.RideablePolarBearsNeoForge;
import com.github.salandora.rideablepolarbears.neoforge.networking.SetAttachmentType;
import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class EntityAttachmentImpl extends EntityAttachment {
	private static final Map<ResourceLocation, NeoForgeAttachmentType<?>> entityAttachments = new HashMap<>();

	public static <T> NeoForgeAttachmentType<T> fromId(ResourceLocation id) {
		return (NeoForgeAttachmentType<T>) entityAttachments.get(id);
	}

	@Override
	public <T> T getData(Entity e, AttachmentType<T> type) {
		return e.getData((net.neoforged.neoforge.attachment.AttachmentType<T>) type.attachmentType());
	}

	@Override
	@Nullable
	public <T> T setData(Entity e, AttachmentType<T> type, T data) {
		T oldValue = e.setData((net.neoforged.neoforge.attachment.AttachmentType<T>) type.attachmentType(), data);
		if (oldValue != data && !e.level().isClientSide) {
			NeoForgeAttachmentType<T> neoType = (NeoForgeAttachmentType<T>)type;
			if (neoType.shouldSync()) {
				SetAttachmentType packet = SetAttachmentType.create(e.getId(), neoType, data, e.level().registryAccess());
				PacketDistributor.sendToPlayersTrackingEntity(e, packet);
			}
		}
		return oldValue;
	}

	@Override
	@Nullable
	public <T> T removeData(Entity e, AttachmentType<T> type) {
		return e.removeData((net.neoforged.neoforge.attachment.AttachmentType<T>) type.attachmentType());
	}

	@Override
	public <T> AttachmentType<T> create(ResourceLocation id, Consumer<Builder<T>> consumer) {
		BuilderImpl<T> builder = new BuilderImpl<>(id);

		consumer.accept(builder);

		return builder.build();
	}

	public static class BuilderImpl<T> implements EntityAttachment.Builder<T> {
		private final ResourceLocation id;
		private Supplier<T> defaultValue;
		private Codec<T> persistent;
		private boolean copyOnDeath = false;
		private StreamCodec<? super RegistryFriendlyByteBuf, T> packetCodec = null;

		BuilderImpl(ResourceLocation id) {
			this.id = id;
		}

		public Builder<T> initializer(Supplier<T> defaultValue) {
			this.defaultValue = defaultValue;
			return this;
		}
		public Builder<T> persistent(Codec<T> codec) {
			this.persistent = codec;
			return this;
		}
		public Builder<T> copyOnDeath() {
			this.copyOnDeath = true;
			return this;
		}
		public Builder<T> synchronize(StreamCodec<? super RegistryFriendlyByteBuf, T> packetCodec){
			this.packetCodec = packetCodec;
			return this;
		}

		AttachmentType<T> build() {
			Supplier<net.neoforged.neoforge.attachment.AttachmentType<T>> attachmentType = RideablePolarBearsNeoForge.ATTACHMENT_TYPES.register(id.getPath(), () -> {
				net.neoforged.neoforge.attachment.AttachmentType.Builder<T> builder = net.neoforged.neoforge.attachment.AttachmentType.builder(this.defaultValue);
				if (this.persistent != null) {
					builder.serialize(this.persistent);
				}
				if (this.copyOnDeath) {
					builder.copyOnDeath();
				}

				return builder.build();
			});

			NeoForgeAttachmentType<T> wrapped = new NeoForgeAttachmentType<>(id, attachmentType, this.packetCodec);
			entityAttachments.put(this.id, wrapped);
			return wrapped;
		}
	}
}
