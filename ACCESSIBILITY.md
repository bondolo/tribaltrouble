# Accessibility

## Commitment

Tribal Trouble is committed to providing an inclusive and accessible real-time strategy experience for all players, contributors, and maintainers. We believe gaming and open-source collaboration should be welcoming to individuals of diverse abilities, and we actively work to identify and eliminate barriers across our user interface, rendering pipeline, controls, and documentation.

Where applicable to our graphical user interface and documentation, our accessibility goals are informed by the [Web Content Accessibility Guidelines (WCAG) 2.2 Level AA](https://www.w3.org/WAI/standards-guidelines/wcag/) principles: Perceivable, Operable, Understandable, and Robust.

---

## Supported Environments & Accessibility Features

Tribal Trouble runs across modern desktop operating systems (macOS, Linux, and Windows) using an OpenGL 4.1 Core Profile engine. The game provides a dedicated **Accessibility** options panel alongside modular graphics, sound, and control configurations.

### Visual & Contrast Customization
* **High-Contrast Mode**: A toggleable high-contrast profile designed to maximize edge definition and form readability across in-game menus, dialogs, and HUD overlays.
* **Granular Contrast Controls**: Sliders allowing players to independently tune:
  * *Contrast Intensity* (up to 2.0x enhancement)
  * *Brightness Offset* (shadow and highlight compensation)
  * *Clarity* (mid-tone sharpening for element legibility)
* **Color Inversion**: Optional full-frame color inversion to aid players with specific light sensitivities or visual impairments.

### Color Vision Deficiency (CVD) Support
* **Real-Time CVD Correction Filters**: Dedicated shader-driven correction modes tailored for:
  * **Protanopia** (red-weak/blind)
  * **Deuteranopia** (green-weak/blind)
  * **Tritanopia** (blue-weak/blind)
* **Adjustable Filter Intensity**: Fine-tune correction intensity to match individual vision profiles.
* **Custom Team & Player Colors**: Fully customizable player and team color palettes, ensuring players can choose colors that are easily distinguished against the natural terrain and water.
* **Team Stencils**: Distinct silhouette patterns and stencils rendered on units and buildings, ensuring allegiance and unit type do not rely on color perception alone.

### Interface Scaling & Layout
* **UI Scaling**: Configurable interface scaling (`ui_scale`) accommodating high-resolution (HiDPI / Retina) displays and players requiring larger text, buttons, and status icons.
* **Dynamic Layout**: Scalable dialogs and responsive component placement to prevent text clipping and overlapping when larger scales or fonts are applied.

### Controls & Input Flexibility
* **Fully Rebindable Keyboard Controls**: Comprehensive key-binding menu allowing reassignment of camera movement, unit commands, construction menus, selection shortcuts, and chat toggles.
* **Alternative Control Schemes**: Ongoing development of controller input schemes and camera easing to support one-handed, keyboard-only, or alternative input devices.

### Audio Accompaniments & Feedback
* **Sound Emojis & Audio Cues**: Distinct auditory signals accompanying chat messages, unit alerts, and gameplay events to provide non-visual feedback.
* **Independent Volume Channels**: Separate control over Master, Sound Effects, and Music audio buses to prevent sensory overload and ensure gameplay audio cues remain clear.

---

## Known Barriers

As a classic 3D real-time strategy game modernized on a custom hardware-accelerated OpenGL engine, several accessibility barriers currently exist:

1. **Native Assistive Technology Integration**:
   * Menus, HUD elements, and chat forms are rendered on a custom OpenGL canvas and do not currently expose semantic accessibility trees to platform screen readers (such as Apple VoiceOver, Windows Narrator, NVDA, or Orca).
2. **Dynamic 3D Battlefield Narration**:
   * Fast-paced real-time tactical combat (unit pathfinding, projectile trajectories, landscape alterations) currently lacks automated text-to-speech or descriptive audio narration.
3. **Screen Magnifier Cursor Tracking**:
   * Hardware mouse capture during tactical edge-scrolling may require customization when used in conjunction with third-party full-screen magnifier software.

---

## CI Guardrails & Definition of Done

To ensure new features and UI improvements do not introduce accessibility regressions, all pull requests and code modifications must adhere to the following standards:

### Definition of Done for UI Changes
* **Keyboard Operability**: All new menus, buttons, forms, and dialog boxes must be fully reachable and operable via keyboard navigation with visible focus indicators.
* **Color Independence**: Visual cues, status indicators, and gameplay requirements must not rely on color alone. Shape, iconography, text labels, or stencil patterns must accompany color distinctions.
* **Tooltips and Labels**: All graphical buttons and icon spinners must include descriptive text labels or localized tooltips.
* **UI Scale Resilience**: Components must support scaling without truncating critical text or misaligning clickable hitboxes.
* **Contrast Compliance**: Text and primary interactive affordances must maintain adequate contrast against their immediate backgrounds under default and high-contrast modes.

---

## Contributor Expectations

Contributors are encouraged to keep accessibility at the forefront of development:
* **Familiarize with Existing Options**: Review [`AccessibilityPanel.java`](content/src/main/java/com/oddlabs/tt/content/form/AccessibilityPanel.java) and [`AccessibilitySettings.java`](engine/src/main/java/com/oddlabs/tt/engine/settings/AccessibilitySettings.java) before implementing new UI or visual features.
* **Avoid Magic Colors**: Use semantic constants and respect user-configured `player_colours` and contrast settings rather than hardcoded RGBA values.
* **Test Multiple Modalities**: Verify changes using both mouse and keyboard navigation, and test with high-contrast and CVD filters enabled.

---

## Reporting Accessibility Issues

We actively welcome feedback and bug reports from players and contributors who experience barriers while playing or developing Tribal Trouble.

If you encounter an accessibility barrier or would like to request an accessibility enhancement:

1. **Open an Issue**: Visit our [GitHub Issue Tracker](https://github.com/bondolo/tribaltrouble/issues).
2. **Apply the Label**: Tag the issue with the `accessibility` label (or mention "Accessibility" in the title).
3. **Include Details**:
   * Description of the barrier encountered.
   * Component, menu, or gameplay phase affected.
   * Operating system, screen resolution, and display scaling settings.
   * Any assistive technologies, input devices, or accessibility options in use (e.g., High-Contrast mode, CVD filter, keyboard-only navigation).
   * Proposed solution or behavior that would make the feature accessible to you.

---

## Ownership & Maintenance

This accessibility policy and roadmap are maintained by the Tribal Trouble open-source contributors and project maintainers. We embrace the principle of **"Nothing about us without us"** and strive to continually evolve the game's accessibility through direct community collaboration.
