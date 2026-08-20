{ pkgs, ... }:

{
  languages = {
    javascript.enable = true;
    python.enable = true;
  };
  packages = with pkgs; [ gnumake python3Packages.crc32c ];
  pre-commit.hooks.prettier.enable = true;
}
