package com.github.salandora.rideablepolarbears.fabric;

import com.github.salandora.rideablepolarbears.RideablePolarBears;
import net.fabricmc.api.ModInitializer;

public class RideablePolarBearsFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		RideablePolarBears.init();
	}
}
