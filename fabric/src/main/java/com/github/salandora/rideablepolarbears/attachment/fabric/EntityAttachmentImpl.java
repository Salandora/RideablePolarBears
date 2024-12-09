package com.github.salandora.rideablepolarbears.attachment.fabric;

import com.github.salandora.rideablepolarbears.attachment.AttachmentType;
import com.github.salandora.rideablepolarbears.attachment.EntityAttachment;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class EntityAttachmentImpl extends EntityAttachment {
	@Override
	public <T> T getData(Entity e, AttachmentType<T> type) {
		return e.getAttachedOrCreate(type.attachmentType());
	}

	@Override
	@Nullable
	public <T> T setData(Entity e, AttachmentType<T> type, T data) {
		return e.setAttached(type.attachmentType(), data);
	}

	@Override
	@Nullable
	public <T> T removeData(Entity e, AttachmentType<T> type) {
		return e.removeAttached(type.attachmentType());
	}

	@Override
	public <T> AttachmentType<T> create(ResourceLocation id, Consumer<Builder<T>> consumer) {
		net.fabricmc.fabric.api.attachment.v1.AttachmentType<T> type = AttachmentRegistry.create(id, builder -> consumer.accept(new BuilderImpl<>(builder)));
		return new AttachmentType<>() {
			@Override
			public <A> A attachmentType() {
				return (A) type;
			}
		};
	}

	public static class BuilderImpl<T> implements EntityAttachment.Builder<T> {
		AttachmentRegistry.Builder<T> builder;

		BuilderImpl(AttachmentRegistry.Builder<T> builder) {
			this.builder = builder;
		}

		public Builder<T> initializer(Supplier<T> defaultValue) {
			this.builder.initializer(defaultValue);
			return this;
		}
		public Builder<T> persistent(Codec<T> codec) {
			this.builder.persistent(codec);
			return this;
		}
		public Builder<T> copyOnDeath() {
			this.builder.copyOnDeath();
			return this;
		}
		public Builder<T> synchronize(StreamCodec<? super RegistryFriendlyByteBuf, T> packetCodec) {
			this.builder.syncWith(packetCodec, AttachmentSyncPredicate.all());
			return this;
		}
	}
}
