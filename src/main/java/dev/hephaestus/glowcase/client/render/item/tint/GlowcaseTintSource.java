package dev.hephaestus.glowcase.client.render.item.tint;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record GlowcaseTintSource(int defaultColor) implements ItemTintSource {
	public static final MapCodec<GlowcaseTintSource> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(ExtraCodecs.RGB_COLOR_CODEC.fieldOf("default").forGetter(GlowcaseTintSource::defaultColor)).apply(instance, GlowcaseTintSource::new)
	);

	@Override
	public int calculate(ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity user) {
		CustomData component = stack.get(DataComponents.BLOCK_ENTITY_DATA);
		if (component == null) return defaultColor;

		CompoundTag nbt = component.copyTag();
		int color = nbt.getIntOr("color", 0);
		if (color != 0 && color != defaultColor) return color;
		return 0xFFAA00AA;
	}

	@Override
	public MapCodec<GlowcaseTintSource> type() {
		return CODEC;
	}
}
