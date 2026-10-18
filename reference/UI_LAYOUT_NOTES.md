# Original-style UI layout notes (0.3.0)

The attached reference screenshot is 1024×768 (4:3). `BattleScreen` now uses that as its logical coordinate system so controls and card wells line up with the reference rather than a 16:9 generic board.

- Left rail, screen-space top to bottom: View Help, Options, Main Menu, Surrender.
- The circular control on the sigil between the mage portraits advances the turn (the original Skip Turn control).
- Enemy and player summaries are horizontal strips to the right of the two portraits; five resource tabs sit to their right.
- The battlefield uses six lanes per side, arranged in two rows. The hand is a 5×4 grid at the lower right. All 20 prototype-deck cards are visible and tappable.
- Lower left contains a selected-card description panel and battle log. The Get Advice button opens the compact help panel.
- Long press (~0.48 seconds) on a hand card or occupied lane opens the detailed rule/live-effect panel. A help-panel tap is consumed to close it so it cannot also play a card.

## Scope caveat

The cropped menu/portrait/ornamental frame pixels come from the user-provided reference screenshot. Remaining elements (slots, resource text, cards, stats, log, and overlays) are native LibGDX drawing. This is a layout pass, not a claim that all of the original Flash screens, animations, save data, or card effects are already ported.
