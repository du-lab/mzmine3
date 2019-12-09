/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.id_sirius;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.MassListComponent;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

public class SiriusProcessingModule implements MZmineProcessingModule {

  private static final String MODULE_NAME = "SIRIUS structure prediction";
  private static final String MODULE_DESCRIPTION = "Sirius identification method.";

  @Override
  public @Nonnull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @Nonnull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  @Nonnull
  public ExitCode runModule(@Nonnull MZmineProject project, @Nonnull ParameterSet parameters,
      @Nonnull Collection<Task> tasks) {

    final PeakList[] peakLists = parameters.getParameter(PeakListIdentificationParameters.peakLists)
        .getValue().getMatchingPeakLists();
    for (final PeakList peakList : peakLists) {
      Task newTask = new PeakListIdentificationTask(parameters, peakList);
      tasks.add(newTask);
    }

    return ExitCode.OK;
  }

  /**
   * Show dialog for identifying a single peak-list row.
   *
   * @param row the feature list row.
   */
  public static void showSingleRowIdentificationDialog(final PeakListRow row) {

    final ParameterSet parameters = new SingleRowIdentificationParameters();

    // Set m/z.
    parameters.getParameter(SingleRowIdentificationParameters.ION_MASS)
        .setValue(row.getAverageMZ());


    if (parameters.showSetupDialog(MZmineCore.getDesktop().getMainWindow(), true) == ExitCode.OK) {
      String massListName = parameters.getParameter(SingleRowIdentificationParameters.MASS_LIST).getValue();
      List<String> massLists = MassListComponent.getMassListNames();

      int fingerCandidates, siriusCandidates, timer;
      timer = parameters.getParameter(SingleRowIdentificationParameters.SIRIUS_TIMEOUT).getValue();
      siriusCandidates =
          parameters.getParameter(SingleRowIdentificationParameters.SIRIUS_CANDIDATES).getValue();
      fingerCandidates =
          parameters.getParameter(SingleRowIdentificationParameters.FINGERID_CANDIDATES).getValue();

      if (timer <= 0 || siriusCandidates <= 0 || fingerCandidates <= 0) {
        MZmineCore.getDesktop().displayErrorMessage(MZmineCore.getDesktop().getMainWindow(),
            "Sirius parameters can't be negative");
      } else if (!massLists.contains(massListName)) {
        MZmineCore.getDesktop().displayErrorMessage(MZmineCore.getDesktop().getMainWindow(),
            "Mass List parameter", String.format("Mass List parameter is set wrong [%s]", massListName));
      } else {     // Run task.
        MZmineCore.getTaskController()
            .addTask(new SingleRowIdentificationTask(parameters.cloneParameterSet(), row));
      }
    }
  }

  @Override
  public @Nonnull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.IDENTIFICATION;
  }

  @Override
  public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
    return PeakListIdentificationParameters.class;
  }
}
