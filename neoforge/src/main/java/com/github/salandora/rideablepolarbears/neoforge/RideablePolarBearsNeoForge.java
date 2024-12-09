package com.github.salandora.rideablepolarbears.neoforge;

import com.github.salandora.rideablepolarbears.RideablePolarBears;
import com.github.salandora.rideablepolarbears.attachment.Attachments;
import com.github.salandora.rideablepolarbears.neoforge.networking.SetAttachmentType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.PolarBear;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@Mod(RideablePolarBears.MODID)
public class RideablePolarBearsNeoForge {
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, RideablePolarBears.MODID);

	public RideablePolarBearsNeoForge(IEventBus modEventBus, ModContainer modContainer) {
		RideablePolarBears.init();

		ATTACHMENT_TYPES.register(modEventBus);

		modEventBus.addListener(RideablePolarBearsNeoForge::registerPayloads);

		IEventBus eventBus = NeoForge.EVENT_BUS;
		eventBus.addListener(RideablePolarBearsNeoForge::onStartTracking);
	}

	public static void registerPayloads(final RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar("1");

		registrar.playToClient(
				SetAttachmentType.TYPE,
				SetAttachmentType.STREAM_CODEC,
				SetAttachmentType::handlePayload
		);
	}

	public static void onStartTracking(PlayerEvent.StartTracking event) {
		if (event.getTarget() instanceof PolarBear e) {
			ServerPlayer player = (ServerPlayer) event.getEntity();
			PacketDistributor.sendToPlayer(player,
					SetAttachmentType.create(e.getId(), Attachments.POLARBEAR_FLAGS, e.getData(Attachments.POLARBEAR_FLAGS::attachmentType), player.registryAccess()),
					SetAttachmentType.create(e.getId(), Attachments.POLARBEAR_OWNER, e.getData(Attachments.POLARBEAR_OWNER::attachmentType), player.registryAccess())
			);
		}
	}
}
