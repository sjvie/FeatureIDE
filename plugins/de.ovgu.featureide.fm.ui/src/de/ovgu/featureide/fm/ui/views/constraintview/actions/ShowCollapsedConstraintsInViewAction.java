/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2019  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 *
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://featureide.cs.ovgu.de/ for further information.
 */
package de.ovgu.featureide.fm.ui.views.constraintview.actions;

import static de.ovgu.featureide.fm.core.localization.StringTable.SHOW_COLLAPSED_CONSTRAINTS;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import de.ovgu.featureide.fm.ui.FMUIPlugin;
import de.ovgu.featureide.fm.ui.editors.IGraphicalFeatureModel;
import de.ovgu.featureide.fm.ui.editors.featuremodel.operations.FeatureModelOperationWrapper;
import de.ovgu.featureide.fm.ui.editors.featuremodel.operations.ShowCollapsedConstraintsOperation;
import de.ovgu.featureide.fm.ui.views.constraintview.ConstraintViewController;
import de.ovgu.featureide.fm.ui.views.constraintview.view.ConstraintViewSettingsMenu;

/**
 * A modified ShowCollapsedConstraintsAction for the ConstraintView
 *
 * @author Domenik Eichhorn
 */
public class ShowCollapsedConstraintsInViewAction extends Action {

	private static final Image SHOW_HIDE_IMAGE = FMUIPlugin.getImage("collapse.gif");
	private final ConstraintViewController controller;
	private final ConstraintViewSettingsMenu settingsMenu;

	public ShowCollapsedConstraintsInViewAction(ConstraintViewController controller, ConstraintViewSettingsMenu constraintViewSettingsMenu) {
		super(SHOW_COLLAPSED_CONSTRAINTS, ImageDescriptor.createFromImage(SHOW_HIDE_IMAGE));
		this.controller = controller;
		settingsMenu = constraintViewSettingsMenu;
	}

	@Override
	public void run() {
		final IGraphicalFeatureModel graphicalFeatureModel = controller.getFeatureModelEditor().diagramEditor.getGraphicalFeatureModel();
		FeatureModelOperationWrapper.run(new ShowCollapsedConstraintsOperation(graphicalFeatureModel));
		controller.getView().refresh();
		settingsMenu.setShowCollapsedConstraintsInViewActionImage(graphicalFeatureModel.getLayout().showCollapsedConstraints());
	}
}
