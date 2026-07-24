# Changelog

## 1.0.0

First release.

- Client-side overlay for **Simple Voice Chat** showing who is currently speaking near you:
  player face, name and a live speaking indicator that reacts to voice loudness (sine wave / bars / dot).
- Handles proximity, locational and static (group) audio.
- Fully draggable overlay — place it anywhere on screen, align left/right, list grows up/down.
- Professional configurator with tabs (Position, Appearance, Colors, Behavior), HSV + hex color pickers
  and a full-screen live preview with sample speakers (tune it solo, no second player needed).
- The settings panel automatically sits on the opposite side of the screen from the overlay, so it
  never hides the element you are positioning.
- Face size adjustable down to 4 px for a very compact overlay.
- Many options: name scale/width, background & border, corner radius, padding, distance display,
  max entries, timeouts, fade in/out, hold, slide-in animation, loudness sensitivity, show self,
  whisper distinction, sort order, and visibility rules (in-game only, hide with HUD/chat).
- Sensible defaults: minimal look (face size 8, no background/border, zero entry spacing).
- Keybinds (open configurator, toggle overlay) and Mod Menu integration.
- English and Polish translations.
- Only real players (from the tab list) are shown — audio routed through plugin/add-on channels
  (radios, music, TTS, non-player entities) no longer appears as "ghost" entries with random names.
