package local.githubpanel;

/** Values stored locally are restricted to UI preferences, never a browsing URL. */
final class PanelPreferences {
    private PanelPreferences() {}
    static String section(String value) {
        return "pulls".equals(value) || "projects".equals(value) ? value : "issues";
    }
    static double zoom(String value) {
        try {
            double zoom = Double.parseDouble(value);
            return Double.isFinite(zoom) ? Math.max(0.5, Math.min(2.0, zoom)) : 1.0;
        } catch (RuntimeException ignored) { return 1.0; }
    }
}
