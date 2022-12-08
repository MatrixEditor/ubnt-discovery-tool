/*
 * MIT License
 *
 * Copyright (c) 2022 MatrixEditor
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.ubnt.ui; //@date 13.11.2022


import java.awt.*;
import java.util.Arrays;

/**
 * This class provides some utility methods around the powerful
 * {@link GridBagLayout} manager.
 * <p>
 * It is recommended to create the {@link LayoutManager} instance before
 * calling the {@link #initLayout(GridBagLayout, int, int)} method, for
 * example:
 * <pre>
 *     GridBagLayout layout = new GridBagLayout();
 *     GridBagUtil.initLayout(layout, 3, 3); // 3x3 grid
 * </pre>
 *
 * @see GridBagLayout
 * @see GridBagConstraints
 * @see GbcBuilder
 */
public final class GridBagUtil {

    private GridBagUtil() {
    }

    public static GbcBuilder gbc() {
        return gbc(new GridBagConstraints());
    }

    public static GbcBuilder gbc(GridBagConstraints gbc) {
        return new GbcBuilder(gbc);
    }

    public static GridBagLayout createLayout(int rows, int columns) {
        GridBagLayout layout = new GridBagLayout();
        initLayout(layout, rows, columns);
        return layout;
    }

    public static void initLayout(GridBagLayout layout, int rows, int columns) {
        layout.rowHeights             = new int[rows + 1];
        layout.rowWeights             = new double[rows + 1];
        layout.rowWeights[rows]       = Double.MIN_VALUE;
        layout.columnWeights          = new double[columns + 1];
        layout.columnWeights[columns] = Double.MIN_VALUE;
        layout.columnWidths           = new int[columns + 1];
    }

    public static void setRowWeights(GridBagLayout layout, int from, int to, double value) {
        Arrays.fill(layout.rowWeights, from, to, value);
    }

    public static void setColumnWeights(GridBagLayout layout, int from, int to, double value) {
        Arrays.fill(layout.columnWeights, from, to, value);
    }

    public static class GbcBuilder {
        private final GridBagConstraints gbc;

        public GbcBuilder(GridBagConstraints gbc) {
            this.gbc = gbc;
        }

        public GbcBuilder gridXY(int x, int y) {
            gbc.gridx = x;
            gbc.gridy = y;
            return this;
        }

        public GbcBuilder widthXY(int widthX, int heightY) {
            gbc.gridwidth  = widthX;
            gbc.gridheight = heightY;
            return this;
        }

        public GbcBuilder anchor(int anchor) {
            gbc.anchor = anchor;
            return this;
        }

        public GbcBuilder fill(int fill) {
            gbc.fill = fill;
            return this;
        }

        public GbcBuilder insets(int top, int left, int bottom, int right) {
            gbc.insets = new Insets(top, left, bottom, right);
            return this;
        }

        public GbcBuilder weightX(double weight) {
            gbc.weightx = weight;
            return this;
        }

        public GridBagConstraints finish() {
            return gbc;
        }
    }
}
