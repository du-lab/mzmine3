package io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.javafx.FxColorUtil;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.stream.Stream;
import javafx.beans.property.SimpleObjectProperty;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MassSpectrumProvider implements PlotXYDataProvider {

  private final NumberFormat mzFormat;
  private final String seriesKey;
  private final MassSpectrum spectrum;
  private final Color color;

  public MassSpectrumProvider(MassSpectrum spectrum, String seriesKey, Color color) {
    this.spectrum = spectrum;
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    this.seriesKey = seriesKey;
    this.color = color;
  }

  public MassSpectrumProvider(double[] mzs, double[] intensities, String seriesKey) {
    this(mzs, intensities, seriesKey,
        MZmineCore.getConfiguration().isDarkMode() ? Color.lightGray : Color.black);
  }

  public MassSpectrumProvider(double[] mzs, double[] intensities, String seriesKey, Color color) {
    this.color = color;
    this.spectrum = new MassSpectrum() {
      @Override
      public int getNumberOfDataPoints() {
        return mzs.length;
      }

      @Override
      public MassSpectrumType getSpectrumType() {
        return null;
      }

      @Override
      public double[] getMzValues(@Nonnull double[] dst) {
        return new double[0]; // Local implementation only so this does not matter
      }

      @Override
      public double[] getIntensityValues(@Nonnull double[] dst) {
        return new double[0]; // Local implementation only so this does not matter
      }

      @Override
      public double getMzValue(int index) {
        return mzs[index];
      }

      @Override
      public double getIntensityValue(int index) {
        return intensities[index];
      }

      @Nullable
      @Override
      public Double getBasePeakMz() {
        return null;
      }

      @Nullable
      @Override
      public Double getBasePeakIntensity() {
        return null;
      }

      @Nullable
      @Override
      public Integer getBasePeakIndex() {
        return null;
      }

      @Nullable
      @Override
      public Range<Double> getDataPointMZRange() {
        return null;
      }

      @Nullable
      @Override
      public Double getTIC() {
        return null;
      }

      @Override
      public Stream<DataPoint> stream() {
        return null;
      }

      @Nonnull
      @Override
      public Iterator<DataPoint> iterator() {
        return null;
      }
    };

    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    this.seriesKey = seriesKey;
  }

  @Nonnull
  @Override
  public Color getAWTColor() {
    return color;
  }

  @Nonnull
  @Override
  public javafx.scene.paint.Color getFXColor() {
    return FxColorUtil.awtColorToFX(color);
  }

  @Nullable
  @Override
  public String getLabel(int index) {
    return mzFormat.format(spectrum.getMzValue(index));
  }

  @Nonnull
  @Override
  public Comparable<?> getSeriesKey() {
    return seriesKey;
  }

  @Nullable
  @Override
  public String getToolTipText(int itemIndex) {
    return null;
  }

  @Override
  public void computeValues(SimpleObjectProperty<TaskStatus> status) {

  }

  @Override
  public double getDomainValue(int index) {
    return spectrum.getMzValue(index);
  }

  @Override
  public double getRangeValue(int index) {
    return spectrum.getIntensityValue(index);
  }

  @Override
  public int getValueCount() {
    return spectrum.getNumberOfDataPoints();
  }

  @Override
  public double getComputationFinishedPercentage() {
    return 1d;
  }
}
