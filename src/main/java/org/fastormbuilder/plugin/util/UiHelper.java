package org.fastormbuilder.plugin.util;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;
import java.awt.*;

public class UiHelper {
    public static Icon icon(String path) {
        return IconLoader.getIcon(path, UiHelper.class);
    }

    public static Icon scaledIcon(String path, int size) {
        Icon original = IconLoader.getIcon(path, UiHelper.class);
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(iconToImage(original), x, y, size, size, null);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    private static java.awt.image.BufferedImage iconToImage(Icon icon) {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                icon.getIconWidth(), icon.getIconHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        icon.paintIcon(null, g, 0, 0);
        g.dispose();
        return img;
    }
}
