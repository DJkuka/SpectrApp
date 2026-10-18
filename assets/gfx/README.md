# Graphics assets

This directory contains 223 individual card-art crops and selected visual resources extracted from the SWF supplied by the user in this conversation:

- `cards/card_001.jpg` … `card_223.jpg`: crops from the original `BigData` atlas, in card-ID order as used by the exported `GParams.getBigData()` method.
- `frames/`: cleaned transparent card-frame templates, with dynamic title/cost/attack/health drawn by the native UI.
- `battle_bg.jpg`, `title_bg.jpg`: original game background/title artwork adapted for the native UI. The battle screen now uses a 4:3 logical layout to match the reference screenshot.
- `portraits/`: mage portrait art.
- `button.png`, `parchment.png`, `detail_panel.png`: UI textures.
- `ui/left_menu.png`, `ui/mage_top.png`, `ui/mage_sigil.png`, `ui/mage_bottom.png`: small source crops from the supplied original-interface screenshot for faithful menu/portrait framing.
- `ui/info_frame.png`, `ui/log_frame.png`, `ui/resource_frame.png`: transparent border overlays extracted from the supplied screenshot, with dynamic text/center area removed.

The graphic integration is a first native rendering pass. It does not reproduce the original Flash timeline animations, scene transitions or every original UI detail. The exported artwork is included here only as a resource for the user's requested personal port.
