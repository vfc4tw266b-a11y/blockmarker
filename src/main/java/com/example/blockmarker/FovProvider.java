package com.example.blockmarker;

import net.minecraft.client.Minecraft;

import java.lang.reflect.Method;

/**
 * Liefert den tatsaechlich verwendeten Blickwinkel (inkl. Sprint-/Effekt-Aenderung), damit die Markierungen
 * exakt mit der Welt uebereinstimmen. Faellt sonst auf die FOV-Einstellung zurueck.
 */
final class FovProvider {

    private static boolean resolved = false;
    private static Method getFov;
    private static Method getCamera;

    private FovProvider() {}

    static float get(Minecraft mc, float partialTick) {
        float base = mc.options.fov().get();
        try {
            Object renderer = mc.gameRenderer;
            if (!resolved) {
                resolved = true;
                try {
                    getCamera = renderer.getClass().getMethod("getMainCamera");
                } catch (Throwable t) {
                    getCamera = null;
                }
                for (Method m : renderer.getClass().getDeclaredMethods()) {
                    Class<?>[] p = m.getParameterTypes();
                    if (m.getName().equals("getFov") && p.length == 3 && p[1] == float.class
                            && p[2] == boolean.class) {
                        m.setAccessible(true);
                        getFov = m;
                        break;
                    }
                }
            }
            if (getFov != null && getCamera != null) {
                Object cam = getCamera.invoke(renderer);
                Object r = getFov.invoke(renderer, cam, partialTick, true);
                if (r instanceof Number n) {
                    float v = n.floatValue();
                    if (v > 10f && v < 170f) {
                        return v;
                    }
                }
            }
        } catch (Throwable t) {
            getFov = null;
        }
        return base;
    }
}
