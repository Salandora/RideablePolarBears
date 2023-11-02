package com.github.salandora.rideablepolarbears;

import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import space.vectrix.ignite.api.Platform;
import space.vectrix.ignite.api.event.Subscribe;
import space.vectrix.ignite.api.event.platform.PlatformInitializeEvent;

public final class RideablePolarBearsMod {
	private final Logger logger;
	private final Platform platform;

	@Inject
	public RideablePolarBearsMod(final Logger logger, final Platform platform) {
		this.logger = logger;
		this.platform = platform;
	}

	@Subscribe
	public void onInitialize(final @NotNull PlatformInitializeEvent event) {
	}
}