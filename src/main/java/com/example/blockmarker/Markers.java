package com.example.blockmarker;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;

/** Die 100 Koordinaten und welche davon gerade markiert sind (wird gespeichert). */
final class Markers {

    /** Block-Koordinaten {X, Y, Z}. Markierung Nr. N = Eintrag N-1. */
    static final int[][] POS = {
        {33, 74, -51},
        {10, 72, -9},
        {5, 73, -23},
        {9, 73, -70},
        {-7, 74, -80},
        {-21, 72, -59},
        {-33, 73, -29},
        {-37, 70, 12},
        {-61, 71, -14},
        {-66, 71, 16},
        {-58, 73, 41},
        {-32, 71, 36},
        {-10, 73, 50},
        {27, 73, 58},
        {48, 78, 67},
        {80, 81, 47},
        {82, 86, 35},
        {67, 75, 17},
        {57, 88, -34},
        {45, 54, -23},
        {64, 50, -28},
        {70, 46, -38},
        {92, 41, -61},
        {101, 44, -76},
        {109, 44, -88},
        {108, 42, -124},
        {102, 54, -152},
        {128, 53, -136},
        {118, 72, -120},
        {149, 70, -145},
        {133, 73, -173},
        {113, 90, -163},
        {103, 76, -158},
        {134, 85, -166},
        {132, 70, -51},
        {115, 86, -35},
        {109, 86, -25},
        {96, 72, 2},
        {107, 73, 24},
        {58, 92, 43},
        {45, 73, 25},
        {18, 87, 64},
        {34, 70, 99},
        {15, 83, 98},
        {13, 99, 92},
        {-13, 81, 92},
        {-36, 72, 107},
        {-51, 89, 104},
        {-82, 99, 115},
        {-103, 74, 119},
        {-82, 97, 86},
        {-100, 80, 92},
        {-106, 72, 84},
        {-77, 73, 69},
        {-63, 82, 82},
        {-56, 92, 65},
        {-37, 73, 63},
        {-12, 86, 64},
        {4, 72, 35},
        {0, 73, 3},
        {36, 71, 0},
        {69, 79, -19},
        {83, 89, -35},
        {94, 103, -53},
        {95, 83, -71},
        {96, 73, -83},
        {81, 81, -84},
        {58, 78, -76},
        {35, 77, -78},
        {53, 74, -106},
        {28, 80, -133},
        {-6, 75, -131},
        {10, 70, -126},
        {16, 77, -96},
        {6, 76, -97},
        {5, 76, -81},
        {-30, 72, -91},
        {-44, 81, -69},
        {-43, 86, -37},
        {-72, 77, -55},
        {-86, 70, -48},
        {-65, 72, -67},
        {-115, 72, -99},
        {-108, 99, -93},
        {-138, 75, -77},
        {-161, 73, -86},
        {-130, 70, -128},
        {-128, 72, -81},
        {-109, 71, -61},
        {-128, 76, -42},
        {-136, 87, -43},
        {-151, 70, -28},
        {-141, 75, -5},
        {-126, 72, 21},
        {-92, 72, 39},
        {-90, 74, 28},
        {-79, 78, -3},
        {-86, 74, -19},
        {-110, 77, -21},
        {-113, 78, -9}
    };

    /** true = Markierung ist sichtbar. */
    static final boolean[] ENABLED = new boolean[POS.length];

    static {
        Arrays.fill(ENABLED, true);
    }

    private Markers() {}

    static int count() {
        return POS.length;
    }

    static int activeCount() {
        int n = 0;
        for (boolean b : ENABLED) {
            if (b) {
                n++;
            }
        }
        return n;
    }

    static void set(int index, boolean value) {
        ENABLED[index] = value;
    }

    static void setAll(boolean value) {
        Arrays.fill(ENABLED, value);
    }

    /** Abstand vom Punkt (x, y, z) zur Mitte des Blocks. */
    static double distance(int index, double x, double y, double z) {
        int[] p = POS[index];
        double dx = p[0] + 0.5 - x;
        double dy = p[1] + 0.5 - y;
        double dz = p[2] + 0.5 - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Index der naechsten markierten (aktiven) Koordinate oder -1. */
    static int nearestActive(double x, double y, double z) {
        int best = -1;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < POS.length; i++) {
            if (!ENABLED[i]) {
                continue;
            }
            double d = distance(i, x, y, z);
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    // ------------------------------------------------------------ Speichern / Laden

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("blockmarker.properties");
    }

    static void load() {
        Path f = file();
        if (!Files.isRegularFile(f)) {
            return;
        }
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(f)) {
            p.load(r);
        } catch (IOException e) {
            return;
        }
        String disabled = p.getProperty("disabled", "");
        for (String part : disabled.split(",")) {
            try {
                int n = Integer.parseInt(part.trim());
                if (n >= 1 && n <= POS.length) {
                    ENABLED[n - 1] = false;
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    static void save() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < POS.length; i++) {
            if (!ENABLED[i]) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(i + 1);
            }
        }
        Properties p = new Properties();
        p.setProperty("disabled", sb.toString());
        try (Writer w = Files.newBufferedWriter(file())) {
            p.store(w, "Block Marker - ausgeblendete Markierungen (Nummern)");
        } catch (IOException ignored) {
        }
    }
}
