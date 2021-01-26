/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.gui.chartbasics.listener;

import io.github.mzmine.gui.chartbasics.ChartLogicsFX;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.input.MouseButton;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;

public class RegionSelectionListener implements ChartMouseListenerFX {

  private static Logger logger = Logger.getLogger(RegionSelectionListener.class.getName());

  private final BooleanSupplier mayPlaceCondition;
  private final ObjectProperty<java.awt.geom.Path2D> buildingPath;
  private final List<Point2D> points;
  private final EChartViewer chart;

  public RegionSelectionListener(BooleanSupplier mayPlaceCondition, EChartViewer chart) {
    this.mayPlaceCondition = mayPlaceCondition;
    this.chart = chart;
    points = new ArrayList<>();
    buildingPath = new SimpleObjectProperty<>();
  }

  @Override
  public void chartMouseClicked(ChartMouseEventFX event) {
    logger.info("" + event.getTrigger().getX());
    logger.info("" + event.getTrigger().getY());

    event.getTrigger().consume();
    if (event.getTrigger().getButton() != MouseButton.PRIMARY) {
      return;
    }

    Point2D p = ChartLogicsFX
        .mouseXYToPlotXY(chart, event.getTrigger().getX(), event.getTrigger().getY());

    points.add(p);
    buildingPath.set(getShape());
  }

  @Override
  public void chartMouseMoved(ChartMouseEventFX event) {

  }

  private Path2D getShape() {
    if (points.isEmpty()) {
      return null;
    }
    Path2D path = new Path2D.Double();
    path.moveTo(points.get(0).getX(), points.get(0).getY());

    for (int i = 1; i < points.size(); i++) {
      path.lineTo(points.get(i).getX(), points.get(i).getY());
    }
    path.closePath();
    return path;
  }

  public ObjectProperty<Path2D> buildingPathProperty() {
    return buildingPath;
  }
}
