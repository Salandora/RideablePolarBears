package com.github.salandora.rideablepolarbears.attachment.neoforge;

import com.github.salandora.rideablepolarbears.attachment.AttachmentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class NeoForgeAttachmentType<T> implements AttachmentType<T> {
	private final ResourceLocation id;
	private final Supplier<net.neoforged.neoforge.attachment.AttachmentType<T>> attachmentType;
	private final StreamCodec<? super RegistryFriendlyByteBuf, T> packetCodec;

	public ResourceLocation id() {
		return id;
	}

	@Nullable
	public StreamCodec<? super RegistryFriendlyByteBuf, T> packetCodec() {
		return packetCodec;
	}

	public boolean shouldSync() {
		return packetCodec != null;
	}

	@Override
	public <A> A attachmentType() {
		return (A) attachmentType.get();
	}

	NeoForgeAttachmentType(ResourceLocation id, Supplier<net.neoforged.neoforge.attachment.AttachmentType<T>> attachmentType, @Nullable StreamCodec<? super RegistryFriendlyByteBuf, T> packetCodec) {
		this.id = id;
		this.attachmentType = attachmentType;
		this.packetCodec = packetCodec;
	}
}
