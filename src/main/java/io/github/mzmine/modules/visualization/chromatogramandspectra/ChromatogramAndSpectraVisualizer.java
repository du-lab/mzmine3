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

package io.github.mzmine.modules.visualization.chromatogramandspectra;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.chromatogram.ChromatogramCursorPosition;
import io.github.mzmine.modules.visualization.chromatogram.FeatureDataSet;
import io.github.mzmine.modules.visualization.chromatogram.TICDataSet;
import io.github.mzmine.modules.visualization.chromatogram.TICPlot;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.modules.visualization.rawdataoverview.RawDataOverviewWindowController;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectrumCursorPosition;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.ScanDataSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.BasicStroke;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javafx.beans.NamedArg;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jfree.chart.plot.ValueMarker;

public class ChromatogramAndSpectraVisualizer extends SplitPane {

  private final NumberFormat mzFormat;
  private final NumberFormat rtFormat;

  public static final Logger logger = Logger
      .getLogger(ChromatogramAndSpectraVisualizer.class.getName());

  private static final BasicStroke MARKER_STROKE = new BasicStroke(2.0f);

  protected FlowPane pnSpectrumControls;
  protected ChromatogramPlotControlPane pnChromControls;

  protected final TICPlot chromPlot;
  protected final SpectraPlot spectrumPlot;
  protected ValueMarker rtMarker;
  protected ValueMarker mzMarker;

  protected boolean showSpectraOfEveryRawFile;


  protected final ObjectProperty<ScanSelection> scanSelection;
  /**
   * Type of chromatogram to be displayed. This is bound bidirectional to the {@link
   * TICPlot#plotTypeProperty()} and the {@link ChromatogramPlotControlPane#cbPlotType#plotTypeProperty()}
   */
  protected final ObjectProperty<TICPlotType> plotType;

  /**
   * Current position of the crosshair in the chromatogram plot. Changes to the position should be
   * reflected in the {@link ChromatogramAndSpectraVisualizer#spectrumPlot}.
   */
  protected final ObjectProperty<ChromatogramCursorPosition> chromPosition;

  /**
   * Current position of the crosshair in the spectrum plot. Changes in the position update the
   * {@link ChromatogramAndSpectraVisualizer#chromPlot} via {@link ChromatogramAndSpectraVisualizer#onSpectrumSelectionChanged(ObservableValue,
   * SpectrumCursorPosition, SpectrumCursorPosition)}.
   */
  protected final ObjectProperty<SpectrumCursorPosition> spectrumPosition;

  /**
   * Tolerance range for the feature chromatograms of the base peak in the selected scan. Listener
   * calls {@link ChromatogramAndSpectraVisualizer#updateFeatureDataSets(double)}.
   */
  protected final ObjectProperty<MZTolerance> bpcChromTolerance;

  /**
   * Tolerance for the generation of the TICDataset. If set to null, the whole m/z range is
   * displayed. To display extracted ion chromatograms set the plotType to {@link TICPlotType#TIC}
   * and select a m/z range.
   */
  protected final ObjectProperty<Range<Double>> mzRange;

  /**
   * Stores the raw data files ands tic data sets currently displayed. Could be observed by a
   * listener in the future, if needed.
   */
  protected ObservableMap<RawDataFile, TICDataSet> filesAndDataSets;

  protected SpectraDataSetCalc currentSpectraDataSetCalc;
  protected FeatureDataSetCalc currentFeatureDataSetCalc;

  public ChromatogramAndSpectraVisualizer() {
    this(Orientation.HORIZONTAL);
  }

