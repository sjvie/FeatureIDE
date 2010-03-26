/* FeatureIDE - An IDE to support feature-oriented software development
 * Copyright (C) 2005-2010  FeatureIDE Team, University of Magdeburg
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 * See http://www.fosd.de/featureide/ for further information.
 */
package featureide.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Platform;

/**
 * Handles FeatureIDE extensions.
 * 
 * @author Tom Brosch
 */
public abstract class ExtensionPointManager<T extends featureide.core.IExtension> {

	protected final String pluginID;
	protected final String extensionPointID;

	protected ExtensionPointManager(String pluginID, String extensionPointID) {
		this.pluginID = pluginID;
		this.extensionPointID = extensionPointID;
	}

	private List<T> cachedProviders = null;

	private void loadProviders() {
		if (cachedProviders != null)
			return;
		cachedProviders = new ArrayList<T>();
		IExtension[] extensions = Platform.getExtensionRegistry()
				.getExtensionPoint(pluginID, extensionPointID)
				.getExtensions();
		for (IExtension extension : extensions) {
			IConfigurationElement[] configurationElements = extension
					.getConfigurationElements();
			for (IConfigurationElement configurationElement : configurationElements) {
				T proxy = parseExtension(configurationElement);
				if (proxy != null)
					cachedProviders.add(proxy);
			}
		}
	}

	protected abstract T parseExtension(
			IConfigurationElement configurationElement);

	protected List<T> getProviders() {
		loadProviders();
		return Collections.unmodifiableList(cachedProviders);
	}
}
