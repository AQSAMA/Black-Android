# BLACK mobile: gameplay-first settings and HUD

User rejected the previous card-heavy, portrait-oriented Control Center. This is a structural replacement, not a recolour.

## Sources inspected, 2026-10-31
- Official PUBG Mobile control customization: https://pubgmobile.helpshift.com/hc/en/3-pubg-mobile/faq/280-i-want-to-change-the-location-of-the-control-key/ — supports movable controls, dedicated Customize action and explicit save. Firecrawl markdown/branding/screenshot captured and screenshot pixels inspected.
- Google Play editorial: https://play.google.com/store/apps/editorial?id=mc_editorial_evergreen_post_install_pubg_mobile_improve_your_controls_gamehub_fcp — fully customizable placements and draggable controls/joystick.
- Historical PUBG HUD screenshot: https://tcolar.github.io/screenshots/controls.PNG — inspected pixels. Landscape play canvas, small floating settings panel, Exit/Reset/Save on top; selected control has size/transparency sliders. Historical visual reference, not proof of current PUBG layout.
- Helpshift site's extracted colours (#FCFCFC background, #0D0D0E text, customHSFont) belong to support documentation, NOT the game's HUD. Do not transplant the support-site template into a shooter.

## Proposed implementation (our choices, not extracted PUBG tokens)
- Landscape-first, slim 152–184dp left navigation, restrained slate surfaces, white text, one amber action colour. Compact Material 3 Compose widgets already in the project; no extra UI framework.
- Main destinations: Play, Controls, Graphics, Audio, More. Controls is the first settings page and prioritizes Camera speed and Customize HUD. Advanced keyboard/cheats/export options remain behind a separate Advanced destination instead of crowding normal play.
- One visible fire target by default. Optional extra fire targets require explicit enablement. Left half belongs to movement; right half to look. Deadzone never creates camera/mouse input. No per-event rounding loss.
- HUD editor uses the full landscape canvas; a small floating inspector exposes selected button size, opacity, hide; concise top toolbar Save/Undo/Reset/Exit. Editing can open directly on the actual game view via the native overlay.
- 48dp touch actions, 16/20/24dp spacing, short labels and minimal descriptive prose. No giant page headers or stacks of nested cards.
- Landscape orientation enforced for game/settings/preview. No web assets copied, no PUBG branding embedded.

## Gaps
No screenshots of this app running are available while Agent device access is disabled. Static screenshot references establish design intent, not runtime validation. Input correctness is checked by focused native tests and persisted-layout tests.

## Implemented 1.8 direction
The catalog search returned metallic/skewomorphic templates that conflict with the user's requested simplicity. The implementation uses the existing ready-made Android Material 3 components with a new flat layout rather than importing React or a heavy framework. BattleSettings.kt is the main shell; old keyboard/controller tools are progressively disclosed under More. Layout studio now fills the landscape screen with a collapsible inspector; it no longer allocates half the display to a scrolling settings form.