  public ChromatogramAndSpectraVisualizer(@NamedArg("orientation") Orientation orientation) {
    super();

    getStyleClass().add("region-match-chart-bg");

    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    rtFormat = MZmineCore.getConfiguration().getRTFormat();

    filesAndDataSets = FXCollections.synchronizedObservableMap(FXCollections.observableMap(
        new Hashtable<RawDataFile, TICDataSet>()));

    setOrientation(orientation);
    showSpectraOfEveryRawFile = true;

    // initialise properties
    plotType = new SimpleObjectProperty<>();
    bpcChromTolerance = new SimpleObjectProperty<>(new MZTolerance(0.001, 10));
    chromPosition = new SimpleObjectProperty<>();
    spectrumPosition = new SimpleObjectProperty<>();
    scanSelection = new SimpleObjectProperty<>(new ScanSelection(1));
    mzRange = new SimpleObjectProperty<>();

    // initialise controls
    pnChromControls = new ChromatogramPlotControlPane();
    pnSpectrumControls = new FlowPane();
    chromPlot = new TICPlot();
    spectrumPlot = new SpectraPlot();
    BorderPane pnWrapSpectrum = new BorderPane();
    BorderPane pnWrapChrom = new BorderPane();
    pnWrapChrom.setCenter(chromPlot);
    pnWrapChrom.setBottom(pnChromControls);
    pnWrapSpectrum.setCenter(spectrumPlot);
    pnWrapSpectrum.setBottom(pnSpectrumControls);
    getItems().addAll(pnWrapChrom, pnWrapSpectrum);

    chromPlot.setLabelColorMatch(true);
    spectrumPlot.setLabelColorMatch(true);

    // property bindings
    plotType.bindBidirectional(chromPlot.plotTypeProperty());
    plotType.bindBidirectional(pnChromControls.getCbPlotType().valueProperty());
    chromPosition.bindBidirectional(chromPlot.cursorPositionProperty());
    spectrumPosition.bindBidirectional(spectrumPlot.cursorPositionProperty());
    mzRange.bindBidirectional(pnChromControls.mzRange);

    // property listeners
    plotType.addListener(((observable, oldValue, newValue) -> {
      chromPlot.removeAllDataSets(false);
      updateAllChromatogramDataSets();
    }));

    // update feature data sets if the tolerance for the extraction changes
    bpcChromToleranceProperty().addListener(
        (obs, old, val) -> updateFeatureDataSets(
            getChromPosition().getDataFile().getScan(getChromPosition().getScanNumber())
                .getHighestDataPoint().getMZ()));

    // update spectrum plot if the user clicks in chromatogram plot
    chromPositionProperty()
        .addListener((obs, old, pos) -> onChromatogramSelectionChanged(obs, old, pos));

    spectrumPositionProperty()
        .addListener(((obs, old, pos) -> {
          onSpectrumSelectionChanged(obs, old, pos);
        }));

    // update chromatogram plot if the ScanSelection changes
    scanSelectionProperty().addListener((obs, old, val) -> updateAllChromatogramDataSets());

    pnChromControls.getBtnUpdateXIC().setOnAction(event -> {
      if (mzRange.getValue() != null && getPlotType() == TICPlotType.TIC) {
        updateAllChromatogramDataSets();
      }
    });

    chromPlot.getXYPlot().setDomainCrosshairVisible(false);
    chromPlot.getXYPlot().setRangeCrosshairVisible(false);
  }

  private void updateAllChromatogramDataSets() {
    List<RawDataFile> rawDataFiles = new ArrayList<>();
    filesAndDataSets.keySet().forEach(raw -> rawDataFiles.add(raw));
    chromPlot.getXYPlot().setNotify(false);
    chromPlot.getXYPlot().clearDomainMarkers();
    rawDataFiles.forEach(raw -> removeRawDataFile(raw));
    rawDataFiles.forEach(raw -> addRawDataFile(raw));
    chromPlot.getXYPlot().setNotify(true);
    chromPlot.getChart().fireChartChanged();
  }

  /**
   * @return The raw data files currently visualised.
   */
  @Nonnull
  public Collection<RawDataFile> getRawDataFiles() {
    return filesAndDataSets.keySet();
  }

  /**
   * Sets the raw data files to be displayed. Already present files are not removed to optimise
   * performance. This should be called over {@link RawDataOverviewWindowController#addRawDataFileTab}
   * if possible.
   *
   * @param rawDataFiles
   */
  public void setRawDataFiles(@Nonnull Collection<RawDataFile> rawDataFiles) {
    // remove files first
    List<RawDataFile> filesToProcess = new ArrayList<>();
    for (RawDataFile rawDataFile : filesAndDataSets.keySet()) {
      if (!rawDataFiles.contains(rawDataFile)) {
        filesToProcess.add(rawDataFile);
      }
    }
    filesToProcess.forEach(r -> removeRawDataFile(r));

    // presence of file is checked in the add method
    rawDataFiles.forEach(r -> addRawDataFile(r));
  }

