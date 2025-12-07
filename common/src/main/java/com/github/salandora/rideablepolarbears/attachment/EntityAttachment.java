package com.github.salandora.rideablepolarbears.attachment;

import com.mojang.serialization.Codec;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class EntityAttachment {
	public static final EntityAttachment INSTANCE;

	static {
		Class<?> clazz;
		try {
			// Try fabric
			clazz = Class.forName("com.github.salandora.rideablepolarbears.attachment.fabric.EntityAttachmentImpl");
		} catch (ReflectiveOperationException e) {
			try {
				// Try neoforge
				clazz = Class.forName("com.github.salandora.rideablepolarbears.attachment.neoforge.EntityAttachmentImpl");
			} catch (ReflectiveOperationException ex) {
				throw new RuntimeException("Could not find EntityAttachmentImpl for fabric nor neoforge.");
			}
		}

		try {
			INSTANCE = (EntityAttachment) clazz.getConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Could not instantiate EntityAttachmentImpl.", e);
		}
	}

	public abstract <T> T getData(Entity e, AttachmentType<T> type);

	@Nullable
	public abstract <T> T setData(Entity e, AttachmentType<T> type, T data);

	@Nullable
	public abstract <T> T removeData(Entity e, AttachmentType<T> type);

	public abstract <T> void registerOnAttachmentSet(Entity e, AttachmentType<T> type, OnAttachmentSet<T> callback);

	public abstract <T> AttachmentType<T> create(ResourceLocation id, Consumer<Builder<T>> consumer);

	public interface Builder<T> {
		Builder<T> initializer(Supplier<T> defaultValue);
		Builder<T> persistent(Codec<T> codec);
		Builder<T> copyOnDeath();
		Builder<T> synchronize(StreamCodec<? super RegistryFriendlyByteBuf, T> packetCodec);
	}

	@FunctionalInterface
	public interface OnAttachmentSet<A> {
		void onAttachedSet(@Nullable A oldValue, @Nullable A newValue);
	}
}
