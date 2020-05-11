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
package de.ovgu.featureide.fm.ui.views.constraintview.view;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import de.ovgu.featureide.fm.core.analysis.ConstraintProperties.ConstraintStatus;
import de.ovgu.featureide.fm.core.base.IConstraint;
import de.ovgu.featureide.fm.core.localization.StringTable;
import de.ovgu.featureide.fm.ui.editors.featuremodel.GUIDefaults;
import de.ovgu.featureide.fm.ui.views.constraintview.ConstraintViewController;
import de.ovgu.featureide.fm.ui.views.constraintview.listener.ConstraintViewColumnSelectionListener;

/**
 * This class represents the view (MVC) of the constraint view. It creates all UI elements and provides methods to get the conten of the view.
 *
 * @author Rosiak Kamil
 * @author Domenik Eichhorn
 * @author Thomas Graave
 * @author Rahel Arens
 */
public class ConstraintView implements GUIDefaults {

	@SuppressWarnings("unused")
	private final Color HEADER_BACKGROUND_COLOR = new Color(Display.getDefault(), 207, 207, 207);
	@SuppressWarnings("unused")
	private final Color HEADER_FORGROUND_COLOR = new Color(Display.getDefault(), 0, 0, 0);
	private final Color ROW_ALTER_COLOR = new Color(Display.getDefault(), 240, 240, 240);

	// Style parameters for the view
	private final int CIRCLE_DECORATION_SIZE = 16;
	private final String CONSTRAINT_HEADER = "Constraint";
	private final String DESCRIPTION_HEADER = "Description";
	private final String DEFAULT_MESSAGE = StringTable.OPEN_A_FEATURE_DIAGRAM;

	private static final float NAME_COLUMN_WIDTH_RATIO = 0.33f;
	private static final float DESCRIPTION_COLUMN_WIDTH_RATIO = 0.67f;
	private static final int COLUMN_DEFAULT_WIDTH = 400;

	// UI elements
	private TreeViewer treeViewer;
	private Tree tree;
	private Text searchBox;

	private final ConstraintViewController controller;

	private TreeColumn nameColumn, descriptionColumn;

	public void dispose() {
		treeViewer.getTree().dispose();
	}

	public ConstraintView(Composite parent, ConstraintViewController controller) {
		this.controller = controller;
		init(parent);
	}

	/**
	 * This method creates a TreeItem from a constraint and adds it to the view
	 */
	public TreeItem addItem(IConstraint constraint) {
		final TreeItem item = new TreeItem(tree, SWT.None);
		item.setData(constraint);

		String displayName = constraint.getDisplayName();
		displayName = replaceSpecialChars(displayName);

		final String description = constraint.getDescription().replaceAll("\n", " ");

		item.setText(new String[] { displayName, description });

		if (((tree.getItemCount() % 2) == 1)) {
			item.setBackground(ROW_ALTER_COLOR);
		}
		if (controller.getConstraintProperty(constraint).hasStatus(ConstraintStatus.REDUNDANT)) {
			item.setImage(FM_INFO);
		}
		tree.setHeaderVisible(true);
		return item;
	}

	/**
	 * This method decorates the icon of the TreeItem with the evidence color of the explanation.
	 *
	 * @param constraint the constraint that would be shown in the view.
	 * @param color the evidence color of the explanation.
	 */
	public void addDecoratedItem(IConstraint constraint, Color color) {
		final TreeItem item = addItem(constraint);
		Image elementImg;
		if (color == null) {
			elementImg = FM_INFO;
		} else {
			elementImg = getColoredCircleIcon(color);
		}
		item.setImage(elementImg);
	}

	/**
	 * Changes the existing item to a decorated item
	 */
	public void changeToDecoratedItem(IConstraint constraint, Color color) {
		for (final TreeItem item : tree.getItems()) {
			if (item.getData() instanceof IConstraint) {
				if (item.getData().equals(constraint)) {
					Image elementImg;
					if (color == null) {
						elementImg = FM_INFO;
					} else {
						elementImg = getColoredCircleIcon(color);
					}
					item.setImage(elementImg);
				}
			}
		}
	}

	/**
	 * This method draws a circle icon filled with the parameters color.
	 *
	 * @param color that the icon will be filled with.
	 * @return
	 */
	private Image getColoredCircleIcon(Color color) {
		final Image image = new Image(Display.getDefault(), CIRCLE_DECORATION_SIZE, CIRCLE_DECORATION_SIZE);
		final GC gc = new GC(image);
		gc.setBackground(color);
		gc.setAntialias(SWT.ON);
		gc.fillOval(0, 0, CIRCLE_DECORATION_SIZE, CIRCLE_DECORATION_SIZE);
		gc.dispose();
		return image;
	}

	/**
	 * Removes decoration from item
	 */
	public void undecorateItem(IConstraint constraint) {
		for (final TreeItem item : tree.getItems()) {
			if (item.getData() instanceof IConstraint) {
				if (item.getData().equals(constraint)) {
					if (controller.getConstraintProperty(constraint).hasStatus(ConstraintStatus.REDUNDANT)) {
						item.setImage(FM_INFO);
					} else {
						item.setImage((Image) null);
					}
				}
			}
		}
	}

	/**
	 * replaces logical connectives with unicode signs
	 */
	private String replaceSpecialChars(String string) {

		string = string.replace("|", "\u2228");
		string = string.replace("<=>", "\u21D4");
		string = string.replace("=>", "\u21D2");
		string = string.replace("&", "\u2227");
		string = string.replace("-", "\u00AC");
		return string;
	}

