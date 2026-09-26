package electricity.io;

import electricity.model.*;
import java.io.*;
import java.util.*;

/**
 * Handles saving and loading circuit files in a JSON-based format (.csim).
 * Uses a hand-rolled JSON writer/parser (no external libraries needed).
 */
public class CircuitIO {

    // -------------------------------------------------------------------------
    // SAVE
    // -------------------------------------------------------------------------

    public static void saveToFile(Component[][] grid, File file) throws IOException {
        int height = grid.length;
        int width = grid[0].length;

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"version\": 1,\n");
        sb.append("  \"gridWidth\": ").append(width).append(",\n");
        sb.append("  \"gridHeight\": ").append(height).append(",\n");
        sb.append("  \"components\": [\n");

        boolean first = true;
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                Component comp = grid[r][c];
                if (comp == null) continue;

                if (!first) sb.append(",\n");
                first = false;

                sb.append("    ").append(serializeComponent(comp));
            }
        }

        sb.append("\n  ]\n}\n");

        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)))) {
            writer.print(sb.toString());
        }
    }

    private static String serializeComponent(Component comp) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        sb.append("\"type\": \"").append(comp.getClass().getSimpleName()).append("\"");
        sb.append(", \"col\": ").append(comp.getCol());
        sb.append(", \"row\": ").append(comp.getRow());
        sb.append(", \"direction\": \"").append(comp.getDirection().name()).append("\"");

        if (comp instanceof Battery) {
            Battery b = (Battery) comp;
            sb.append(", \"voltage\": ").append(b.getVoltage());
            sb.append(", \"internalResistance\": ").append(b.getInternalResistance());
        } else if (comp instanceof Resistor) {
            sb.append(", \"resistance\": ").append(((Resistor) comp).getResistance());
        } else if (comp instanceof Bulb) {
            sb.append(", \"resistance\": ").append(((Bulb) comp).getResistance());
        } else if (comp instanceof Wire) {
            sb.append(", \"wireType\": \"").append(((Wire) comp).getType().name()).append("\"");
        } else if (comp instanceof Switch) {
            sb.append(", \"open\": ").append(((Switch) comp).isOpen());
        } else if (comp instanceof Fuse) {
            Fuse f = (Fuse) comp;
            sb.append(", \"limit\": ").append(f.getLimit());
            sb.append(", \"blown\": ").append(f.isBlown());
        } else if (comp instanceof TextLabel) {
            String text = ((TextLabel) comp).getText()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n");
            sb.append(", \"text\": \"").append(text).append("\"");
        } else if (comp instanceof Capacitor) {
            sb.append(", \"capacitance\": ").append(((Capacitor) comp).getCapacitance());
        } else if (comp instanceof Inductor) {
            sb.append(", \"inductance\": ").append(((Inductor) comp).getInductance());
        } else if (comp instanceof ACMotor) {
            ACMotor ac = (ACMotor) comp;
            sb.append(", \"peakVoltage\": ").append(ac.getPeakVoltage());
            sb.append(", \"frequency\": ").append(ac.getFrequency());
        }
        // LED, Diode, Ammeter, Voltmeter have no additional serialized fields

        sb.append(" }");
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // LOAD
    // -------------------------------------------------------------------------

    public static Component[][] loadFromFile(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }

        String json = sb.toString();

        // Check version key
        String versionStr = extractStringValue(json, "version");
        // We accept version 1 (versionStr might be "1" as a number token)

        // Parse grid dimensions (optional, default to 50x50)
        int gridW = 50;
        int gridH = 50;
        String gwStr = extractStringValue(json, "gridWidth");
        String ghStr = extractStringValue(json, "gridHeight");
        if (gwStr != null) { try { gridW = Integer.parseInt(gwStr.trim()); } catch (NumberFormatException ignore) {} }
        if (ghStr != null) { try { gridH = Integer.parseInt(ghStr.trim()); } catch (NumberFormatException ignore) {} }

        Component[][] grid = new Component[Math.max(gridH, 50)][Math.max(gridW, 50)];

        // Find components array
        int arrStart = findKeyArray(json, "components");
        if (arrStart < 0) throw new IOException("Invalid .csim file: missing 'components' field.");

        int arrEnd = findMatchingBracket(json, arrStart, '[', ']');
        if (arrEnd < 0) throw new IOException("Invalid .csim file: malformed components array.");

        String arrayContent = json.substring(arrStart + 1, arrEnd);
        List<Map<String, String>> entries = parseObjectArray(arrayContent);

        for (Map<String, String> props : entries) {
            Component comp = deserializeComponent(props);
            if (comp != null) {
                int c = comp.getCol();
                int r = comp.getRow();
                if (r >= 0 && r < grid.length && c >= 0 && c < grid[0].length) {
                    grid[r][c] = comp;
                }
            }
        }

        return grid;
    }

    // ---- Parsing helpers ----------------------------------------------------

    /** Finds the '[' that starts the value of the given key. */
    private static int findKeyArray(String json, String key) {
        String needle = "\"" + key + "\"";
        int idx = json.indexOf(needle);
        if (idx < 0) return -1;
        int colon = json.indexOf(':', idx + needle.length());
        if (colon < 0) return -1;
        // Skip whitespace to find '['
        int i = colon + 1;
        while (i < json.length() && json.charAt(i) != '[') i++;
        return i < json.length() ? i : -1;
    }

    /**
     * Extracts a simple scalar value (number, boolean) or a quoted string value
     * for the given JSON key at the top level. Returns null if not found.
     */
    private static String extractStringValue(String json, String key) {
        String needle = "\"" + key + "\"";
        int idx = json.indexOf(needle);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + needle.length());
        if (colon < 0) return null;
        int i = colon + 1;
        // Skip whitespace
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        if (i >= json.length()) return null;
        char first = json.charAt(i);
        if (first == '"') {
            // Quoted string
            int end = i + 1;
            while (end < json.length() && json.charAt(end) != '"') {
                if (json.charAt(end) == '\\') end++;
                end++;
            }
            return json.substring(i + 1, end);
        } else {
            // Scalar (number / boolean)
            int end = i;
            while (end < json.length() && ",}\n\r ".indexOf(json.charAt(end)) < 0) end++;
            return json.substring(i, end).trim();
        }
    }

    /** Finds the closing bracket/brace that matches the opening one at 'start'. */
    private static int findMatchingBracket(String s, int start, char open, char close) {
        int depth = 0;
        boolean inString = false;
        for (int i = start; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\\' && inString) { i++; continue; }
            if (ch == '"') { inString = !inString; continue; }
            if (inString) continue;
            if (ch == open)  { depth++; }
            else if (ch == close) { depth--; if (depth == 0) return i; }
        }
        return -1;
    }

    /** Parses the content between outer array brackets into a list of key→value maps. */
    private static List<Map<String, String>> parseObjectArray(String content) {
        List<Map<String, String>> list = new ArrayList<>();
        int i = 0;
        while (i < content.length()) {
            int objStart = content.indexOf('{', i);
            if (objStart < 0) break;
            int objEnd = findMatchingBracket(content, objStart, '{', '}');
            if (objEnd < 0) break;
            String objStr = content.substring(objStart + 1, objEnd);
            list.add(parseSimpleObject(objStr));
            i = objEnd + 1;
        }
        return list;
    }

    /**
     * Parses a flat JSON object body (key: value pairs, no nested objects).
     * Returns a map of key -> raw-value strings.
     */
    private static Map<String, String> parseSimpleObject(String body) {
        Map<String, String> map = new LinkedHashMap<>();
        int i = 0;
        while (i < body.length()) {
            // Find key
            int ks = body.indexOf('"', i);
            if (ks < 0) break;
            int ke = body.indexOf('"', ks + 1);
            if (ke < 0) break;
            String key = body.substring(ks + 1, ke);

            // Find colon
            int colon = body.indexOf(':', ke + 1);
            if (colon < 0) break;

            // Skip whitespace to find value start
            int vs = colon + 1;
            while (vs < body.length() && Character.isWhitespace(body.charAt(vs))) vs++;
            if (vs >= body.length()) break;

            String value;
            char fc = body.charAt(vs);
            if (fc == '"') {
                // Quoted string – find closing quote respecting escapes
                int ve = vs + 1;
                while (ve < body.length()) {
                    char c = body.charAt(ve);
                    if (c == '\\') { ve += 2; continue; }
                    if (c == '"') break;
                    ve++;
                }
                // Unescape common sequences
                value = body.substring(vs + 1, ve)
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\")
                        .replace("\\n", "\n");
                i = ve + 1;
            } else {
                // Scalar value
                int ve = vs;
                while (ve < body.length()) {
                    char c = body.charAt(ve);
                    if (c == ',' || c == '}' || c == '\n' || c == '\r') break;
                    ve++;
                }
                value = body.substring(vs, ve).trim();
                i = ve + 1;
            }
            map.put(key, value);
        }
        return map;
    }

    // ---- Deserialization ----------------------------------------------------

    private static Component deserializeComponent(Map<String, String> p) {
        String type = p.get("type");
        if (type == null) return null;

        int col = intVal(p, "col", 0);
        int row = intVal(p, "row", 0);
        Direction dir = directionVal(p, "direction");

        Component comp = null;
        switch (type) {
            case "Battery": {
                Battery b = new Battery(col, row);
                b.setVoltage(doubleVal(p, "voltage", 9.0));
                b.setInternalResistance(doubleVal(p, "internalResistance", 0.0));
                comp = b;
                break;
            }
            case "Resistor": {
                Resistor r = new Resistor(col, row);
                r.setResistance(doubleVal(p, "resistance", 100.0));
                comp = r;
                break;
            }
            case "Bulb": {
                Bulb b = new Bulb(col, row);
                b.setResistance(doubleVal(p, "resistance", 100.0));
                comp = b;
                break;
            }
            case "Wire": {
                Wire.WireType wt = Wire.WireType.STRAIGHT;
                String wts = p.get("wireType");
                if (wts != null) {
                    try { wt = Wire.WireType.valueOf(wts); } catch (IllegalArgumentException ignore) {}
                }
                comp = new Wire(col, row, wt);
                break;
            }
            case "Switch": {
                Switch sw = new Switch(col, row);
                // Switch defaults to open=true; if saved as open=false we need to close it
                boolean isOpen = boolVal(p, "open", true);
                sw.setOpen(isOpen);
                comp = sw;
                break;
            }
            case "Ammeter":  comp = new Ammeter(col, row);  break;
            case "Voltmeter": comp = new Voltmeter(col, row); break;
            case "Diode":    comp = new Diode(col, row);    break;
            case "LED":      comp = new LED(col, row);      break;
            case "Fuse": {
                Fuse f = new Fuse(col, row);
                f.setLimit(doubleVal(p, "limit", 5.0));
                f.setBlown(boolVal(p, "blown", false));
                comp = f;
                break;
            }
            case "TextLabel": {
                TextLabel tl = new TextLabel(col, row);
                tl.setText(p.getOrDefault("text", "Label"));
                comp = tl;
                break;
            }
            case "Capacitor": {
                Capacitor cap = new Capacitor(col, row);
                cap.setCapacitance(doubleVal(p, "capacitance", 1e-6));
                comp = cap;
                break;
            }
            case "Inductor": {
                Inductor ind = new Inductor(col, row);
                ind.setInductance(doubleVal(p, "inductance", 1.0));
                comp = ind;
                break;
            }
            case "ACMotor": {
                ACMotor ac = new ACMotor(col, row);
                ac.setPeakVoltage(doubleVal(p, "peakVoltage", 9.0));
                ac.setFrequency(doubleVal(p, "frequency", 1.0));
                comp = ac;
                break;
            }
            default:
                return null;
        }

        if (comp != null) comp.setDirection(dir);
        return comp;
    }

    // ---- Value helpers ------------------------------------------------------

    private static int intVal(Map<String, String> p, String key, int def) {
        String s = p.get(key);
        if (s == null) return def;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
    }

    private static double doubleVal(Map<String, String> p, String key, double def) {
        String s = p.get(key);
        if (s == null) return def;
        try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return def; }
    }

    private static boolean boolVal(Map<String, String> p, String key, boolean def) {
        String s = p.get(key);
        if (s == null) return def;
        return s.trim().equalsIgnoreCase("true");
    }

    private static Direction directionVal(Map<String, String> p, String key) {
        String s = p.get(key);
        if (s == null) return Direction.NORTH;
        try { return Direction.valueOf(s.trim()); } catch (IllegalArgumentException e) { return Direction.NORTH; }
    }
}
