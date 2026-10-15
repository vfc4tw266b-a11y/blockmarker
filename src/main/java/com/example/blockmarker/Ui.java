package com.example.blockmarker;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Zeichen- und Animations-Helfer. */
final class Ui {

    private Ui() {}

    static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }

    static float easeOutCubic(float t) {
        float u = 1f - clamp01(t);
        return 1f - u * u * u;
    }

    static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /** Naehert einen Wert zeitabhaengig und weich an ein Ziel an. */
    static float approach(float value, float target, float dt, float rate) {
        float v = value + (target - value) * (1f - (float) Math.exp(-dt * rate));
        return Math.abs(v - target) < 0.002f ? target : v;
    }

    static int alpha(int argb, float a) {
        int al = Math.round(((argb >>> 24) & 0xFF) * clamp01(a));
        return (al << 24) | (argb & 0xFFFFFF);
    }

    static int mix(int c1, int c2, float t) {
        t = clamp01(t);
        int a = Math.round(lerp((c1 >>> 24) & 0xFF, (c2 >>> 24) & 0xFF, t));
        int r = Math.round(lerp((c1 >> 16) & 0xFF, (c2 >> 16) & 0xFF, t));
        int g = Math.round(lerp((c1 >> 8) & 0xFF, (c2 >> 8) & 0xFF, t));
        int b = Math.round(lerp(c1 & 0xFF, c2 & 0xFF, t));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Gefuelltes Rechteck mit runden Ecken. */
    static void rrect(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        g.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            g.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
    }

    /** Nur der 1px-Rand eines runden Rechtecks. */
    static void rrectOutline(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int color) {
        if (w <= 2 || h <= 2) {
            return;
        }
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 1) {
            g.fill(x, y, x + w, y + 1, color);
            g.fill(x, y + h - 1, x + w, y + h, color);
            g.fill(x, y + 1, x + 1, y + h - 1, color);
            g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
            return;
        }
        int prev = 0;
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            if (i == 0) {
                g.fill(x + inset, y, x + w - inset, y + 1, color);
                g.fill(x + inset, y + h - 1, x + w - inset, y + h, color);
            } else {
                int segEnd = Math.max(inset + 1, prev);
                g.fill(x + inset, y + i, x + segEnd, y + i + 1, color);
                g.fill(x + w - segEnd, y + i, x + w - inset, y + i + 1, color);
                g.fill(x + inset, y + h - 1 - i, x + segEnd, y + h - i, color);
                g.fill(x + w - segEnd, y + h - 1 - i, x + w - inset, y + h - i, color);
            }
            prev = inset;
        }
        g.fill(x, y + r, x + 1, y + h - r, color);
        g.fill(x + w - 1, y + r, x + w, y + h - r, color);
    }
}
