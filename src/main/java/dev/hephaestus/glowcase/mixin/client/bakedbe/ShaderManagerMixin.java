package dev.hephaestus.glowcase.mixin.client.bakedbe;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.hephaestus.glowcase.util.LinedStringBuilder;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ShaderManager.class)
public class ShaderManagerMixin {
	@WrapOperation(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/resources/Resource;readAllAsString()Ljava/lang/String;"), method = "loadInclude")
	private static String modifyFogShader(Resource instance, Operation<String> original, @Local(argsOnly = true, name = "location") Identifier location) {
		var contents = original.call(instance);
		if (location.equals(Identifier.withDefaultNamespace("shaders/include/fog.glsl"))) {
			contents = glowcase$injectOffsetUniform(contents);
			contents = glowcase$injectOffsetMath("float fog_spherical_distance(vec3 pos) {", contents);
			contents = glowcase$injectOffsetMath("float fog_cylindrical_distance(vec3 pos) {", contents);
		}

		return contents;
	}

	@Unique
	private static String glowcase$injectOffsetUniform(String contents) {
		int index = contents.indexOf("layout(std140) uniform Fog {");

		var part1 = contents.substring(0, index);
		var part2 = contents.substring(index);

		return new LinedStringBuilder(part1)
			.appendLine("layout(std140) uniform GlowcaseFogOffset {")
			.appendLine("    int GlowcaseFogOffsetEnabled;")
			.appendLine("    ivec3 GlowcaseFogOffsetBlockPos;")
			.appendLine("    vec3 GlowcaseFogOffsetOffset;")
			.appendLine("};")
			.appendLine(part2)
			.toString();
	}

	@Unique
	private static String glowcase$injectOffsetMath(String search, String contents) {
		int index = contents.indexOf(search);
		index = contents.indexOf('\n', index);

		var part1 = contents.substring(0, index);
		var part2 = contents.substring(index);

		return new LinedStringBuilder(part1)
			.appendLine("    if (GlowcaseFogOffsetEnabled == 1) {")
			.appendLine("        pos += GlowcaseFogOffsetBlockPos + GlowcaseFogOffsetOffset;")
			.appendLine("    }")
			.appendLine(part2)
			.toString();
	}
}
