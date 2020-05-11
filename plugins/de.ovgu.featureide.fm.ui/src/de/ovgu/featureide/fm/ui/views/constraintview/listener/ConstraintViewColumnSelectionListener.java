/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2017  FeatureIDE team, University of Magdeburg, Germany
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import de.ovgu.featureide.fm.ui.views.constraintview.view.ConstraintView;

/**
 * SelectionListener for TreeColumns to sort when the column label is selected (clicked)
 *
 * @author Soeren Viegener
 * @author Philipp Vulpius
 */
public class ConstraintViewColumnSelectionListener implements SelectionListener {

	final private TreeColumn column;
	final private ConstraintView constraintView;

	public ConstraintViewColumnSelectionListener(ConstraintView constraintView, TreeColumn column) {
		this.column = column;
		this.constraintView = constraintView;
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		final Tree tree = constraintView.getViewer().getTree();
		if (column.equals(tree.getSortColumn())) {
			constraintView.sortTree(tree, column, tree.getSortDirection() == SWT.UP ? SWT.DOWN : SWT.UP);
		} else {
			constraintView.sortTree(tree, column, SWT.UP);
		}
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {

	}
}
