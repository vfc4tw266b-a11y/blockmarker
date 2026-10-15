package com.example.blockmarker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

/**
 * Zeichnet die Markierungen als Overlay ueber das Bild. Weil sie direkt auf dem Bildschirm gezeichnet werden,
 * sind sie automatisch durch Waende und Boden sichtbar.
 */
final class MarkerHud {

    private static final int ORANGE = 0xFFFFA726;
    private static final int ORANGE_HI = 0xFFFFD08A;
    private static final int PILL = 0xD0101218;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int SUB = 0xFFB4BAC7;
    private static final double MAX_DIST = 400.0;

    /** Zeitpunkt des letzten Spiel-Ticks (fuer weiche Bewegung zwischen den Ticks). */
    static volatile long lastTickNs = System.nanoTime();

    private static String toastText = null;
    private static long toastNs = 0;

    private MarkerHud() {}

    static void toast(String text) {
        toastText = text;
        toastNs = System.nanoTime();
    }

    /** F6: entfernt die Markierung, die dem Spieler am naechsten ist. */
    static void removeNearest() {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null) {
            return;
        }
        int idx = Markers.nearestActive(p.getX(), p.getY() + p.getEyeHeight(), p.getZ());
        if (idx < 0) {
            toast("Keine aktive Markierung mehr");
            return;
        }
        Markers.set(idx, false);
        Markers.save();
        int[] c = Markers.POS[idx];
        toast("Markierung #" + (idx + 1) + " entfernt   (X " + c[0] + "  Y " + c[1] + "  Z " + c[2] + ")");
    }

    // ------------------------------------------------------------ Projektion Welt -> Bildschirm

    private static double camX, camY, camZ;
    private static double fx, fy, fz, rx, ry, rz, ux, uy, uz;
    private static double tanX, tanY;
    private static int screenW, screenH;

    /** Rechnet einen Weltpunkt in Bildschirmkoordinaten um. false = liegt hinter der Kamera. */
    private static boolean project(double x, double y, double z, double[] out) {
        double dx = x - camX;
        double dy = y - camY;
        double dz = z - camZ;
        double depth = dx * fx + dy * fy + dz * fz;
        if (depth < 0.05) {
            return false;
        }
        double right = dx * rx + dy * ry + dz * rz;
        double up = dx * ux + dy * uy + dz * uz;
        out[0] = screenW / 2.0 + (right / (depth * tanX)) * (screenW / 2.0);
        out[1] = screenH / 2.0 - (up / (depth * tanY)) * (screenH / 2.0);
        out[2] = depth;
        return true;
    }

    // ------------------------------------------------------------ Zeichnen

    static void render(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || mc.level == null) {
            return;
        }
        long now = System.nanoTime();
        float pt = Math.min(1f, Math.max(0f, (now - lastTickNs) / 50_000_000f));

        screenW = mc.getWindow().getGuiScaledWidth();
        screenH = mc.getWindow().getGuiScaledHeight();

        camX = Mth.lerp(pt, p.xo, p.getX());
        camY = Mth.lerp(pt, p.yo, p.getY()) + p.getEyeHeight();
        camZ = Mth.lerp(pt, p.zo, p.getZ());

        double yaw = Math.toRadians(p.getViewYRot(pt));
        double pitch = Math.toRadians(p.getViewXRot(pt));
        fx = -Math.sin(yaw) * Math.cos(pitch);
        fy = -Math.sin(pitch);
        fz = Math.cos(yaw) * Math.cos(pitch);
        rx = -Math.cos(yaw);
        ry = 0.0;
        rz = -Math.sin(yaw);
        ux = ry * fz - rz * fy;
        uy = rz * fx - rx * fz;
        uz = rx * fy - ry * fx;

        double fov = FovProvider.get(mc, pt);
        tanY = Math.tan(Math.toRadians(fov) / 2.0);
        tanX = tanY * ((double) screenW / screenH);

        float pulse = 0.5f + 0.5f * (float) Math.sin(now / 180_000_000.0);
        double[] tmp = new double[3];

        for (int i = 0; i < Markers.count(); i++) {
            if (!Markers.ENABLED[i]) {
                continue;
            }
            drawMarker(g, mc, i, pulse, tmp);
        }
        drawToast(g, mc, now);
    }

    private static void drawMarker(GuiGraphicsExtractor g, Minecraft mc, int idx, float pulse, double[] tmp) {
        int[] c = Markers.POS[idx];
        double dist = Markers.distance(idx, camX, camY, camZ);
        if (dist > MAX_DIST) {
            return;
        }

        // Box des Blocks: alle 8 Ecken projizieren
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        boolean behind = false;
        for (int dx = 0; dx <= 1 && !behind; dx++) {
            for (int dy = 0; dy <= 1 && !behind; dy++) {
                for (int dz = 0; dz <= 1; dz++) {
                    if (!project(c[0] + dx, c[1] + dy, c[2] + dz, tmp)) {
                        behind = true;
                        break;
                    }
                    minX = Math.min(minX, tmp[0]);
                    maxX = Math.max(maxX, tmp[0]);
                    minY = Math.min(minY, tmp[1]);
                    maxY = Math.max(maxY, tmp[1]);
                }
            }
        }
        double cxm;
        double cym;
        if (behind) {
            if (!project(c[0] + 0.5, c[1] + 0.5, c[2] + 0.5, tmp)) {
                return;
            }
            cxm = tmp[0];
            cym = tmp[1];
            minX = cxm - 7;
            maxX = cxm + 7;
            minY = cym - 7;
            maxY = cym + 7;
        } else {
            cxm = (minX + maxX) / 2.0;
            cym = (minY + maxY) / 2.0;
        }
        // Mindestgroesse, damit auch weit entfernte Bloecke gut sichtbar sind
        if (maxX - minX < 12) {
            minX = cxm - 6;
            maxX = cxm + 6;
        }
        if (maxY - minY < 12) {
            minY = cym - 6;
            maxY = cym + 6;
        }
        if (maxX < -80 || minX > screenW + 80 || maxY < -80 || minY > screenH + 80) {
            return;
        }

        int x0 = (int) Math.round(minX);
        int y0 = (int) Math.round(minY);
        int w = (int) Math.round(maxX - minX);
        int h = (int) Math.round(maxY - minY);

        // Leuchten + Rahmen
        Ui.rrectOutline(g, x0 - 3, y0 - 3, w + 6, h + 6, 4, Ui.alpha(ORANGE, 0.18f + 0.18f * pulse));
        Ui.rrectOutline(g, x0 - 2, y0 - 2, w + 4, h + 4, 3, Ui.alpha(ORANGE, 0.35f + 0.25f * pulse));
        Ui.rrect(g, x0, y0, w, h, 2, Ui.alpha(ORANGE, 0.14f + 0.10f * pulse));
        Ui.rrectOutline(g, x0 - 1, y0 - 1, w + 2, h + 2, 2, ORANGE);

        // Beschriftung: Nummer + Entfernung
        boolean aimed = Math.hypot(cxm - screenW / 2.0, cym - screenH / 2.0) < 30;
        String label = "#" + (idx + 1) + "   " + Math.round(dist) + " m";
        int lw = mc.font.width(label) + 12;
        int px = (int) Math.round(cxm) - lw / 2;
        px = Math.max(2, Math.min(screenW - lw - 2, px));
        int py = y0 - 20;
        if (py < 2) {
            py = y0 + h + 6;
        }
        Ui.rrect(g, px, py, lw, 14, 7, PILL);
        Ui.rrectOutline(g, px, py, lw, 14, 7, Ui.alpha(ORANGE, aimed ? 0.9f : 0.45f));
        g.text(mc.font, label, px + 6, py + 3, aimed ? ORANGE_HI : WHITE, false);

        if (aimed) {
            String coords = "X " + c[0] + "   Y " + c[1] + "   Z " + c[2];
            int cw = mc.font.width(coords) + 12;
            int cpx = Math.max(2, Math.min(screenW - cw - 2, (int) Math.round(cxm) - cw / 2));
            int cpy = y0 + h + 6;
            Ui.rrect(g, cpx, cpy, cw, 14, 7, PILL);
            g.text(mc.font, coords, cpx + 6, cpy + 3, SUB, false);
        }
    }

    private static void drawToast(GuiGraphicsExtractor g, Minecraft mc, long now) {
        if (toastText == null) {
            return;
        }
        float t = (now - toastNs) / 1_000_000_000f;
        float life = 2.6f;
        if (t > life) {
            toastText = null;
            return;
        }
        float a = Math.min(Ui.clamp01(t / 0.18f), Ui.clamp01((life - t) / 0.45f));
        float slide = (1f - Ui.easeOutCubic(Ui.clamp01(t / 0.25f))) * 10f;
        int w = mc.font.width(toastText) + 24;
        int x = (screenW - w) / 2;
        int y = 18 - Math.round(slide);
        Ui.rrect(g, x, y, w, 20, 10, Ui.alpha(PILL, a));
        Ui.rrectOutline(g, x, y, w, 20, 10, Ui.alpha(ORANGE, 0.7f * a));
        int tc = Ui.alpha(WHITE, a);
        if (((tc >>> 24) & 0xFF) >= 12) {
            g.text(mc.font, toastText, x + 12, y + 6, tc, false);
        }
    }
}
