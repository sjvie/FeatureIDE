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
package de.ovgu.featureide.fm.core.io.guidsl;

import static de.ovgu.featureide.fm.core.localization.StringTable.EMPTY___;

import java.util.List;

import org.prop4j.Node;
import org.prop4j.NodeWriter;

import de.ovgu.featureide.fm.core.base.FeatureUtils;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;

/**
 * Writes a feature model in the GUIDSL format (grammar).
 *
 * @author Thomas Thuem
 * @author Marcus Pinnecke (Feature Interface)
 */
public class GuidslWriter {

	private IFeatureModel object;

	private boolean hasHiddenFeatures() {
		for (final IFeature feat : object.getFeatures()) {
			if (feat.getStructure().isHidden()) {
				return true;
			}
		}
		return false;
	}

	public String writeToString(IFeatureModel featureModel) {
		object = featureModel;
		// open a string buffer for writing
		final StringBuilder out = new StringBuilder();// = new BufferedWriter(new FileWriter(file));

		// write generating information
//		DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM, Locale.ENGLISH);
//		out.append("//This model was generated by the Eclipse Plugin at ");
//		out.append(formatter.format(new Date()));
//		out.append("\r\n\r\n");

		// write comments
		writeComments(out);

		// write featureModel
		writeGrammarDefinition(out);
		writePropositionalConstraints(out);

		// write hidden features
		if (hasHiddenFeatures()) {
			out.append("##\r\n\r\n");
			for (final IFeature feat : featureModel.getFeatures()) {
				if (feat.getStructure().isHidden()) {
					out.append(feat.getName() + " { hidden } \r\n");
				}
			}
		}

		return out.toString();
	}

	private void writeComments(StringBuilder out) {
		for (final String comment : object.getProperty().getComments()) {
			out.append("//" + comment + "\n");
		}
	}

	private void writeGrammarDefinition(StringBuilder out) {
		final IFeature root = object.getStructure().getRoot().getFeature();
		if (root != null) {
			if (root.getStructure().isOr()) {
				out.append(root.getName());
				out.append("_ : ");
				out.append(root.getName());
				out.append("+ :: _");
				out.append(root.getName());
				out.append(" ;\r\n\r\n");
			}
			writeRule(object.getStructure().getRoot().getFeature(), out);
		} else {
			out.append("\r\n");
		}
	}

	private void writePropositionalConstraints(StringBuilder out) {
		if (object.getConstraints().isEmpty()) {
			return;
		}
		out.append("%%\r\n\r\n");
		for (final Node node : FeatureUtils.getPropositionalNodes(object.getConstraints())) {
			out.append(node.toString(NodeWriter.textualSymbols) + " ;\r\n");
		}
		out.append("\r\n");
	}

	// private void writeAnnotations(StringBuffer out) {
	// if (featureModel.getAnnotations() != null)
	// out.append(featureModel.getAnnotations() + "\r\n\r\n");
	// }

	private void writeRule(IFeature mainFeature, StringBuilder out) {
		// check if there is a rule to write
		if (!mainFeature.getStructure().hasChildren()) {
			return;
		}
		final List<IFeature> mainChildren = FeatureUtils.convertToFeatureList(mainFeature.getStructure().getChildren());

		// left part of the rule
		out.append(mainFeature.getName());
		out.append(" :");

		// check if a output over more than one line is possible
		final boolean moreThanOneLine = isMoreThanOneLinePossible(mainFeature, mainChildren);
		// moreThanOneLine = !mainFeature.isAND();

		// write out the line(s)
		for (int i = 0; i < mainChildren.size(); i++) {
			final IFeature feature = mainChildren.get(i);
			if (moreThanOneLine && (i > 0)) {
				out.append("\r\n\t|");
			} else if (!mainFeature.getStructure().isAnd() && (i > 0)) {
				out.append("\r\n\t|");
			}
			if (!mainFeature.getStructure().isAnd() && feature.getStructure().hasInlineRule()) {
				final List<IFeature> children = FeatureUtils.convertToFeatureList(feature.getStructure().getChildren());
				for (int j = 0; j < children.size(); j++) {
					out.append(" ");
					out.append(getRightGrammarToken(children.get(j)));
				}
				out.append(" :: ");
				out.append(feature.getName());
			} else {
				out.append(" ");
				out.append(getRightGrammarToken(feature));
				if (!mainFeature.getStructure().isAnd() && (!feature.getStructure().isMandatory() || feature.getStructure().isMultiple())) {
					out.append(" :: ");
					out.append(feature.getName() + EMPTY___);
				}
			}
		}
		if (mainFeature.getStructure().isAnd()) {// && mainChildren.size() > 1) {
			out.append(" :: _");
			out.append(mainFeature.getName());
		}
		out.append(" ;\r\n\r\n");

		// write all left rules
		writeChildRules(mainFeature, mainChildren, out);
	}

	private boolean isMoreThanOneLinePossible(IFeature feature, List<IFeature> children) {
		if (!feature.getStructure().isAnd()) {
			for (int i = 0; i < children.size(); i++) {
				final IFeature child = children.get(i);
				if (child.getStructure().hasInlineRule()) {
					return true;
				}
			}
		}
		return false;
	}

	public static String getRightGrammarToken(IFeature feature) {
		if (feature.getStructure().isMultiple()) {
			return feature.getName() + (feature.getStructure().isMandatory() ? "+" : "*");
		}
		return feature.getStructure().isMandatory() ? feature.getName() : "[" + feature.getName() + "]";
	}

	private void writeChildRules(IFeature mainFeature, List<IFeature> mainChildren, StringBuilder out) {
		for (int i = 0; i < mainChildren.size(); i++) {
			final IFeature feature = mainChildren.get(i);
			if (!mainFeature.getStructure().isAnd() && feature.getStructure().hasInlineRule()) {
				final List<IFeature> children = FeatureUtils.convertToFeatureList(feature.getStructure().getChildren());
				for (int j = 0; j < children.size(); j++) {
					writeRule(children.get(j), out);
				}
			} else {
				writeRule(feature, out);
			}
		}
	}

	/**
	 * @return true, if the feature model has at least one abstract feature
	 */
	public boolean hasAbstractFeatures() {
		return hasAbstractFeaturesRec(object.getStructure().getRoot().getFeature());
	}

	private boolean hasAbstractFeaturesRec(IFeature mainFeature) {
		final List<IFeature> mainChildren = FeatureUtils.convertToFeatureList(mainFeature.getStructure().getChildren());
		for (int i = 0; i < mainChildren.size(); i++) {
			final IFeature feature = mainChildren.get(i);
			if (feature.getStructure().isAbstract()) {
				return true;
			} else if (feature.getStructure().hasChildren()) {
				return hasAbstractFeaturesRec(feature);
			}
		}
		return false;
	}

	public boolean hasConcreteCompounds() {
		return hasConcreteCompoundsRec(object.getStructure().getRoot().getFeature());
	}

	private boolean hasConcreteCompoundsRec(IFeature mainFeature) {
		if (!mainFeature.getStructure().isAbstract() && mainFeature.getStructure().hasChildren()) {
			return true;
		}
		final List<IFeature> mainChildren = FeatureUtils.convertToFeatureList(mainFeature.getStructure().getChildren());
		for (int i = 0; i < mainChildren.size(); i++) {
			final IFeature feature = mainChildren.get(i);
			if (!feature.getStructure().isAbstract() && feature.getStructure().hasChildren()) {
				return true;
			} else if (feature.getStructure().hasChildren()) {
				return hasConcreteCompoundsRec(feature);
			}
		}
		return false;
	}
}
