# ⚡ Interactive 2D Electricity Circuit Simulator

A premium, interactive, 2D grid-based **Electricity Circuit Simulator** built from scratch in Java Swing and AWT. It features a real-time Modified Nodal Analysis (MNA) physics engine, transient RC/RL behavior, diode/LED bias convergence, AC motor simulation, smart auto-routing wires, a full transaction-based Undo/Redo stack, real-time graphing and an oscilloscope, circuit save/load, and a zoomable, pannable large-grid workspace — all wrapped in a custom dark-mode UI.

---

## 🚀 Key Features

* **Advanced MNA Physics Solver**: Computes real-time currents ($I$), node voltages ($V$), and individual component voltage drops ($V_{drop}$) using Modified Nodal Analysis and Gaussian elimination with partial pivoting. Accurately simulates parallel paths, ideal $0\Omega$ batteries, short circuits, and multiple independent circuits on the same grid.
* **Transient & Nonlinear Components**: Capacitors and inductors are modeled with a time-step companion model (charge/discharge behavior), and diodes/LEDs converge to the correct forward/reverse bias state through iterative resolving.
* **AC Simulation**: AC Motors generate a time-varying sine voltage (configurable peak voltage and frequency), solved and visualized in real time.
* **Solver Diagnostics**: Floating or singular nodes (e.g. a disconnected battery or an incomplete loop) are automatically detected and flagged — affected components are marked inactive with a visible warning, rather than silently reporting a misleading zero.
* **Smart Auto-Routing Wires**: Wires automatically orient and switch geometries (`Straight`, `Corner`, `T-Junction`, `Cross`, and `Dotted`) depending on their neighbors, allowing seamless drag-drawing of complex paths, plus a current-based heat-map tint on live wires.
* **Digital Measurement Tools**:
  * **Ammeter**: Measures current in series with a real-time digital readout.
  * **Voltmeter**: Measures voltage drop in parallel with high internal resistance, with a real-time digital readout.
* **Real-Time Graphing & Oscilloscope**: Plot any component's live voltage/current on a rolling graph, or open a multi-channel oscilloscope to monitor several AC sources, capacitors, inductors, and batteries at once.
* **Circuit Save/Load**: Save and load full circuit layouts, including component values, orientation, and wire routing, to a `.csim` file.
* **Undo/Redo Actions**: A complete Command-Pattern history stack supporting all edits (placements, erases, rotations, value changes, switch toggles, and multi-wire drags) via `Ctrl+Z` / `Ctrl+Y`.
* **Zoom & Pan Viewport**: Pan around a large `50x50` grid using arrow keys, WASD, or a middle-mouse drag, and zoom in/out centering on the cursor using the scroll wheel or the `+` / `-` keys.
* **Custom Dark UI**: A dedicated dark design system (not the default Swing look) spans the canvas, property editor, oscilloscope/graph panels, and menu chrome, with a consistent color language for interaction state, measurement data, and solver warnings.

---

## 🔌 Components

| Component | Description |
| :--- | :--- |
| **Battery** | Configurable voltage and internal resistance (defaults to ideal $0\Omega$). |
| **Resistor** | Restricts current flow via a configurable resistance value. |
| **Bulb** | Lights up when current flows; configurable resistance. |
| **Switch** | Interactive open/closed toggle. |
| **Fuse** | Blows and breaks the circuit above a configurable current limit. |
| **Capacitor** | Charges/discharges over time using a configurable capacitance. |
| **Inductor** | Resists changes in current using a configurable inductance. |
| **Diode** | Conducts only in forward bias; reverse-blocks otherwise. |
| **LED** | A light-emitting diode variant that lights when conducting. |
| **AC Motor** | An AC voltage source with configurable peak voltage and frequency. |
| **Ammeter** | In-series current meter with a live digital readout. |
| **Voltmeter** | In-parallel voltage meter with a live digital readout. |
| **Wire** | Auto-routing conductor with current-based heat-map coloring. |
| **Text Label** | Freeform annotation placed on the grid. |

---

## 🕹️ Controls and Keyboard Shortcuts

| Action | Input | Description |
| :--- | :--- | :--- |
| **Select Tool** | Click Sidebar Buttons | Switch the active tool from the **Place Component** section. |
| **Place Component** | **Left-Click** (Empty Cell) | Places the active tool. |
| **Delete Component** | **Delete** / **Backspace** | Erases the hovered component. |
| **Rotate Component** | **R** | Rotates the hovered component (or the active placement tool) clockwise. |
| **Toggle Switch / Cycle Wire** | **C** | Toggles a Switch state (Open/Closed) or cycles a wire's geometry. |
| **Configure Parameters** | **Double-Click** | Opens the property editor for the hovered component, showing a live simulation readout alongside its editable values. |
| **Open Graph** | **Alt+Click**, or hover + **G** | Opens a real-time voltage/current graph for the component. |
| **Toggle Oscilloscope** | **Ctrl+Shift+O** | Opens the multi-channel oscilloscope. |
| **Drag Wire / Eraser** | **Click & Drag** | Drag-draws wires or erases an area. |
| **Undo / Redo** | **Ctrl+Z** / **Ctrl+Y** | Reverts or reapplies edits in order. |
| **New / Open / Save / Save As** | **Ctrl+N** / **Ctrl+O** / **Ctrl+S** / **Ctrl+Shift+S** | Manage `.csim` circuit files. |
| **Grid Panning** | Arrow Keys, WASD, or Middle-Drag | Pan the viewport around the grid. |
| **Grid Zooming** | Scroll Wheel or **+** / **-** | Zoom in and out of the grid viewport. |
| **Reset View** | **Home** | Resets zoom and pan. |
| **Manual Auto-Route** | **F** | Force-recalculates all wire alignments. |

A full, always-up-to-date shortcut reference is also available in-app via **Help → Keyboard Shortcuts**.

---

## 🎨 Interface

The UI uses a dedicated dark design system rather than the platform default:

* **Canvas**: dark blue-gray workspace with a minor/major grid hierarchy, a compact zoom-status chip, and a current-flow legend (cyan → coral).
* **Interaction states**: mint highlights for placement/wire actions, coral for the eraser.
* **Solver warnings**: floating or singular nodes are marked with an amber outline, a `!` badge on the affected component, and an on-screen warning banner — kept visually distinct from ordinary current coloring.
* **Property editor, sidebar, graph, and oscilloscope panels** all share the same palette, typography, and spacing system, with native OS title bars on dialogs.

---

## 📦 Project Structure

```text
├── Sprites/
│   ├── Components/     # Sprites for Battery, Bulb, Resistor, Switch, Meters, etc.
│   └── Wires/          # Sprites for Straight, Diagonal, T-Junction, Cross, Dotted
├── src/main/java/electricity/
│   ├── model/          # Components and directional model types
│   ├── solver/         # MNA solver and shared grid dimensions
│   ├── command/        # Undo/redo command types
│   ├── io/             # .csim serialization
│   ├── ui/             # Swing window, canvas, renderer, input, dialogs
│   └── assets/         # Sprite loading
├── src/test/java/      # JUnit 5 solver tests
├── pom.xml             # Maven build and test configuration
└── run.bat             # Build and launch helper
```

---

## ⚙️ How to Build and Run

Make sure you have a Java JDK and Maven installed on your system. Open your terminal in the project directory and run:

1. **Run the JUnit test suite**:

   ```bash
   mvn test
   ```

2. **Run the Simulator**:

   ```bash
   run.bat
   ```

---

## 📜 License

This project is licensed under the MIT License - feel free to use it for personal or educational purposes!