	/**
	 * This method adds an item to represent that no feature model editor is activated or no feature model is loaded.
	 */
	public void addNoFeatureModelItem() {
		removeAll();
		final TreeItem item = new TreeItem(tree, SWT.None);
		item.setText(DEFAULT_MESSAGE);
		item.setImage(DEFAULT_IMAGE);
		tree.setHeaderVisible(false);
	}

	/**
	 * This method removes a constraint from the view
	 */
	public void removeItem(IConstraint element) {
		treeViewer.remove(element);
	}

	/**
	 * This method returns the tree viewer
	 */
	public TreeViewer getViewer() {
		return treeViewer;
	}

	/**
	 * This method removes all constraints from the view
	 */
	public void removeAll() {
		if (treeViewer.getTree() != null) {
			treeViewer.getTree().removeAll();
		}
	}

	/**
	 * This method initializes the view
	 */
	private void init(Composite parent) {
		parent.setLayout(new GridLayout(1, false));

		final GridData boxData = new GridData();
		boxData.grabExcessHorizontalSpace = true;
		boxData.horizontalAlignment = SWT.FILL;
		searchBox = new Text(parent, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL | SWT.BORDER);
		searchBox.setLayoutData(boxData);

		treeViewer = new TreeViewer(parent, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		final GridData treeData = new GridData();
		treeData.grabExcessHorizontalSpace = true;
		treeData.horizontalAlignment = SWT.FILL;
		treeData.grabExcessVerticalSpace = true;
		treeData.verticalAlignment = SWT.FILL;
		tree = treeViewer.getTree();
		tree.setLayoutData(treeData);
		// XXX Not available for Eclipse Neon or below
//		tree.setHeaderBackground(HEADER_BACKGROUND_COLOR);
//		tree.setHeaderForeground(HEADER_FORGROUND_COLOR);
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);

		addColumns(treeViewer);
	}

	/**
	 * Adds the columns with topics to the tree viewer
	 */
	private void addColumns(TreeViewer viewer) {
		nameColumn = new TreeColumn(viewer.getTree(), SWT.LEFT);
		nameColumn.setResizable(true);
		nameColumn.setMoveable(true);
		nameColumn.setWidth(COLUMN_DEFAULT_WIDTH);
		nameColumn.setText(CONSTRAINT_HEADER);

		nameColumn.addSelectionListener(new ConstraintViewColumnSelectionListener(this, nameColumn));

		descriptionColumn = new TreeColumn(viewer.getTree(), SWT.LEFT);
		descriptionColumn.setResizable(true);
		descriptionColumn.setMoveable(true);
		descriptionColumn.setWidth(COLUMN_DEFAULT_WIDTH);
		descriptionColumn.setText(DESCRIPTION_HEADER);

		descriptionColumn.addSelectionListener(new ConstraintViewColumnSelectionListener(this, descriptionColumn));

		// resize the columns once relative to the view size
		final ControlListener viewResizeListener = new ControlListener() {

			@Override
			public void controlMoved(ControlEvent e) {}

			@Override
			public void controlResized(ControlEvent e) {
				final int viewerWidth = viewer.getTree().getParent().getClientArea().width;
				nameColumn.setWidth((int) (viewerWidth * NAME_COLUMN_WIDTH_RATIO));
				descriptionColumn.setWidth((int) (viewerWidth * DESCRIPTION_COLUMN_WIDTH_RATIO));

				// Eclipse resizes views on startup twice. The final view size is only reached,
				// when the view is visible.
				if (viewer.getControl().isVisible()) {
					// Remove the listener, because we only want the resizing to happen once
					viewer.getControl().getParent().removeControlListener(this);
				}
			}
		};
		viewer.getControl().getParent().addControlListener(viewResizeListener);
	}

	/**
	 * Sorts the TreeItems of the given Tree
	 *
	 * @param tree Tree to be sorted
	 * @param treeColumn Column to sort by
	 * @param direction Direction to sort by. SWT.UP to sort in ascending order, SWT.DOWN to sort in descending order
	 */
	public void sortTree(Tree tree, TreeColumn treeColumn, int direction) {
		tree.setSortColumn(treeColumn);
		tree.setSortDirection(direction);

		final TreeItem[] items = tree.getItems();

		final IConstraint[] constraints = new IConstraint[items.length];
		for (int i = 0; i < items.length; i++) {
			constraints[i] = (IConstraint) items[i].getData();
		}
		tree.removeAll();

		if (treeColumn.equals(descriptionColumn)) {
			Arrays.sort(constraints, (o1, o2) -> {
				final int compare = o1.getDescription().compareTo(o2.getDescription());
				if (compare == 0) {
					return o1.getDisplayName().compareTo(o2.getDisplayName());
				}

				if (o1.getDescription().equals("")) {
					return 1;
				}

				if (o2.getDescription().equals("")) {
					return -1;
				}
				return compare;
			});
		} else if (treeColumn.equals(nameColumn)) {
			Arrays.sort(constraints, Comparator.comparing(o -> o.getDisplayName()));
		}

		// reverse direction when sorting down
		if (direction == SWT.DOWN) {
			for (int i = 0; i < (constraints.length / 2); i++) {
				final IConstraint tmp = constraints[i];
				constraints[i] = constraints[constraints.length - i - 1];
				constraints[constraints.length - i - 1] = tmp;
			}
		}
		for (final IConstraint constraint : constraints) {
			addItem(constraint);
		}
	}

	/**
	 * Text searchBox
	 */
	public Text getSearchBox() {
		return searchBox;
	}

}
