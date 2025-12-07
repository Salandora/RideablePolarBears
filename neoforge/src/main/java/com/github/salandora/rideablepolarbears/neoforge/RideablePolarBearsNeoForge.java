package com.github.salandora.rideablepolarbears.neoforge;

import com.github.salandora.rideablepolarbears.RideablePolarBears;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@Mod(RideablePolarBears.MODID)
public class RideablePolarBearsNeoForge {
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, RideablePolarBears.MODID);

	public RideablePolarBearsNeoForge(IEventBus modEventBus, ModContainer modContainer) {
		RideablePolarBears.init();

		ATTACHMENT_TYPES.register(modEventBus);
	}
}
