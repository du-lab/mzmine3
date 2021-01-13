/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.rawdataoverviewims.providers;

import java.awt.Color;
import java.text.NumberFormat;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.scans.ScanUtils;
import javafx.beans.property.SimpleObjectProperty;

public class FrameSummedSpectrumProvider implements PlotXYDataProvider {

  protected final NumberFormat rtFormat;
  protected final NumberFormat mzFormat;
  protected final NumberFormat mobilityFormat;
  protected final NumberFormat intensityFormat;
  protected final UnitFormat unitFormat;
  private final Frame frame;
  private DataPoint[] dataPoints;

  private double finishedPercentage;

  public FrameSummedSpectrumProvider(Frame frame) {
    this.frame = frame;
    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();

    finishedPercentage = 0d;
  }

  @Override
  public String getLabel(int index) {
    return mzFormat.format(dataPoints[index].getMZ());
  }

  @Override
  public Color getAWTColor() {
    return frame.getDataFile().getColorAWT();
  }

  @Override
  public javafx.scene.paint.Color getFXColor() {
    return frame.getDataFile().getColor();
  }

  @Override
  public Comparable<?> getSeriesKey() {
    return frame.getDataFile().getName() + " - Frame " + frame.getFrameId() + " "
        + rtFormat.format(frame.getRetentionTime()) + " min";
  }

  @Override
  public String getToolTipText(int itemIndex) {
    return "Frame #" + frame.getFrameId() + "RT " + frame.getRetentionTime() + "\nm/z "
        + mzFormat.format(dataPoints[itemIndex].getMZ()) + "\nIntensity "
        + intensityFormat.format(dataPoints[itemIndex].getIntensity());
  }

  @Override
  public void computeValues(SimpleObjectProperty<TaskStatus> status) {
    dataPoints = ScanUtils.extractDataPoints(frame);
  }

  @Override
  public double getDomainValue(int index) {
    return dataPoints[index].getMZ();
  }

  @Override
  public double getRangeValue(int index) {
    return dataPoints[index].getIntensity();
  }

  @Override
  public int getValueCount() {
    return dataPoints.length;
  }

  @Override
  public double getComputationFinishedPercentage() {
    return 1;
  }
}