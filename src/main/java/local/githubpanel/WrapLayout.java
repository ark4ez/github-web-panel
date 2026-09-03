package local.githubpanel;

import java.awt.*;

/** Flow layout whose preferred height follows the available width. */
final class WrapLayout extends FlowLayout {
    WrapLayout() { super(FlowLayout.LEADING, 4, 4); }

    @Override public Dimension preferredLayoutSize(Container target) {
        synchronized (target.getTreeLock()) {
            int width = target.getWidth();
            if (width <= 0 && target.getParent() != null) width = target.getParent().getWidth();
            if (width <= 0) width = 640;
            Insets insets = target.getInsets();
            int available = Math.max(1, width - insets.left - insets.right - getHgap() * 2);
            int rowWidth = 0, rowHeight = 0, height = 0;
            for (Component component : target.getComponents()) {
                if (!component.isVisible()) continue;
                Dimension size = component.getPreferredSize();
                int gap = rowWidth == 0 ? 0 : getHgap();
                if (rowWidth > 0 && rowWidth + gap + size.width > available) {
                    height += rowHeight + getVgap();
                    rowWidth = 0;
                    rowHeight = 0;
                    gap = 0;
                }
                rowWidth += gap + size.width;
                rowHeight = Math.max(rowHeight, size.height);
            }
            return new Dimension(width, height + rowHeight + insets.top + insets.bottom + getVgap() * 2);
        }
    }

    @Override public Dimension minimumLayoutSize(Container target) {
        Dimension preferred = preferredLayoutSize(target);
        return new Dimension(0, preferred.height);
    }
}
