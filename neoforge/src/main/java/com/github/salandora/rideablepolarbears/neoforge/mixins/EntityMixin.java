package com.github.salandora.rideablepolarbears.neoforge.mixins;

import com.github.salandora.rideablepolarbears.attachment.AttachmentType;
import com.github.salandora.rideablepolarbears.attachment.EntityAttachment;
import com.github.salandora.rideablepolarbears.attachment.neoforge.AttachmentInterface;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@Mixin(Entity.class)
public class EntityMixin implements AttachmentInterface {
	@Unique
	private Map<AttachmentType<?>, List<EntityAttachment.OnAttachmentSet<?>>> rideableRavagers$attachmentCallbacks = null;

	@Unique
	public <T> List<EntityAttachment.OnAttachmentSet<T>> rideableRavagers$onAttachedSet(AttachmentType<T> type) {
		if (rideableRavagers$attachmentCallbacks == null) {
			rideableRavagers$attachmentCallbacks = new IdentityHashMap<>();
		}

		return (List<EntityAttachment.OnAttachmentSet<T>>) (List<?>) rideableRavagers$attachmentCallbacks.computeIfAbsent(type, t -> new ArrayList<>());
	}

	@Override
	public <T> void rideableRavagers$invokeOnAttacheSet(AttachmentType<T> type, T oldValue, T newValue) {
		if (rideableRavagers$attachmentCallbacks == null || !rideableRavagers$attachmentCallbacks.containsKey(type)) {
			return;
		}

		rideableRavagers$attachmentCallbacks.get(type).forEach(callback -> ((EntityAttachment.OnAttachmentSet<T>) callback).onAttachedSet(oldValue, newValue));
	}
}