  /**
   * Adds a raw data file to the chromatogram plot.
   *
   * @param rawDataFile
   */
  public void addRawDataFile(@Nonnull final RawDataFile rawDataFile) {

    if (filesAndDataSets.keySet().contains(rawDataFile)) {
      logger.fine("Raw data file " + rawDataFile.getName() + " already displayed.");
      return;
    }

    final Scan[] scans = getScanSelection().getMatchingScans(rawDataFile);
    if (scans.length == 0) {
      MZmineCore.getDesktop().displayErrorMessage("No scans found.");
      return;
    }

    Range<Double> rawMZRange =
        (getMzRange() != null && pnChromControls.cbXIC.isSelected()
            && plotType.getValue() == TICPlotType.TIC) ? getMzRange()
            : rawDataFile.getDataMZRange();

    TICDataSet ticDataset = new TICDataSet(rawDataFile, scans, rawMZRange, null, getPlotType());
    filesAndDataSets.put(rawDataFile, ticDataset);
    chromPlot.addTICDataSet(ticDataset, rawDataFile.getColorAWT());

    logger.fine("Added raw data file " + rawDataFile.getName());
  }

  /**
   * Removes a raw data file and it's features from the chromatogram and spectrum plot.
   *
   * @param file The raw data file
   */
  public void removeRawDataFile(@Nonnull final RawDataFile file) {
    logger.fine("Removing raw data file " + file.getName());
    TICDataSet dataset = filesAndDataSets.get(file);
    chromPlot.getXYPlot().setDataset(chromPlot.getXYPlot().indexOf(dataset), null);
    chromPlot.removeFeatureDataSetsOfFile(file);
    filesAndDataSets.remove(file);
  }

  /**
   * Called by a listener to the currentPostion to update the spectraPlot accordingly. The listener
   * is triggered by a change to the {@link ChromatogramAndSpectraVisualizer#chromPosition}
   * property.
   */
  private void onChromatogramSelectionChanged(
      ObservableValue<? extends ChromatogramCursorPosition> obs,
      ChromatogramCursorPosition old,
      ChromatogramCursorPosition pos) {
    RawDataFile file = pos.getDataFile();

    updateChromatogramDomainMarker(pos);
    // update feature data sets
    updateFeatureDataSets(file.getScan(pos.getScanNumber()).getHighestDataPoint().getMZ());
    // update spectrum plots
    updateSpectraPlot(filesAndDataSets.keySet(), pos);
  }

  /**
   * Called by changes to {@link ChromatogramAndSpectraVisualizer#spectrumPosition}.
   *
   * @param obs
   * @param old
   * @param pos
   */
  private void onSpectrumSelectionChanged(ObservableValue<? extends SpectrumCursorPosition> obs,
      SpectrumCursorPosition old,
      SpectrumCursorPosition pos) {
    updateSpectrumDomainMarker(pos);
    mzRangeProperty().set(getBpcChromTolerance().getToleranceRange(pos.getMz()));
    updateFeatureDataSets(pos.getMz());
  }

  /**
   * Changes the position of the domain marker. Is called by the mouse listener initialized in
   * {@link ChromatogramAndSpectraVisualizer#onChromatogramSelectionChanged(ObservableValue,
   * ChromatogramCursorPosition, ChromatogramCursorPosition)}
   *
   * @param pos
   */
  private void updateChromatogramDomainMarker(@Nonnull ChromatogramCursorPosition pos) {
    chromPlot.getXYPlot().clearDomainMarkers();

    if (rtMarker == null) {
      rtMarker = new ValueMarker(
          pos.getDataFile().getScan(pos.getScanNumber()).getRetentionTime());
      rtMarker.setStroke(MARKER_STROKE);
    } else {
      rtMarker.setValue(pos.getDataFile().getScan(pos.getScanNumber()).getRetentionTime());
    }
    rtMarker.setPaint(MZmineCore.getConfiguration().getDefaultColorPalette().getNeutralColorAWT());

    chromPlot.getXYPlot().addDomainMarker(rtMarker);
  }

  private void updateSpectrumDomainMarker(@Nonnull SpectrumCursorPosition pos) {
    spectrumPlot.getXYPlot().clearDomainMarkers();

    if (mzMarker == null) {
      mzMarker = new ValueMarker(pos.getMz());
      mzMarker.setStroke(MARKER_STROKE);
    } else {
      mzMarker.setValue(pos.getMz());
    }
    mzMarker.setPaint(MZmineCore.getConfiguration().getDefaultColorPalette().getNeutralColorAWT());

    spectrumPlot.getXYPlot().addDomainMarker(mzMarker);
  }

