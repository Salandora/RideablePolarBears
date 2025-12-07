package com.github.salandora.rideablepolarbears.attachment.neoforge;

import com.github.salandora.rideablepolarbears.attachment.AttachmentType;
import com.github.salandora.rideablepolarbears.attachment.EntityAttachment;
import com.github.salandora.rideablepolarbears.neoforge.RideablePolarBearsNeoForge;
import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.attachment.AttachmentSyncHandler;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class EntityAttachmentImpl extends EntityAttachment {
	private static final Map<ResourceLocation, AttachmentType<?>> entityAttachments = new HashMap<>();

	public static <T> AttachmentType<T> fromId(ResourceLocation id) {
		return (AttachmentType<T>) entityAttachments.get(id);
	}

	@Override
	public <T> T getData(Entity e, AttachmentType<T> type) {
		return e.getData((net.neoforged.neoforge.attachment.AttachmentType<T>) type.attachmentType());
	}

	@Override
	@Nullable
	public <T> T setData(Entity e, AttachmentType<T> type, T data) {
		return e.setData((net.neoforged.neoforge.attachment.AttachmentType<T>) type.attachmentType(), data);
	}

	@Override
	@Nullable
	public <T> T removeData(Entity e, AttachmentType<T> type) {
		return e.removeData((net.neoforged.neoforge.attachment.AttachmentType<T>) type.attachmentType());
	}

	@Override
	public <T> void registerOnAttachmentSet(Entity e, AttachmentType<T> type, EntityAttachment.OnAttachmentSet<T> callback) {
		((AttachmentInterface) e).rideableRavagers$onAttachedSet(type).add(callback);
	}

	@Override
	public <T> AttachmentType<T> create(ResourceLocation id, Consumer<Builder<T>> consumer) {
		BuilderImpl<T> builder = new BuilderImpl<>(id);

		consumer.accept(builder);

		return builder.build();
	}

	private static class BuilderImpl<T> implements EntityAttachment.Builder<T> {
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
			Supplier<net.neoforged.neoforge.attachment.AttachmentType<T>> type = RideablePolarBearsNeoForge.ATTACHMENT_TYPES.register(id.getPath(), () -> {
				net.neoforged.neoforge.attachment.AttachmentType.Builder<T> builder = net.neoforged.neoforge.attachment.AttachmentType.builder(this.defaultValue);
				if (this.persistent != null) {
					builder.serialize(this.persistent.fieldOf(id.getPath()));
				}
				if (this.copyOnDeath) {
					builder.copyOnDeath();
				}
				if (this.packetCodec != null) {
					builder.sync(new SyncHandler<>(this.id, this.packetCodec));
				}

				return builder.build();
			});

			AttachmentType<T> wrapped = new AttachmentType<>() {
				private net.neoforged.neoforge.attachment.AttachmentType<T> value;

				@Override
				public <A> A attachmentType() {
					if (value == null) {
						value = type.get();
					}

					return (A) value;
				}
			};
			entityAttachments.put(this.id, wrapped);
			return wrapped;
		}
	}

	private record SyncHandler<T>(ResourceLocation id, StreamCodec<? super RegistryFriendlyByteBuf, T> packetCodec)
			implements AttachmentSyncHandler<T> {
		@Override
		public void write(RegistryFriendlyByteBuf buf, T data, boolean initialSync) {
			this.packetCodec.encode(buf, data);
		}

		@Override
		public T read(IAttachmentHolder holder, RegistryFriendlyByteBuf buf, @Nullable T previousData) {
			T newData = this.packetCodec.decode(buf);

			AttachmentType<T> type = EntityAttachmentImpl.fromId(this.id);
			if (type != null) {
				Minecraft.getInstance().execute(() -> ((AttachmentInterface) holder).rideableRavagers$invokeOnAttacheSet(type, previousData, newData));
			}

			return newData;
		}

		@Override
		public boolean sendToPlayer(IAttachmentHolder holder, ServerPlayer to) {
			return true;
		}
	}
}
