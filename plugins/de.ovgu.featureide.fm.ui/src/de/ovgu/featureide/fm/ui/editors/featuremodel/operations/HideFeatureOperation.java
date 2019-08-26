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
package de.ovgu.featureide.fm.ui.editors.featuremodel.operations;

import static de.ovgu.featureide.fm.core.localization.StringTable.HIDE_OPERATION;

import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.IFeatureStructure;
import de.ovgu.featureide.fm.core.base.event.FeatureIDEEvent;
import de.ovgu.featureide.fm.core.base.event.FeatureIDEEvent.EventType;
import de.ovgu.featureide.fm.core.base.event.FeatureModelOperationEvent;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import de.ovgu.featureide.fm.core.io.manager.IFeatureModelManager;

/**
 * Required to execute compound operations
 *
 * @author Chico Sundermann
 * @author Paul Westphal
 */
public class HideFeatureOperation extends AbstractFeatureModelOperation {

	public static final String ID = ID_PREFIX + "HideFeatureOperation";

	private final String featureName;

	public HideFeatureOperation(String featureName, IFeatureModelManager featureModelManager) {
		super(featureModelManager, HIDE_OPERATION);
		this.featureName = featureName;
	}

	@Override
	protected FeatureIDEEvent operation(IFeatureModel featureModel) {
		final IFeature feature = featureModel.getFeature(featureName);
		final IFeatureStructure structure = feature.getStructure();
		structure.setHidden(!structure.isHidden());
		return new FeatureModelOperationEvent(ID, EventType.FEATURE_HIDDEN_CHANGED, feature, null, null);
	}

	@Override
	protected FeatureIDEEvent inverseOperation(IFeatureModel featureModel) {
		return operation(featureModel);
	}

	@Override
	protected int getChangeIndicator() {
		return FeatureModelManager.CHANGE_DEPENDENCIES;
	}

}