  /**
   * Sets a single scan into the spectrum plot. Triggers {@link ChromatogramAndSpectraVisualizer#chromPositionProperty()}'s
   * listeners.
   *
   * @param rawDataFile The rawDataFile to focus.
   * @param scanNum     The scan number.
   */
  public void setFocusedScan(@Nonnull RawDataFile rawDataFile, int scanNum) {
    if (!filesAndDataSets.keySet().contains(rawDataFile) || rawDataFile.getScan(scanNum) == null) {
      return;
    }
    ChromatogramCursorPosition pos = new ChromatogramCursorPosition(
        rawDataFile.getScan(scanNum).getRetentionTime(), 0, 0,
        rawDataFile, scanNum);
    setChromPosition(pos);
  }

  /**
   * Forces a single scan in the spectrum plot without notifying the listener of the {@link
   * ChromatogramAndSpectraVisualizer#chromPosition}.
   *
   * @param rawDataFile The raw data file
   * @param scanNum     The number of the scan
   */
  private void forceScanDataSet(@Nonnull RawDataFile rawDataFile, int scanNum) {
    spectrumPlot.removeAllDataSets();
    ScanDataSet dataSet = new ScanDataSet(rawDataFile.getScan(scanNum));
    spectrumPlot.addDataSet(dataSet, rawDataFile.getColorAWT(), false);
    spectrumPlot.setTitle(rawDataFile.getName() + "(#" + scanNum + ")", "");
  }

  /**
   * This can be called from the outside to focus a specific scan without triggering {@link
   * ChromatogramAndSpectraVisualizer#chromPositionProperty()}'s listeners. Use with care.
   *
   * @param rawDataFile The rawDataFile to focus.
   * @param scanNum     The scan number.
   */
  public void setFocusedScanSilent(@Nonnull RawDataFile rawDataFile, int scanNum) {
    if (!filesAndDataSets.keySet().contains(rawDataFile) || rawDataFile.getScan(scanNum) == null) {
      return;
    }
    ChromatogramCursorPosition pos = new ChromatogramCursorPosition(
        rawDataFile.getScan(scanNum).getRetentionTime(), 0, 0,
        rawDataFile, scanNum);
    updateChromatogramDomainMarker(pos);
    forceScanDataSet(rawDataFile, scanNum);
  }

  public TICPlot getChromPlot() {
    return chromPlot;
  }


  public SpectraPlot getSpectrumPlot() {
    return spectrumPlot;
  }

  // ----- Plot updaters -----

  /**
   * Calculates {@link FeatureDataSet}s for the given m/z range.
   *
   * @param mz
   */
  private void updateFeatureDataSets(double mz) {
    // mz of the base peak in the selected scan of the selected raw data file.
    Range<Double> bpcChromToleranceRange = getBpcChromTolerance().getToleranceRange(mz);
    FeatureDataSetCalc thread = new FeatureDataSetCalc(filesAndDataSets.keySet(),
        bpcChromToleranceRange, getScanSelection(), getChromPlot());

    thread.addTaskStatusListener((task, newStatus, oldStatus) -> {
      logger
          .finest("FeatureUpdate status changed from " + oldStatus.toString() + " to " + newStatus
              .toString());
      currentFeatureDataSetCalc = null;
    });
    if (currentFeatureDataSetCalc != null) {
      currentFeatureDataSetCalc.setStatus(TaskStatus.CANCELED);
    }
    currentFeatureDataSetCalc = thread;
    MZmineCore.getTaskController()
        .addTask(thread);
  }

  /**
   * Updates the {@link ChromatogramAndSpectraVisualizer#spectrumPlot} with the scan data sets of
   * the currently selected retention time in the {@link ChromatogramAndSpectraVisualizer#chromPlot}.
   *
   * @param rawDataFiles The raw data files in the chromatogram plot.
   * @param pos          the currently selected {@link ChromatogramCursorPosition}.
   */
  private void updateSpectraPlot(@Nonnull Collection<RawDataFile> rawDataFiles,
      @Nonnull ChromatogramCursorPosition pos) {
    SpectraDataSetCalc thread = new SpectraDataSetCalc(rawDataFiles,
        pos, getScanSelection(), showSpectraOfEveryRawFile, getSpectrumPlot());

    thread.addTaskStatusListener((task, newStatus, oldStatus) -> {
      logger
          .finest("SpectraUpdate status changed from " + oldStatus.toString() + " to " + newStatus
              .toString());
      currentSpectraDataSetCalc = null;
    });

    if (currentSpectraDataSetCalc != null) {
      currentSpectraDataSetCalc.setStatus(TaskStatus.CANCELED);
    }
    currentSpectraDataSetCalc = thread;
    getSpectrumPlot().getXYPlot().clearDomainMarkers();
    MZmineCore.getTaskController().addTask(thread);
  }

