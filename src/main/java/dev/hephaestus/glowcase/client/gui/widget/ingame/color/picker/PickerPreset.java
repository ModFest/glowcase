package dev.hephaestus.glowcase.client.gui.widget.ingame.color.picker;

import net.minecraft.ChatFormatting;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PickerPreset extends PickerArea {
	public static final ChatFormatting[] FORMATTING_PRESETS = new ChatFormatting[]{
		ChatFormatting.DARK_RED, ChatFormatting.RED, ChatFormatting.GOLD, ChatFormatting.YELLOW,
		ChatFormatting.GREEN, ChatFormatting.DARK_GREEN, ChatFormatting.AQUA, ChatFormatting.DARK_AQUA,
		ChatFormatting.BLUE, ChatFormatting.DARK_BLUE, ChatFormatting.LIGHT_PURPLE, ChatFormatting.DARK_PURPLE,
		ChatFormatting.WHITE, ChatFormatting.GRAY, ChatFormatting.DARK_GRAY, ChatFormatting.BLACK
	};

	public static final Integer[] ALPHA_PRESETS = new Integer[]{
		0x00000000, 0x40000000, 0x55000000, 0x77000000, 0x99000000, 0xAA000000, 0xCC000000, 0xFF000000
	};

	public static List<PickerPreset> createFormattingPresets(ColorPickerWidget colorPicker) {
		List<PickerPreset> presets = new ArrayList<>();
		for (ChatFormatting formattingPreset : FORMATTING_PRESETS) {
			//noinspection DataFlowIssue - We can assume that the presets in this array have their color ints
			presets.add(new PickerPreset(colorPicker, ARGB.color(1f, formattingPreset.getColor()), formattingPreset, false));
		}
		return presets;
	}

	public static List<PickerPreset> createAlphaPresets(ColorPickerWidget colorPicker) {
		List<PickerPreset> presets = new ArrayList<>();
		for (Integer alphaColor : ALPHA_PRESETS) {
			presets.add(new PickerPreset(colorPicker, alphaColor, null, true));
		}
		return presets;
	}

	private final int presetColor;
	@Nullable
	private final ChatFormatting presetFormatting;
	private final boolean isAlphaPreset;

	public PickerPreset(ColorPickerWidget colorPicker, int presetColor, @Nullable ChatFormatting presetFormatting, boolean isAlphaPreset) {
		super(colorPicker, true, null, null);
		this.presetColor = presetColor;
		this.presetFormatting = presetFormatting;
		this.isAlphaPreset = isAlphaPreset;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY) {
		if(super.mouseClicked(mouseX, mouseY)) {
			this.getColorPicker().onPresetClick(this);
			return true;
		}
		return false;
	}

	public int getPresetColor() {
		if (this.presetFormatting != null && this.presetFormatting.isColor() && this.presetFormatting.getColor() != null) {
			// Needed otherwise it's transparent for some reason
			return ARGB.color(1f, this.presetFormatting.getColor());
		}
		return this.presetColor;
	}

	public @Nullable ChatFormatting getPresetFormatting() {
		return presetFormatting;
	}

	public boolean isAlphaPreset() {
		return isAlphaPreset;
	}

	public float getAlpha() {
		return ARGB.alphaFloat(this.presetColor);
	}
}
