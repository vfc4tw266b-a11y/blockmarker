package com.example.blockmarker;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Comparator;

/** F9: Liste aller Koordinaten mit einem Schalter pro Eintrag. */
public class MarkerMenuScreen extends Screen {

    // ---------------------------------------------------------------- Farben
    private static final int WINDOW_BG = 0xD0101218;
    private static final int EDGE = 0x30FFFFFF;
    private static final int CARD_BG = 0x12FFFFFF;
    private static final int CARD_HOVER = 0x26FFFFFF;
    private static final int TEXT = 0xFFEDEFF4;
    private static final int SUB = 0xFF8E96A8;
    private static final int ACCENT = 0xFFFFA726;
    private static final int ACCENT_HI = 0xFFFFD08A;
    private static final int TRACK_OFF = 0xFF2B303B;
    private static final int WHITE = 0xFFFFFFFF;

    // ---------------------------------------------------------------- Masse
    private static final int HEADER_H = 64;
    private static final int ROW_H = 32;
    private static final int ROW_GAP = 6;
    private static final int N = Markers.count();

    // ---------------------------------------------------------------- Zustand
    private float openT = 0f;
    private boolean closing = false;
    private final long openedNs = System.nanoTime();
    private long lastNs = System.nanoTime();
    private float fade = 1f;

    private boolean sortByDistance = false;
    private Integer[] order = new Integer[N];

    private float scrollPos = 0f;
    private float scrollVel = 0f;
    private float scrollbarA = 0f;

    private final float[] rowHover = new float[N];
    private final float[] rowAnim = new float[N];
    private final float[] rowPress = new float[N];
    private final int[] rowY = new int[N];
    private final float[] btnHover = new float[3];

    // ---------------------------------------------------------------- Layout
    private int wx, wy, ww, wh, listY, listH, rowX, rowW;
    private float maxScroll;
    private final int[] btnX = new int[3];
    private final int[] btnW = new int[3];
    private int btnY;
    private static final int BTN_H = 22;

    public MarkerMenuScreen() {
        super(Component.literal("Block Marker"));
        for (int i = 0; i < N; i++) {
            rowAnim[i] = Markers.ENABLED[i] ? 1f : 0f;
        }
        computeOrder();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        closing = true;
    }

    @Override
    public void tick() {
        if (closing && openT <= 0f) {
            super.onClose();
        }
    }

    @Override
    public void removed() {
        Markers.save();
        super.removed();
    }

    private void computeOrder() {
        for (int i = 0; i < N; i++) {
            order[i] = i;
        }
        if (sortByDistance) {
            var p = Minecraft.getInstance().player;
            if (p != null) {
                double px = p.getX();
                double py = p.getY() + p.getEyeHeight();
                double pz = p.getZ();
                Arrays.sort(order, Comparator.comparingDouble(i -> Markers.distance(i, px, py, pz)));
            }
        }
    }

    // ================================================================ Layout / Update

    private String sortLabel() {
        return sortByDistance ? "Sortierung: Nähe" : "Sortierung: Nummer";
    }

    private void layout() {
        ww = Math.min(this.width - 40, 460);
        wh = Math.min(this.height - 40, 340);
        wx = (this.width - ww) / 2;
        wy = (this.height - wh) / 2;
        listY = wy + HEADER_H;
        listH = wh - HEADER_H - 14;
        rowX = wx + 16;
        rowW = ww - 32 - 8;

        String[] labels = {sortLabel(), "Alle an", "Alle aus"};
        int x = wx + ww - 16;
        btnY = wy + 14;
        for (int i = 2; i >= 0; i--) {
            btnW[i] = this.font.width(labels[i]) + 18;
            x -= btnW[i];
            btnX[i] = x;
            x -= 6;
        }

        float total = N * (ROW_H + ROW_GAP) - ROW_GAP + 8;
        maxScroll = Math.max(0f, total - listH);
    }

    private void update(float dt) {
        openT = Ui.clamp01(openT + (closing ? -dt / 0.20f : dt / 0.30f));

        // Scrollen mit Traegheit + Gummiband an den Raendern
        float over = scrollPos < 0f ? scrollPos : (scrollPos > maxScroll ? scrollPos - maxScroll : 0f);
        if (over != 0f) {
            scrollVel -= over * 140f * dt;
            scrollVel *= (float) Math.exp(-dt * 9f);
        } else {
            scrollVel *= (float) Math.exp(-dt * 6f);
        }
        scrollPos += scrollVel * dt;
        if (over == 0f && Math.abs(scrollVel) < 2f) {
            scrollVel = 0f;
        }
        if (over != 0f && Math.abs(scrollVel) < 2f && Math.abs(over) < 0.5f) {
            scrollPos = Math.max(0f, Math.min(maxScroll, scrollPos));
            scrollVel = 0f;
        }
        scrollbarA = Math.max(0f, scrollbarA - dt * 1.6f);
        if (Math.abs(scrollVel) > 8f) {
            scrollbarA = 1f;
        }

        for (int i = 0; i < N; i++) {
            rowAnim[i] = Ui.approach(rowAnim[i], Markers.ENABLED[i] ? 1f : 0f, dt, 13f);
            rowPress[i] = Ui.approach(rowPress[i], 0f, dt, 14f);
        }
    }

