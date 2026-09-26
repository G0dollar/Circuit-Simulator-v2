# ⚡ Circuit Simulator — Feature Additions & Enhancements

This document summarizes all major features, UI improvements, and architectural upgrades implemented in the Electricity Simulator application after the addition of AC circuits and AC Motor simulation.

---

## 📋 Overview of New Features

| Feature | Key Functionality | Shortcut / Access |
|---|---|---|
| **💾 Save & Load Circuits** | Save and load complete circuit configurations using a lightweight, pure-Java JSON format (`.csim`). | `Ctrl+S`, `Ctrl+O`, `Ctrl+N` |
| **🔍 Smooth Zoom & Pan** | Dynamic canvas scaling centered on cursor and middle-click drag panning. | `Scroll Wheel`, `Middle Drag`, `Home` |
| **🎨 Current-Based Wire Tinting** | Pixel-accurate heat-map coloring overlaying only the wire conductor sprites based on current flow. | Automatic |
| **📉 Real-Time Signal Oscilloscope** | Dark-themed, multi-channel oscilloscope monitoring AC signals, node voltages, and battery levels. | `Ctrl+Shift+O` |
| **📊 Real-Time V-I Graphing** | Dual-axis rolling plot for voltage and current of any component. | `Alt+Click`, `G` key |
| **🛠️ Component Property Editor** | Dark-mode modal dialog for editing component values with real-time input validation. | `Double-Click` |
| **⌨️ Interactive Help & Shortcuts** | Built-in reference table detailing all canvas navigation and component manipulation hotkeys. | **Help → Keyboard Shortcuts** |

---

## 🛠️ Feature Breakdown

### 1. Save & Load Circuits (`.csim` JSON Format)
- **Zero-Dependency JSON**: Custom, lightweight serializer/parser implemented in [`CircuitIO.java`](file:///c:/Users/jakat/All%20Codes/Combined%20Projects/Electricity%20Sim/CircuitIO.java) without requiring external JAR files.
- **Full State Preservation**: Captures grid positions, orientations, component parameter values (e.g., resistance, voltage, capacitance, inductance, frequency), fuse status, text labels, and wire routing geometries.
- **File Menu Integration**:
  - **New Circuit** (`Ctrl+N`): Prompts confirmation before clearing the active grid.
  - **Open Circuit** (`Ctrl+O`): File chooser filtered to `.csim` files.
  - **Save** (`Ctrl+S`) / **Save As** (`Ctrl+Shift+S`): Quick-saves to active file or prompts destination.

---

### 2. Interactive Zoom and Pan
- **Cursor-Centered Zoom**: Scaling (25% to 400%) adjusts view offsets dynamically so the point under the mouse cursor remains fixed.
- **Middle-Mouse Drag**: Smoothly translates the viewport across large circuits.
- **Keyboard Navigation**:
  - `Arrow Keys` or `WASD` for step panning.
  - `+` / `-` keys for incremental zooming.
  - `Home` key resets zoom to 100% and centers view.
- **HUD Indicator**: Displays current zoom percentage (e.g. `🔍 100%`) in top-left screen overlay.

---

### 3. Current-Based Heat-Map Wire Tinting
- **Pixel-Accurate Composition**: Uses `AlphaComposite.SrcAtop` blending to apply tinting directly over the sprite graphics without drawing colored background bounding boxes.
- **Thermal Heat-Map Palette**:
  - 🔵 **Cyan / Blue**: Low current flow ($< 0.30\text{ A}$)
  - 🟡 **Amber / Yellow**: Medium current flow ($0.30\text{ A} - 0.65\text{ A}$)
  - 🔴 **Red**: High current flow ($> 0.65\text{ A}$)
- **On-Screen Legend**: Displays a dynamic current scale overlay in the bottom-right corner when active current flow is detected.

---

### 4. Real-Time Signal Oscilloscope
- **AC & Transient Waveform Visualizer**: Non-modal dialog ([`OscilloscopeDialog.java`](file:///c:/Users/jakat/All%20Codes/Combined%20Projects/Electricity%20Sim/OscilloscopeDialog.java)) hosting a CRT/phosphor-style scope ([`OscilloscopePanel.java`](file:///c:/Users/jakat/All%20Codes/Combined%20Projects/Electricity%20Sim/OscilloscopePanel.java)).
- **Multi-Channel Auto-Discovery**: Automatically tracks all AC Motor sources, capacitors, inductors, and batteries present in the circuit.
- **Scope Controls**:
  - **Voltage Scale**: Adjustable sensitivity ($0.5\text{V}$ to $100\text{V}$ per division).
  - **Freeze Toggle**: Pauses real-time sampling to analyze instantaneous waveform values.

---

### 5. Component Real-Time V-I Graphing
- **Dual-Axis Time Series Plot**: Displays voltage (blue) and current (amber) on rolling 300-sample buffers ([`GraphPanel.java`](file:///c:/Users/jakat/All%20Codes/Combined%20Projects/Electricity%20Sim/GraphPanel.java)).
- **Independent Y-Axis Auto-Scaling**: Handles millivolts/microamps up to high-power ranges dynamically.
- **Quick Shortcuts**:
  - **`Alt + Left Click`** on any component directly opens its telemetry graph.
  - **`G` key** opens the graph for the currently hovered component cell.
  - Accessible via the **"📈 Show Graph"** button in the property editor.

---

### 6. Dynamic Component Property Editor
- **Modern Dark-Themed Dialog**: Replaces basic option panes with a custom property configuration modal ([`PropertyEditorDialog.java`](file:///c:/Users/jakat/All%20Codes/Combined%20Projects/Electricity%20Sim/PropertyEditorDialog.java)).
- **Inline Validation**: Highlights invalid inputs with red borders and error messages prior to submission.
- **Editable Parameters**:
  - **Batteries**: DC Voltage ($V$) and Internal Resistance ($r$).
  - **Resistors / Bulbs**: Resistance ($\Omega$).
  - **Capacitors**: Capacitance ($\mu\text{F}$).
  - **Inductors**: Inductance ($H$).
  - **AC Sources / Motors**: Peak Voltage ($V$) and Frequency ($\text{Hz}$).
  - **Fuses**: Current Rating ($A$) and Blown/Tripped status toggle.
  - **Text Labels**: Custom display text strings.

---

## ⌨️ Shortcut Summary Reference

```
┌─────────────────────────────┬────────────────────────────────────────────────────────┐
│ Shortcut                    │ Action                                                 │
├─────────────────────────────┼────────────────────────────────────────────────────────┤
│ Scroll Wheel                │ Zoom canvas in / out (centered on mouse cursor)        │
│ Middle Mouse Drag           │ Pan canvas viewport                                    │
│ Home                        │ Reset zoom (100%) and center canvas view               │
│ Ctrl + S / Ctrl + Shift + S │ Save / Save As circuit file (.csim)                    │
│ Ctrl + O / Ctrl + N         │ Open circuit file / New blank circuit                  │
│ Ctrl + Shift + O            │ Toggle Oscilloscope window                             │
│ Double-Click                │ Open Component Property Editor                         │
│ Alt + Click / G Key         │ Open Real-Time V-I Graph for targeted component        │
│ R Key                       │ Rotate component orientation clockwise                 │
│ C Key                       │ Cycle wire geometry / Toggle switch state              │
│ Delete / Backspace          │ Erase hovered component                                │
│ F Key                       │ Auto-route and optimize wire connections               │
└─────────────────────────────┴────────────────────────────────────────────────────────┘
```
