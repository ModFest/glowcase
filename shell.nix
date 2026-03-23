let
  nixpkgsVer = "812b3986fd1568f7a858f97fcf425ad996ba7d25";
  pkgs = import (fetchTarball "https://github.com/NixOS/nixpkgs/archive/${nixpkgsVer}.tar.gz") { config = {}; overlays = []; };
  libs = with pkgs; [
    libpulseaudio
    libGL
    glfw
    openal
    stdenv.cc.cc.lib
  ];
in pkgs.mkShell {
  name = "glowcase";

  buildInputs = with pkgs; [
    jdk25 # Maybe change this to JBR 25 once it's in the nixpkgs upstream
  ] ++ libs;

  LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath libs;
}