    // ================================================================ Rendering

    private void text(GuiGraphicsExtractor g, String s, int x, int y, int color, float f) {
        int c = Ui.alpha(color, f);
        if (((c >>> 24) & 0xFF) < 12) {
            return;
        }
        g.text(this.font, s, x, y, c, false);
    }

    private void text(GuiGraphicsExtractor g, Component comp, int x, int y, int color, float f) {
        int c = Ui.alpha(color, f);
        if (((c >>> 24) & 0xFF) < 12) {
            return;
        }
        g.text(this.font, comp, x, y, c, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - lastNs) / 1_000_000_000f);
        lastNs = now;

        layout();
        update(dt);

        float e = Ui.easeOutCubic(openT);
        if (e <= 0.001f) {
            return;
        }
        fade = e;
        boolean interactive = openT > 0.9f && !closing;

        g.fill(0, 0, this.width, this.height, Ui.alpha(0xFF000000, 0.35f * fade));

        float s = 0.95f + 0.05f * e;
        float pcx = wx + ww / 2f;
        float pcy = wy + wh / 2f;
        g.pose().pushMatrix();
        g.pose().translate(pcx, pcy);
        g.pose().scale(s, s);
        g.pose().translate(-pcx, -pcy);

        // Fenster (Schatten nur als Ringe aussen, damit das Glas durchsichtig bleibt)
        for (int i = 1; i <= 9; i++) {
            Ui.rrectOutline(g, wx - i, wy - i + 3, ww + 2 * i, wh + 2 * i, 14 + i,
                    Ui.alpha(0xFF000000, 0.11f * (1f - i / 10f) * fade));
        }
        Ui.rrect(g, wx, wy, ww, wh, 14, Ui.alpha(WINDOW_BG, fade));
        Ui.rrectOutline(g, wx, wy, ww, wh, 14, Ui.alpha(EDGE, fade));

        drawHeader(g, mouseX, mouseY, dt, interactive);
        drawList(g, mouseX, mouseY, dt, interactive);

