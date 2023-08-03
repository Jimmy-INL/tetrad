///////////////////////////////////////////////////////////////////////////////
// For information as to what this class does, see the Javadoc, below.       //
// Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006,       //
// 2007, 2008, 2009, 2010, 2014, 2015, 2022 by Peter Spirtes, Richard        //
// Scheines, Joseph Ramsey, and Clark Glymour.                               //
//                                                                           //
// This program is free software; you can redistribute it and/or modify      //
// it under the terms of the GNU General Public License as published by      //
// the Free Software Foundation; either version 2 of the License, or         //
// (at your option) any later version.                                       //
//                                                                           //
// This program is distributed in the hope that it will be useful,           //
// but WITHOUT ANY WARRANTY; without even the implied warranty of            //
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             //
// GNU General Public License for more details.                              //
//                                                                           //
// You should have received a copy of the GNU General Public License         //
// along with this program; if not, write to the Free Software               //
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA //
///////////////////////////////////////////////////////////////////////////////

package edu.cmu.tetradapp.workbench;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * The display component for measured nodes--an opaque rounded rectangle.
 *
 * @author josephramsey
 */
public class MeasuredDisplaySmallComp extends JComponent implements DisplayComp {
    private boolean selected;

    public MeasuredDisplaySmallComp(String name) {
        setBackground(DisplayNodeUtils.getNodeFillColor());
        setFont(DisplayNodeUtils.getFont());
        setName(name);
    }

    public void setName(String name) {
        super.setName(name);
        setSize(getPreferredSize());
    }

    public boolean contains(int x, int y) {
        return getShape().contains(x, y);
    }

    /**
     * @return the shape of the component.
     */
    private Shape getShape() {
        return new Ellipse2D.Double(0, 0, getPreferredSize().width - 1,
                getPreferredSize().height - 1);
    }

    /**
     * Paints the component.
     *
     * @param g the graphics context.
     */
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        FontMetrics fm = getFontMetrics(new Font("Monospaced", Font.PLAIN, 3));
        String name = getName();

        if (name.startsWith("X")) name = name.substring(1);

        final int stringX = 2;
        int stringY = 4 * fm.getAscent() + DisplayNodeUtils.getPixelGap();

        g2.setColor(isSelected() ? DisplayNodeUtils.getNodeSelectedFillColor() :
                DisplayNodeUtils.getNodeFillColor());
        g2.fill(getShape());
        g2.setColor(isSelected() ? DisplayNodeUtils.getNodeSelectedEdgeColor() :
                DisplayNodeUtils.getNodeEdgeColor());
        g2.draw(getShape());
        g2.setColor(DisplayNodeUtils.getNodeTextColor());
        g2.setFont(DisplayNodeUtils.getFont());
        g2.drawString(name, stringX, stringY);
    }

    /**
     * Calculates the size of the component based on its name.
     */
    public Dimension getPreferredSize() {
        return new Dimension(30, 30);
    }

    private boolean isSelected() {
        return this.selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}




