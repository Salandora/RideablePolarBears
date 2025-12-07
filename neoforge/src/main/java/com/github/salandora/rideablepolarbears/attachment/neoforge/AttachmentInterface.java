package com.github.salandora.rideablepolarbears.attachment.neoforge;


import com.github.salandora.rideablepolarbears.attachment.AttachmentType;
import com.github.salandora.rideablepolarbears.attachment.EntityAttachment;

import java.util.List;

public interface AttachmentInterface {
	<T> List<EntityAttachment.OnAttachmentSet<T>> rideableRavagers$onAttachedSet(AttachmentType<T> type);

	<T> void rideableRavagers$invokeOnAttacheSet(AttachmentType<T> type, T oldValue, T newValue);
}