  // ----- Property getters and setters -----

  /**
   * Changes the plot type. Also recalculates the all data sets if changed from BPC to TIC.
   *
   * @param plotType The new plot type.
   */
  public void setPlotType(@Nonnull TICPlotType plotType) {
    this.plotType.set(plotType);
  }

  @Nonnull
  public TICPlotType getPlotType() {
    return plotType.get();
  }

  @Nonnull
  public ObjectProperty<TICPlotType> plotTypeProperty() {
    return plotType;
  }

  @Nonnull
  public ObjectProperty<ChromatogramCursorPosition> chromPositionProperty() {
    return chromPosition;
  }

  private void setChromPosition(@Nonnull ChromatogramCursorPosition chromPosition) {
    this.chromPosition.set(chromPosition);
  }

  @Nonnull
  public ChromatogramCursorPosition getChromPosition() {
    return chromPosition.get();
  }

  public SpectrumCursorPosition getSpectrumPosition() {
    return spectrumPosition.get();
  }

  public ObjectProperty<SpectrumCursorPosition> spectrumPositionProperty() {
    return spectrumPosition;
  }

  public void setSpectrumPosition(SpectrumCursorPosition spectrumPosition) {
    this.spectrumPosition.set(spectrumPosition);
  }

  /**
   * To listen to changes in the selected raw data file, use {@link ChromatogramAndSpectraVisualizer#chromPositionProperty#addListener}.
   *
   * @return Returns the currently selected raw data file. Could be null.
   */
  @Nullable
  public RawDataFile getSelectedRawDataFile() {
    ChromatogramCursorPosition pos = getChromPosition();
    return (pos == null) ? null : pos.getDataFile();
  }

  @Nullable
  public Range<Double> getMzRange() {
    return mzRange.get();
  }

  public void setMzRange(@Nullable Range<Double> mzRange) {
    this.mzRange.set(mzRange);
  }

  @Nonnull
  public ObjectProperty<Range<Double>> mzRangeProperty() {
    return mzRange;
  }

  /**
   * Sets the scan selection. Also updates all data sets in the chromatogram plot accordingly.
   *
   * @param selection The new scan selection.
   */
  public void setScanSelection(@Nonnull ScanSelection selection) {
    scanSelection.set(selection);
  }

  @Nonnull
  public ScanSelection getScanSelection() {
    return scanSelection.get();
  }

  @Nonnull
  public ObjectProperty<ScanSelection> scanSelectionProperty() {
    return scanSelection;
  }

  @Nonnull
  public MZTolerance getBpcChromTolerance() {
    return bpcChromTolerance.get();
  }

  @Nonnull
  public ObjectProperty<MZTolerance> bpcChromToleranceProperty() {
    return bpcChromTolerance;
  }

  public void setBpcChromTolerance(@Nonnull MZTolerance bpcChromTolerance) {
    this.bpcChromTolerance.set(bpcChromTolerance);
  }


  // ----- Object method overrides -----
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ChromatogramAndSpectraVisualizer)) {
      return false;
    }
    ChromatogramAndSpectraVisualizer that = (ChromatogramAndSpectraVisualizer) o;
    return showSpectraOfEveryRawFile == that.showSpectraOfEveryRawFile &&
        chromPlot.equals(that.chromPlot) &&
        spectrumPlot.equals(that.spectrumPlot) &&
        Objects.equals(scanSelection.get(), that.scanSelection.get()) &&
        Objects.equals(mzRange.get(), that.mzRange.get()) &&
        Objects.equals(chromPosition.get(), that.chromPosition.get()) &&
        Objects.equals(rtMarker, that.rtMarker) &&
        bpcChromTolerance.get().equals(that.bpcChromTolerance.get()) &&
        Objects.equals(filesAndDataSets, that.filesAndDataSets);
  }

  @Override
  public int hashCode() {
    return Objects.hash(chromPlot, spectrumPlot, scanSelection, mzRange, chromPosition,
        showSpectraOfEveryRawFile, rtMarker, bpcChromTolerance, filesAndDataSets);
  }
}
