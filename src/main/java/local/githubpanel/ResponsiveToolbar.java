package local.githubpanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/** Invalidates BoxLayout's cached height when the available width changes. */
final class ResponsiveToolbar extends JPanel {
    private int measuredWidth = -1;
    ResponsiveToolbar() {
        super(new WrapLayout());
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent event) {
                if (getWidth() == measuredWidth) return;
                measuredWidth = getWidth();
                Container parent = getParent();
                if (parent != null) { parent.invalidate(); parent.revalidate(); }
            }
        });
    }
}
