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

package io.github.mzmine.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.Mobilogram;
import io.github.mzmine.datamodel.impl.MobilityDataPoint;
import io.github.mzmine.datamodel.impl.SimpleMobilogram;
import io.github.mzmine.util.scans.ScanUtils;

public class MobilogramUtils {

  /**
   * If the number of consecutively empty scans exceeds this constant, it's interpreted as a
   * decrease to 0 intensity.
   */
  public static final int MIN_GAP = 1;

  /**
   * @return a list of the added data points.
   */
  public static List<MobilityDataPoint> fillMissingScanNumsWithZero(SimpleMobilogram mobilogram) {
    List<MobilityDataPoint> dataPoints = mobilogram.getDataPoints();
    if (dataPoints.size() <= 3) {
      return Collections.emptyList();
    }

    // find smallest mobility distance between two points
    // get two dp
    double minDist = getMobilityStepSize(mobilogram);

    List<MobilityDataPoint> newDps = new ArrayList<>();
    MobilityDataPoint dp = dataPoints.stream().findFirst().get();
    int nextScanNum = dp.getScanNum() + 1;
    double lastMobility = dp.getMobility();
    Iterator<MobilityDataPoint> iterator = dataPoints.iterator();

    if (iterator.hasNext()) {
      iterator.next();
    }
    while (iterator.hasNext()) {
      MobilityDataPoint nextDp = iterator.next();

      while (nextDp.getScanNum() != nextScanNum) {
        MobilityDataPoint newDp =
            new MobilityDataPoint(mobilogram.getMZ(), 0.0d, lastMobility - minDist, nextScanNum);
        newDps.add(newDp);
        nextScanNum++;
        lastMobility -= minDist;
      }
      lastMobility = nextDp.getMobility();
      nextScanNum = nextDp.getScanNum() + 1;
    }
    newDps.forEach(mobilogram::addDataPoint);
    mobilogram.calc();
    return newDps;
  }


  /**
   * Manually adds a 0 intensity data point to the edges of this mobilogram, if a gap of more than
   * minGap scans is detected.
   *
   * @param minGap
   */
  private static void fillEdgesWithZeros(SimpleMobilogram mobilogram, int minGap) {
    List<MobilityDataPoint> dataPoints = mobilogram.getDataPoints();
    List<MobilityDataPoint> newDataPoints = new ArrayList<>();
    final double minStep = getMobilityStepSize(mobilogram);

    for (MobilityDataPoint dp : dataPoints) {
      final int gap = getNumberOfConsecutiveEmptyScans(mobilogram, dp.getScanNum());
      if (gap > minGap) {
        MobilityDataPoint firstDp = new MobilityDataPoint(mobilogram.getMZ(), 0.0,
            dp.getMobility() - minStep, dp.getScanNum() + 1);
        MobilityDataPoint lastDp = new MobilityDataPoint(mobilogram.getMZ(), 0.0,
            dp.getMobility() - minStep * (gap - 1), dp.getScanNum() + gap - 1);
        newDataPoints.add(firstDp);
        newDataPoints.add(lastDp);
      }
    }
    newDataPoints.forEach(mobilogram::addDataPoint);
    mobilogram.calc();
  }

  public static int getNumberOfConsecutiveEmptyScans(Mobilogram mobilogram, int startScanNum) {
    return getNextAvailableScanNumber(mobilogram, startScanNum) - startScanNum;
  }

  public static int getNextAvailableScanNumber(Mobilogram mobilogram, int startScanNum) {
    boolean foundStartKey = false;
    List<MobilityDataPoint> dataPoints = mobilogram.getDataPoints().stream()
        .sorted(Comparator.comparingInt(MobilityDataPoint::getScanNum))
        .collect(Collectors.toList());

    for (int i = 0; i < dataPoints.size(); i++) {
      int scanNum = dataPoints.get(i).getScanNum();
      if (scanNum == startScanNum) {
        foundStartKey = true;
        continue;
      }
      if (foundStartKey) {
        return scanNum;
      }
    }
    return dataPoints.get(dataPoints.size() - 1).getScanNum();
  }

  private static double getMobilityStepSize(Mobilogram mobilogram) {
    List<MobilityDataPoint> dataPoints = mobilogram.getDataPoints().stream()
        .sorted(Comparator.comparingInt(MobilityDataPoint::getScanNum))
        .collect(Collectors.toList());
    // find smallest mobility distance between two points get two dp
    MobilityDataPoint aDp = null;
    for (MobilityDataPoint dp : dataPoints) {
      if (aDp == null) {
        aDp = dp;
      } else {
        return Math
            .abs((aDp.getMobility() - dp.getMobility()) / (aDp.getScanNum() - dp.getScanNum()));
      }
    }
    return 0;
  }

  public static SimpleMobilogram removeZeroIntensityDataPoints(Mobilogram mobilogram) {
    SimpleMobilogram newMobilogram =
        new SimpleMobilogram(mobilogram.getMobilityType(), mobilogram.getRawDataFile());

    for (MobilityDataPoint dp : mobilogram.getDataPoints()) {
      if (Double.compare(dp.getIntensity(), 0) == 0.0d) {
        newMobilogram.addDataPoint(dp);
      }
    }

    fillEdgesWithZeros(newMobilogram, MIN_GAP);
    newMobilogram.calc();
    return newMobilogram;
  }

  /**
   * Builds a Mobilogram for the selected Frames. Should not be used to build multiple mobilograms
   * du to lower performance than the
   * {@link io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder.MobilogramBuilderTask}.
   *
   * @param frames
   * @param mzRange
   * @return
   */
  @Nullable
  public static SimpleMobilogram buildMobilogramForMzRange(Collection<Frame> frames,
      Range<Double> mzRange) {
    Frame anyFrame = frames.stream().findAny().orElse(null);
    if (anyFrame == null) {
      throw new IllegalArgumentException(
          "Collection of frames was empty. Cannot build mobilogram.");
    }
    SimpleMobilogram mobilogram =
        new SimpleMobilogram(anyFrame.getMobilityType(), (IMSRawDataFile) anyFrame.getDataFile());

    // collect all eligible data points from all frame-subscans
    Map<Integer, Set<DataPoint>> eligibleDataPoints = new HashMap<>(); // k = subscan number
    for (Frame frame : frames) {
      for (MobilityScan scan : frame.getMobilityScans()) {
        Set<DataPoint> dpSet = eligibleDataPoints.computeIfAbsent(scan.getMobilityScanNumber(),
            key -> new HashSet<>());
        // DataPoint[] dps = scan.getDataPoints();
        // Arrays.sort(dps, Comparator.comparingDouble(DataPoint::getMZ));
        DataPoint[] foundDps =
            ScanUtils.selectDataPointsByMass(ScanUtils.extractDataPoints(scan), mzRange);
        Collections.addAll(dpSet, foundDps);
      }
    }

    if (eligibleDataPoints.size() == 0) {
      return null;
    }

    for (var entry : eligibleDataPoints.entrySet()) {
      if (entry.getValue().isEmpty()) {
        continue;
      }
      double intensity = entry.getValue().stream().mapToDouble(DataPoint::getIntensity).sum();
      double mz = entry.getValue().stream().mapToDouble(DataPoint::getMZ).average().getAsDouble();
      mobilogram.addDataPoint(new MobilityDataPoint(mz, intensity,
          anyFrame.getMobilityForMobilityScanNumber(entry.getKey()), entry.getKey()));
    }
    if (mobilogram.getDataPoints().isEmpty()) {
      return null;
    }
    mobilogram.calc();
    fillMissingScanNumsWithZero(mobilogram);
    return mobilogram;
  }
}
