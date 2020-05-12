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
package de.ovgu.featureide.fm.ui.views.constraintview.listener;

import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;

import de.ovgu.featureide.fm.ui.editors.FeatureModelEditor;
import de.ovgu.featureide.fm.ui.views.constraintview.ConstraintViewController;

/**
 * This class is the implementation of the IPartListener2 for the ConstraintView.
 *
 * @author Rosiak Kamil
 */
public class ConstraintViewPartListener implements IPartListener2 {

	private final ConstraintViewController controller;

	public ConstraintViewPartListener(ConstraintViewController cvc) {
		controller = cvc;
	}

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {}

	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {}

	@Override
	public void partActivated(IWorkbenchPartReference partRef) {
		final IWorkbenchPart activePart = partRef.getPart(false);
		if (activePart instanceof FeatureModelEditor) {
			controller.switchToFeatureModelEditor((FeatureModelEditor) activePart);
		}
	}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {
		final IWorkbenchPart hiddenPart = partRef.getPart(false);
		if (hiddenPart instanceof FeatureModelEditor) {

			// remove the editor from the constraints view only if the currently relevant editor is now hidden
			if (hiddenPart.equals(controller.getFeatureModelEditor())) {
				controller.switchToFeatureModelEditor(null);
			}
		}

		if (partRef.getId().equals(ConstraintViewController.ID)) {
			controller.setConstraintsHidden(false);
		}
	}

	@Override
	public void partVisible(IWorkbenchPartReference partRef) {
		final IWorkbenchPart visiblePart = partRef.getPart(false);

		if (visiblePart instanceof FeatureModelEditor) {
			controller.switchToFeatureModelEditor((FeatureModelEditor) visiblePart);
		} else if (partRef.getId().equals(ConstraintViewController.ID)) {
			final FeatureModelEditor visibleFeatureModelEditor = getVisibleFeatureModelEditor();
			if (visibleFeatureModelEditor != null) {
				controller.switchToFeatureModelEditor(visibleFeatureModelEditor);
			}
		}
	}

	@Override
	public void partInputChanged(IWorkbenchPartReference partRef) {}

	/**
	 * Searches the Workbench for visible FeatureModelEditors and returns the first one found
	 *
	 * @return A visible FeatureModelEditor
	 */
	private FeatureModelEditor getVisibleFeatureModelEditor() {
		final IEditorReference[] references = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
		for (final IEditorReference reference : references) {
			final IWorkbenchPart part = reference.getPart(false);
			if ((part != null) && (part instanceof FeatureModelEditor)
				&& PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().isPartVisible(part)) {
				return (FeatureModelEditor) part;
			}
		}
		return null;
	}

}
