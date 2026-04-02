let
 	nixpkgsVer = "812b3986fd1568f7a858f97fcf425ad996ba7d25";
 	tmpnixpkgsVer = "cebab056b44e0fc3885c392604d16fce2df9af68";
	pkgs = import (fetchTarball "https://github.com/NixOS/nixpkgs/archive/${nixpkgsVer}.tar.gz") { config = {}; overlays = []; };
	tmppkgs = import (fetchTarball "https://github.com/Awakened-Redstone/nixpkgs/archive/${tmpnixpkgsVer}.tar.gz") { config = {}; overlays = []; };
	libs = with pkgs; [
		libpulseaudio
		libGL
		glfw
		openal
		renderdoc
		pciutils # For sodium
		flite # Narrator
		stdenv.cc.cc.lib
	];
in pkgs.mkShell {
	name = "glowcase";

	buildInputs = with pkgs; [
		(tmppkgs.jetbrains.jdk.override { withJcef = false; })
	] ++ libs;

	LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath libs;
}
