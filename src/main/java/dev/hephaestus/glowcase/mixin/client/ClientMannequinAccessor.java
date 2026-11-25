package dev.hephaestus.glowcase.mixin.client;

import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Mixin(ClientMannequin.class)
public interface ClientMannequinAccessor {
	@Accessor("skinLookup")
	CompletableFuture<Optional<PlayerSkin>> glowcase$getSkinLookup();

	@Accessor("skinLookup")
	void glowcase$setSkinLookup(CompletableFuture<Optional<PlayerSkin>> skinLookup);

	@Invoker("setSkin")
	void glowcase$invokeSetSkin(PlayerSkin skin);
}
