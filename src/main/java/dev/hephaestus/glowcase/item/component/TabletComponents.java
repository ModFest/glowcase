package dev.hephaestus.glowcase.item.component;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;

public class TabletComponents {

	// TODO: Rewrite to use RecordCodecBuilder rather than three components
	// (see NoteComponent.java for an example)

	public static final DataComponentType<Pair<UUID, BlockPos>> LINKED_SCREEN_TYPE;
	public static final DataComponentType<Integer> CURRENT_SLIDE_TYPE;
	public static final DataComponentType<List<Pair<String,String>>> SLIDESHOW_COMPONENT_TYPE;

	static {
		{
			Codec<UUID> uuidCodec = Codec.INT_STREAM.comapFlatMap(stream -> Util.fixedSize(stream, 4).map(UUIDUtil::uuidFromIntArray), uuid -> Arrays.stream(UUIDUtil.uuidToIntArray(uuid)));
			Codec<Pair<UUID, BlockPos>> codec = Codec.mapPair(uuidCodec.fieldOf("uuid"), BlockPos.CODEC.fieldOf("pos")).codec();
			LINKED_SCREEN_TYPE = DataComponentType.<Pair<UUID, BlockPos>>builder()
				.persistent(codec)
				.networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(codec))
				.build();
		}

		{
			CURRENT_SLIDE_TYPE = DataComponentType.<Integer>builder()
				.persistent(ExtraCodecs.NON_NEGATIVE_INT)
				.networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(ExtraCodecs.NON_NEGATIVE_INT))
				.build();
		}

		{
			Codec<List<Pair<String, String>>> codec = Codec.mapPair(Codec.STRING.fieldOf("url"), Codec.STRING.fieldOf("alt")).codec().listOf();
			SLIDESHOW_COMPONENT_TYPE = DataComponentType.<List<Pair<String,String>>>builder()
				.persistent(codec)
				.networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(codec))
				.build();
		}
	}
}
