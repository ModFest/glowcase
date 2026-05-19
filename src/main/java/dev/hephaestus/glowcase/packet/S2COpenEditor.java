package dev.hephaestus.glowcase.packet;

import com.mojang.logging.LogUtils;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.GlowcaseBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

/**
 * @author Ampflower
 **/
public record S2COpenEditor(ResourceKey<Level> dimension, BlockPos pos) implements CustomPacketPayload {
	private static final Logger logger = LogUtils.getLogger();
/*
	private static final MethodHandle yeahIGotLevels;

	static {
		MethodHandle $yeahIGotLevels = MethodHandles.dropArguments(MethodHandles.throwException(Level.class, AssertionError.class), 0, Object.class);
		try {
			final Class<?> minecraft = Class.forName("net.minecraft.client.Minecraft");

			$yeahIGotLevels = MethodHandles.lookup().unreflectGetter(minecraft.getField("level"));
		} catch (Throwable t) {
			System.err.println(":3");
			t.printStackTrace();
		}
		yeahIGotLevels = $yeahIGotLevels;
	}*/

	public static final Type<S2COpenEditor> ID = new Type<>(Glowcase.id("open_editor"));
	public static final StreamCodec<RegistryFriendlyByteBuf, S2COpenEditor> PACKET_CODEC = StreamCodec.composite(
		ResourceKey.streamCodec(Registries.DIMENSION), S2COpenEditor::dimension,
		BlockPos.STREAM_CODEC, S2COpenEditor::pos,
		S2COpenEditor::new
	);

	public S2COpenEditor(BlockEntity entity) {
		this(entity.getLevel().dimension(), entity.getBlockPos());
	}

	@Override
	public Type<S2COpenEditor> type() {
		return ID;
	}

	@Environment(EnvType.CLIENT)
	public void receive(ClientPlayNetworking.Context context) {
		final Minecraft client = context.client();
		final Level level = client.level;
		// A *lil* difficult if the dimension is mismatched.
		if (level == null || !this.dimension().equals(level.dimension())) {
			context.responseSender().sendPacket(new C2SUnlockEditor(this.dimension(), this.pos()));
			return;
		}
		final BlockState state = client.level.getBlockState(this.pos());
		final Block block = state.getBlock();

		if (!(block instanceof GlowcaseBlock glowcaseBlock)) {
			final Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
			logger.warn("Ignoring unknown block {} => {}", this.pos(), id);
			return;
		}

		glowcaseBlock.openEditScreen(pos());
	}
}