        g.pose().popMatrix();
    }

    private void drawHeader(GuiGraphicsExtractor g, int mx, int my, float dt, boolean interactive) {
        Ui.rrect(g, wx + 18, wy + 19, 8, 8, 4, Ui.alpha(ACCENT, fade));
        text(g, Component.literal("Block Marker").withStyle(ChatFormatting.BOLD), wx + 32, wy + 19, TEXT, fade);
        text(g, Markers.activeCount() + " von " + N + " markiert", wx + 18, wy + 36, SUB, fade);

        String[] labels = {sortLabel(), "Alle an", "Alle aus"};
        for (int i = 0; i < 3; i++) {
            boolean over = interactive && Ui.inside(mx, my, btnX[i], btnY, btnW[i], BTN_H);
            btnHover[i] = Ui.approach(btnHover[i], over ? 1f : 0f, dt, 16f);
            float h = btnHover[i];
            float sc = 1f + 0.04f * h;
            float ccx = btnX[i] + btnW[i] / 2f;
            float ccy = btnY + BTN_H / 2f;
            g.pose().pushMatrix();
            g.pose().translate(ccx, ccy);
            g.pose().scale(sc, sc);
            g.pose().translate(-ccx, -ccy);
            Ui.rrect(g, btnX[i], btnY, btnW[i], BTN_H, 8, Ui.alpha(Ui.mix(CARD_BG, CARD_HOVER, h), fade));
            Ui.rrectOutline(g, btnX[i], btnY, btnW[i], BTN_H, 8, Ui.alpha(ACCENT, 0.55f * h * fade));
            int lw = this.font.width(labels[i]);
            text(g, labels[i], btnX[i] + (btnW[i] - lw) / 2, btnY + 7, Ui.mix(SUB, TEXT, h), fade);
            g.pose().popMatrix();
        }
        g.fill(wx + 16, wy + HEADER_H - 8, wx + ww - 16, wy + HEADER_H - 7, Ui.alpha(EDGE, fade));
    }

    private void drawList(GuiGraphicsExtractor g, int mx, int my, float dt, boolean interactive) {
        var p = Minecraft.getInstance().player;
        double px = p != null ? p.getX() : 0;
        double py = p != null ? p.getY() + p.getEyeHeight() : 0;
        double pz = p != null ? p.getZ() : 0;

        g.enableScissor(rowX - 6, listY, rowX + rowW + 14, listY + listH);
        for (int k = 0; k < N; k++) {
            int idx = order[k];
            int y = Math.round(listY + 4 + k * (ROW_H + ROW_GAP) - scrollPos);
            rowY[idx] = y;
            if (y + ROW_H < listY || y > listY + listH) {
                continue;
            }
            boolean over = interactive && Ui.inside(mx, my, rowX, y, rowW, ROW_H)
                    && my >= listY && my < listY + listH
                    && !Ui.inside(mx, my, 0, 0, 0, 0);
            rowHover[idx] = Ui.approach(rowHover[idx], over ? 1f : 0f, dt, 15f);
            drawRow(g, idx, y, px, py, pz);
        }
        g.disableScissor();

        if (maxScroll > 0.5f) {
            float total = maxScroll + listH;
            int barH = Math.max(22, Math.round(listH * listH / total));
            float t = Math.max(0f, Math.min(1f, scrollPos / maxScroll));
            int barY = listY + Math.round((listH - barH) * t);
            int col = Ui.mix(0x40FFFFFF, ACCENT, scrollbarA);
            Ui.rrect(g, wx + ww - 11, barY, 3, barH, 1, Ui.alpha(col, fade * (0.5f + 0.5f * scrollbarA)));
        }
    }

    private void drawRow(GuiGraphicsExtractor g, int idx, int y, double px, double py, double pz) {
        float h = rowHover[idx];
        float a = rowAnim[idx];
        float scale = 1f + 0.015f * h - 0.025f * rowPress[idx];
        float ccx = rowX + rowW / 2f;
        float ccy = y + ROW_H / 2f;
        g.pose().pushMatrix();
        g.pose().translate(ccx, ccy);
        g.pose().scale(scale, scale);
        g.pose().translate(-ccx, -ccy);

        Ui.rrect(g, rowX, y, rowW, ROW_H, 9, Ui.alpha(Ui.mix(CARD_BG, CARD_HOVER, h), fade));

        // Nummer
        String num = "#" + (idx + 1);
        int bw = 38;
        Ui.rrect(g, rowX + 8, y + 6, bw, 20, 7, Ui.alpha(Ui.mix(0x22FFFFFF, 0x33FFA726, a), fade));
        text(g, num, rowX + 8 + (bw - this.font.width(num)) / 2, y + 12, Ui.mix(SUB, ACCENT_HI, a), fade);

        // Koordinaten
        int[] c = Markers.POS[idx];
        String coords = "X " + c[0] + "   Y " + c[1] + "   Z " + c[2];
        text(g, coords, rowX + 56, y + 12, Ui.mix(SUB, TEXT, a), fade);

        // Entfernung
        String dist = Math.round(Markers.distance(idx, px, py, pz)) + " m";
        int tw = 36;
        int tx = rowX + rowW - 12 - tw;
        text(g, dist, tx - 10 - this.font.width(dist), y + 12, SUB, fade);

        // Schalter
        int th = 18;
        int ty = y + (ROW_H - th) / 2;
        Ui.rrect(g, tx, ty, tw, th, 9, Ui.alpha(Ui.mix(TRACK_OFF, ACCENT, a), fade));
        int knob = 14;
        int kx = tx + 2 + Math.round(a * (tw - knob - 4));
        Ui.rrect(g, kx, ty + 2, knob, knob, 7, Ui.alpha(WHITE, fade));

        g.pose().popMatrix();
    }

    // ================================================================ Eingabe

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && openT > 0.9f && !closing) {
            double mx = event.x();
            double my = event.y();

            for (int i = 0; i < 3; i++) {
                if (Ui.inside(mx, my, btnX[i], btnY, btnW[i], BTN_H)) {
                    if (i == 0) {
                        sortByDistance = !sortByDistance;
                        computeOrder();
                    } else {
                        Markers.setAll(i == 1);
                        Markers.save();
                    }
                    return true;
                }
            }

            if (my >= listY && my < listY + listH) {
                for (int k = 0; k < N; k++) {
                    int idx = order[k];
                    if (Ui.inside(mx, my, rowX, rowY[idx], rowW, ROW_H)) {
                        Markers.set(idx, !Markers.ENABLED[idx]);
                        rowPress[idx] = 1f;
                        Markers.save();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScroll > 0f) {
            float over = scrollPos < 0f ? -scrollPos : (scrollPos > maxScroll ? scrollPos - maxScroll : 0f);
            float damp = 1f / (1f + over / 30f);
            scrollVel += -(float) scrollY * 520f * damp;
            scrollbarA = 1f;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // Dieselbe Taste schliesst die Liste wieder (kurze Sperre, damit eine gehaltene Taste nicht sofort schliesst)
        if (System.nanoTime() - openedNs > 350_000_000L
                && Keys.matches(BlockMarkerClient.menuKey, event, BlockMarkerClient.DEFAULT_MENU_KEY)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }
}
