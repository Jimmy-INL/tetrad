///////////////////////////////////////////////////////////////////////////////
// For information as to what this class does, see the Javadoc, below.       //
// Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006,       //
// 2007, 2008, 2009, 2010, 2014, 2015 by Peter Spirtes, Richard Scheines, Joseph   //
// Ramsey, and Clark Glymour.                                                //
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
 * Eliptical variable display for a latent.
 */
public class LatentDisplayComp extends JComponent implements DisplayComp {
    private boolean selected = false;

    public LatentDisplayComp(final String name) {
        setBackground(DisplayNodeUtils.getNodeFillColor());
        setFont(DisplayNodeUtils.getFont());
        setName(name);
        super.setSize(getPreferredSize());
    }

    public void setName(final String name) {
        super.setName(name);
        setSize(getPreferredSize());
    }

    public void setSelected(final boolean selected) {
        this.selected = selected;
    }

    public boolean contains(final int x, final int y) {
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
    public void paint(final Graphics g) {
        final Graphics2D g2 = (Graphics2D) g;
        final FontMetrics fm = getFontMetrics(DisplayNodeUtils.getFont());
        final int width = getPreferredSize().width;
        final int stringWidth = fm.stringWidth(getName());
        final int stringX = (width - stringWidth) / 2;
        final int stringY = fm.getAscent() + DisplayNodeUtils.getPixelGap();

        g2.setColor(isSelected() ? DisplayNodeUtils.getNodeSelectedFillColor() :
                DisplayNodeUtils.getNodeFillColor());
        g2.fill(getShape());
        g2.setColor(isSelected() ? DisplayNodeUtils.getNodeSelectedEdgeColor() :
                DisplayNodeUtils.getNodeEdgeColor());
        g2.draw(getShape());
        g2.setColor(DisplayNodeUtils.getNodeTextColor());
        g2.setFont(DisplayNodeUtils.getFont());
        g2.drawString(getName(), stringX, stringY);
    }

    /**
     * Calculates the size of the component based on its name.
     */
    public Dimension getPreferredSize() {
        final FontMetrics fm = getFontMetrics(DisplayNodeUtils.getFont());
        final String name1 = getName();
        final int textWidth = fm.stringWidth(name1);
        final int textHeight = fm.getAscent();
        int width = textWidth + fm.getMaxAdvance() + 5;
        final int height = 2 * DisplayNodeUtils.getPixelGap() + textHeight + 5;

        width = (width < 60) ? 60 : width;

        return new Dimension(width, height);
    }

    private boolean isSelected() {
        return this.selected;
    }
}




