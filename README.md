# ⚡ Interactive 2D Electricity Circuit Simulator

A premium, interactive, 2D grid-based **Electricity Circuit Simulator** built from scratch in Java Swing and AWT. It features a real-time Modified Nodal Analysis (MNA) physics engine, smart auto-routing wires, interactive measurement tools (Ammeters and Voltmeters), a full transaction-based Undo/Redo stack, and a zoomable, pannable large-grid workspace.

---

## 🚀 Key Features

* **Advanced MNA Physics Solver**: Computes real-time currents ($I$), node voltages ($V$), and individual component voltage drops ($V_{drop}$) using Modified Nodal Analysis and Gaussian elimination with partial pivoting. Accurately simulates parallel paths, ideal $0\Omega$ batteries, and short circuits.
* **Smart Auto-Routing Wires**: Wires automatically orient and switch geometries (`Straight`, `Corner`, `T-Junction`, `Cross`, and `Dotted`) depending on their neighbors, allowing seamless drag-drawing of complex paths.
* **Digital Measurement Tools**:
  * **Ammeter**: Measures current in series, displaying a real-time glowing green digital readout.
  * **Voltmeter**: Measures voltage drop in parallel with high $10\text{ M}\Omega$ internal resistance, displaying a real-time glowing yellow/orange digital readout.
* **Undo/Redo Actions**: A complete Command-Pattern history stack supporting all edits (placements, erases, rotations, values, toggling switches, and multi-wire drags) via `Ctrl+Z` / `Ctrl+Y`.
* **Zoom & Pan Viewport**: Pan around a large `50x50` grid using arrow keys or WASD, and zoom in/out centering on the cursor using the `+` / `-` keys.
* **Interactive Components**:
  * **Battery**: Set custom voltage and internal resistance (defaults to ideal $0.0\Omega$). Toggles texture to OFF state when inactive.
  * **Bulb**: Automatically lights up when current flows. Configurable resistance.
  * **Resistor**: Restricts current flow. Configurable resistance.
  * **Switch**: Interactive toggle (using `C` key or toolbar click) with active lever animations.

---

## 🕹️ Controls and Keyboard Shortcuts

| Action | Input | Description |
| :--- | :--- | :--- |
| **Select Tool** | Click Toolbar Buttons | Switch active tool (Wire, Battery, Bulb, Resistor, Switch, Ammeter, Voltmeter, Eraser). |
| **Place Component** | **Left-Click** (Empty Cell) | Places the active tool. |
| **Delete Component** | **Delete** / **Backspace** | Quickly erases the hovered component under the mouse cursor. |
| **Rotate Component** | **Right-Click** or Press **`R`** | Rotates the hovered component CW (or pre-rotates the active placement tool if hovering an empty cell). |
| **Toggle Switch / Cycle Wire** | Press **`C`** | Toggles a Switch state (Open/Closed) or cycles a wire type manually. |
| **Configure Parameters** | **Double-Click** | Configure Voltage / Resistance values via dialogue prompt. |
| **Drag Wire / Eraser** | **Click & Drag** | Drag-draws lines/rectangles of wires (green outline) or erases areas (red outline). |
| **Undo / Redo** | **`Ctrl + Z`** / **`Ctrl + Y`** | Reverts or reapplies layout actions in order. |
| **Grid Panning** | **Arrow Keys** or **WASD** | Pan the viewport around the large grid. |
| **Grid Zooming** | **`+` (Equals)** / **`-` (Minus)** | Zoom in and out of the grid viewport (from `0.5x` to `3.0x`). |
| **Manual Auto-Route** | Press **`F`** | Manually force-recalculate all wire alignments. |

---

## 📦 Project Structure

```
├── Sprites/
│   ├── Components/     # Sprites for Battery, Bulb, Resistor, Switch, Meters
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

Make sure you have Java JDK installed on your system. Open your terminal in the directory and run:

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
