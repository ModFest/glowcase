let
	pkgs = import <nixpkgs> { config = {}; overlays = []; };
	libs = with pkgs; [
		libpulseaudio
		libGL
		glfw
		vulkan-loader
		openal
		renderdoc
		pciutils # For sodium
		flite # Narrator
		stdenv.cc.cc.lib
	];
in pkgs.mkShell {
	name = "glowcase";

	buildInputs = with pkgs; [
		(pkgs.jetbrains.jdk.override { withJcef = false; })
	] ++ libs;

	LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath libs;
}
