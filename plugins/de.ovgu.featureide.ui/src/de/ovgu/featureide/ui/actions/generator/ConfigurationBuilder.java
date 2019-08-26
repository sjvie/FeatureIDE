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
package de.ovgu.featureide.ui.actions.generator;

import static de.ovgu.featureide.fm.core.localization.StringTable.COUNTING___;
import static de.ovgu.featureide.fm.core.localization.StringTable.INCLING;
import static de.ovgu.featureide.fm.core.localization.StringTable.OF;
import static de.ovgu.featureide.fm.core.localization.StringTable.RESTRICTION;

import java.security.KeyStore.Builder;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.CheckForNull;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaProject;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.fm.core.analysis.cnf.LiteralSet;
import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula;
import de.ovgu.featureide.fm.core.job.IJob.JobStatus;
import de.ovgu.featureide.fm.core.job.IRunner;
import de.ovgu.featureide.fm.core.job.LongRunningWrapper;
import de.ovgu.featureide.fm.core.job.monitor.IMonitor;
import de.ovgu.featureide.fm.core.job.monitor.ProgressMonitor;
import de.ovgu.featureide.fm.core.localization.StringTable;
import de.ovgu.featureide.ui.UIPlugin;
import de.ovgu.featureide.ui.actions.generator.configuration.AConfigurationGenerator;
import de.ovgu.featureide.ui.actions.generator.configuration.AllConfigrationsGenerator;
import de.ovgu.featureide.ui.actions.generator.configuration.CASAConfigurationGenerator;
import de.ovgu.featureide.ui.actions.generator.configuration.CHVATALConfigurationGenerator;
import de.ovgu.featureide.ui.actions.generator.configuration.CurrentConfigurationsGenerator;
import de.ovgu.featureide.ui.actions.generator.configuration.ICPLConfigurationGenerator;
import de.ovgu.featureide.ui.actions.generator.configuration.IncLingConfigurationGenerator;
import de.ovgu.featureide.ui.actions.generator.configuration.ModuleConfigurationGenerator;
import de.ovgu.featureide.ui.actions.generator.configuration.RandConfigurationGenerator;
import de.ovgu.featureide.ui.actions.generator.sorter.AbstractConfigurationSorter;
import de.ovgu.featureide.ui.actions.generator.sorter.InteractionSorter;
import de.ovgu.featureide.ui.actions.generator.sorter.PriorizationSorter;

/**
 * Builds all valid or current configurations for a selected feature project.
 *
 * @author Jens Meinicke
 */
@SuppressWarnings(RESTRICTION)
public class ConfigurationBuilder implements IConfigurationBuilderBasics {

	private static final UIPlugin LOGGER = UIPlugin.getDefault();

	public IFeatureProject featureProject;
	private FeatureModelFormula featureModel;

	/**
	 * This is the place where all configurations should be generated if no new projects should be generated.
	 */
	IFolder folder;

	/**
	 * The count of valid configurations.<br> This is generated by the sat-solver or the amount current configurations at the configurations folder.
	 */
	public long configurationNumber = 0;

	/**
	 * A flag that indicates that the counting Job is running or has finished.
	 */
	public boolean counting = true;

	/**
	 * Saves the classpath entry for compilation.
	 */
	String classpath = "";

	/**
	 * This folder is used for compilation.
	 */
	IFolder tmp;

	/**
	 * This flag indicates if a new project should be created for each configuration.
	 */
	boolean createNewProjects;

	/**
	 * The count of how many configurations where already built
	 */
	private int built;

	/**
	 * This flag indicates that all jobs should be aborted.
	 */
	boolean cancelGeneratorJobs = false;

	/**
	 * Saves the time of start.
	 */
	protected long time;

	/**
	 * This flag indicates that no more configurations will be added and the {@link Generator} jobs can finish.
	 */
	boolean finish = false;

	/**
	 * <code>true</code>: all valid configurations should be built.<br> <code>false</code>: all configurations at the configurations folder should be built.
	 */
	BuildType buildType;

	/**
	 * This list contains all {@link Generator} jobs.
	 */
	final List<Generator> generatorJobs = new ArrayList<>();

	public AbstractConfigurationSorter sorter;

	public final boolean runTests;

	TestResults testResults;

	private AConfigurationGenerator configurationGenerator;

	/**
	 * Gets the first entry of configurations or <code>null</code> if there is none.
	 *
	 * @return The first entry
	 */
	@CheckForNull
	public synchronized BuilderConfiguration getConfiguration() {
		return sorter.getConfiguration();
	}

	/**
	 * Adds the given configuration to configurations.
	 *
	 * @param configuration to add
	 */
	public synchronized void addConfiguration(BuilderConfiguration configuration) {
		sorter.addConfiguration(configuration);
	}

	// TODO revise long parameter list
	/**
	 * Starts the build process for valid or current configurations for the given feature project.
	 *
	 * @param featureProject The feature project
	 * @param buildAllValidConfigurations <code>true</code> if all possible valid configurations should be build<br> <code>false</code> if all current
	 *        configurations should be build
	 * @param createNewProjects <code>true</code> if the configurations should be built into separate projects
	 * @param runTests
	 * @param max Maximal number of configurations to generate.
	 * @param tOrder
	 * @see BuildAllCurrentConfigurationsAction
	 * @see BuildAllValidConfigurationsAction
	 */
	/**
	 *
	 * @param featureProject The feature project
	 * @param buildType <code>true</code> if all possible valid configurations should be build<br> <code>false</code> if all current configurations should be
	 *        build
	 * @param createNewProjects <code>true</code> if the configurations should be built into separate projects
	 * @param algorithm selected algorithmen
	 * @param t t
	 * @param buildOrder build order
	 * @param runTests whether the tests should be run after the operation
	 * @param max Maximal number of configurations to generate.
	 * @param tOrder tOrder
	 */
	public ConfigurationBuilder(final IFeatureProject featureProject, final BuildType buildType, final boolean createNewProjects, final String algorithm,
			final int t, final BuildOrder buildOrder, boolean runTests, int max, int tOrder) {
		this(featureProject, buildType, createNewProjects, algorithm, t, buildOrder, runTests, null, max, tOrder);
	}

	public ConfigurationBuilder(final IFeatureProject featureProject, final BuildType buildType, final String featureName) {
		this(featureProject, BuildType.INTEGRATION, false, "", 0, BuildOrder.DEFAULT, true, featureName, Integer.MAX_VALUE, 1);
	}

	static int id = 0;
	IProgressMonitor globalMonitor;

	public ConfigurationBuilder(final IFeatureProject featureProject, final BuildType buildType, final boolean createNewProjects, final String algorithm,
			final int t, final BuildOrder buildOrder, boolean runTests, final String featureName, final int maxConfigs, int tOrder) {
		this.runTests = runTests;
		if (maxConfigs <= 0) {
			return;
		}
		configurationNumber = maxConfigs;
		if (runTests) {
			testResults = new TestResults(featureProject.getProjectName(), "FeatureIDE test: " + featureProject.getProjectName());
		} else {
			testResults = null;
		}
		if (!featureProject.getComposer().preBuildConfiguration()) {
			return;
		}
		this.featureProject = featureProject;
		this.createNewProjects = createNewProjects;
		this.buildType = buildType;

		featureModel = featureProject.getFeatureModelManager().getPersistentFormula();

		switch (buildOrder) {
		case DEFAULT:
			sorter = new AbstractConfigurationSorter(featureModel.getFeatureModel());
			break;
		case DISSIMILARITY:
			sorter = new PriorizationSorter(featureModel);
			break;
		case INTERACTION:
			sorter = new InteractionSorter(tOrder, featureModel.getFeatureModel(), buildType == BuildType.T_WISE);
			break;
		default:
			LOGGER.logWarning("Case statement missing for: " + buildOrder);
			sorter = new AbstractConfigurationSorter(featureModel.getFeatureModel());
			break;
		}

		String jobName = "";
		switch (buildType) {
		case ALL_CURRENT:
			configurationGenerator = new CurrentConfigurationsGenerator(this, featureProject);
			jobName = JOB_TITLE_CURRENT;
			break;
		case ALL_VALID:
			configurationGenerator = new AllConfigrationsGenerator(this, featureModel);
			jobName = JOB_TITLE;
			break;
		case T_WISE:
			if (algorithm.equals(INCLING)) {
				configurationGenerator = new IncLingConfigurationGenerator(this, featureModel);
			} else if (algorithm.equals("ICPL")) {
				configurationGenerator = new ICPLConfigurationGenerator(this, featureModel, t);
			} else if (algorithm.equals("Chvatal")) {
				configurationGenerator = new CHVATALConfigurationGenerator(this, featureModel, t);
			} else if (algorithm.equals("CASA")) {
				configurationGenerator = new CASAConfigurationGenerator(this, featureModel, t);
			} else {
				throw new RuntimeException(buildType + " not supported");
			}
			jobName = JOB_TITLE_T_WISE;
			break;
		case RANDOM:
			configurationGenerator = new RandConfigurationGenerator(this, featureModel);
			jobName = JOB_TITLE_RANDOM;
			break;
		case INTEGRATION:
			configurationNumber = 2;
			configurationGenerator = new ModuleConfigurationGenerator(this, featureModel, featureName);
			break;
		default:
			throw new RuntimeException(buildType + " not supported");
		}
		jobName += StringTable.FOR + featureProject.getProjectName();
		RemoveBaseMarkerFromSourceFolderFiles();
		final Job job = new Job(jobName) {

			private IRunner<List<LiteralSet>> configurationBuilderJob;

			@Override
			public IStatus run(IProgressMonitor monitor) {
				try {
					monitor = SubMonitor.convert(monitor, getTaskName(), maxConfigs);
					globalMonitor = monitor;
					if (!init(monitor, buildType)) {
						return Status.OK_STATUS;
					}

					time = System.currentTimeMillis();

					if (featureProject.getComposer().canGeneratInParallelJobs()) {
						if (buildType != BuildType.ALL_CURRENT) {
							newgeneratorJobs(Runtime.getRuntime().availableProcessors() * 2);
						} else {
							int contJobs = Runtime.getRuntime().availableProcessors() * 2;
							if (configurationNumber < contJobs) {
								contJobs = (int) configurationNumber;
							}
							newgeneratorJobs(contJobs);
						}
					} else {
						newgeneratorJobs(1);
					}
					configurationBuilderJob = LongRunningWrapper.getRunner(configurationGenerator, "Create Configurations " + id++);
					configurationBuilderJob.schedule();
					showStatistics(monitor);
					if (!createNewProjects) {
						try {
							folder.refreshLocal(IResource.DEPTH_INFINITE, null);
						} catch (final CoreException e) {
							LOGGER.logError(e);
						}
					}
				} finally {
					configurationBuilderJob.cancel();
					cancelGenerationJobs();
					for (final Thread g : generatorJobs) {
						g.interrupt();
					}
					monitor.done();
				}
				return Status.OK_STATUS;
			}

			private void showStatistics(IProgressMonitor monitor) {
				try {
					while (configurationBuilderJob.getStatus() == JobStatus.NOT_STARTED) {
						try {
							Thread.sleep(150);
						} catch (final InterruptedException e) {
							LOGGER.logError(e);
						}
					}
					while (configurationBuilderJob.getStatus() == JobStatus.RUNNING) {
						monitor.setTaskName(getTaskName());
						if (monitor.isCanceled()) {
							cancelGenerationJobs();
							configurationBuilderJob.cancel();
							return;
						}
						try {
							Thread.sleep(150);
						} catch (final InterruptedException e) {
							LOGGER.logError(e);
						}
					}
					System.err.println(configurationBuilderJob.getStatus());

					if (!sorter.isSorted()) {
						final IMonitor<?> workMonitor = new ProgressMonitor<>(getTaskName(), monitor);
						configurationNumber = Math.min(configurationNumber, sorter.sortConfigurations(workMonitor));
					}
					finish();

					((SubMonitor) monitor).setWorkRemaining((int) configurationNumber - built);
					while (!generatorJobs.isEmpty()) {
						try {
							if (monitor.isCanceled()) {
								cancelGenerationJobs();
								break;
							}
							final Generator generator = generatorJobs.get(0);
							if (generator == null) {
								// generator can never be null, however see #416
								generatorJobs.remove(0);
							} else if (generator.getState() == Thread.State.TERMINATED) {
								generatorJobs.remove(generator);
								if (sorter.getBufferSize() != 0) {
									createNewGenerator(generator.nr);
								}
							}
							monitor.setTaskName(getTaskName());
							Thread.sleep(150);
						} catch (final InterruptedException e) {
							LOGGER.logError(e);
						} catch (final IndexOutOfBoundsException e) {
							// nothing here
						}
					}

					final long duration = System.currentTimeMillis() - time;
					final long s = (duration / 1000) % 60;
					final long min = (duration / (60 * 1000)) % 60;
					final long h = duration / (60 * 60 * 1000);
					final String t = h + "h " + (min < 10 ? "0" + min : min) + "min " + (s < 10 ? "0" + s : s) + "s.";

					if (built > configurationNumber) {
						built = (int) configurationNumber;
					}
					LOGGER.logInfo(built + (configurationNumber != 0 ? OF + configurationNumber : "") + " configurations built in " + t);
				} finally {
					generatorJobs.clear();
				}
			}

		};
		job.setPriority(Job.LONG);
		job.schedule();
	}

	/**
	 * Initializes the configuration builder.<br> -Removes old products -Generates the build folder
	 *
	 * @param monitor
	 * @param buildAllValidConfigurations <code>true</code> if all possible valid configurations should be build<br> <code>false</code> if all current
	 *        configurations should be build
	 */
	private boolean init(IProgressMonitor monitor, BuildType buildType) {
		// method is called to initialize composer extension if not yet
		// initialized; so only delete if sure
		featureProject.getComposer();
		if (!createNewProjects) {
			folder = featureProject.getProject().getFolder(FOLDER_NAME);
			if (!folder.exists()) {
				try {
					folder.create(true, true, null);
				} catch (final CoreException e) {
					LOGGER.logError(e);
				}
			} else {
				try {
					folder.delete(true, null);
					folder.create(true, true, null);
				} catch (final CoreException e) {
					LOGGER.logError(e);
				}
			}
			setClassPath();

			tmp = folder.getFolder(TEMPORARY_BIN_FOLDER);
			if (!tmp.exists()) {
				try {
					tmp.create(true, true, null);
				} catch (final CoreException e) {
					LOGGER.logError(e);
				}
			}
		} else {
			try {
				String identifier;
				switch (buildType) {
				case ALL_CURRENT:
					identifier = SEPARATOR_CONFIGURATION;
					break;
				case ALL_VALID:
					identifier = SEPARATOR_VARIANT;
					break;
				case T_WISE:
					identifier = SEPARATOR_T_WISE;
					break;
				case INTEGRATION:
					identifier = SEPARATOR_INTEGRATION;
					break;
				case RANDOM:
					identifier = SEPARATOR_RANDOM;
					break;
				default:
					return false;
				}
				for (final IResource res : ResourcesPlugin.getWorkspace().getRoot().members()) {
					if (res instanceof IProject) {
						final IProject p = (IProject) res;
						final String projectName = p.getName();
						if (projectName.startsWith(featureProject.getProjectName() + identifier)) {
							monitor.setTaskName("Remove old products : " + projectName);
							res.delete(true, null);
						}
					}
				}
			} catch (final CoreException e) {
				LOGGER.logError(e);
			}
		}
		return true;
	}

	/**
	 * Sets the java classPath for compiling.
	 */
	private void setClassPath() {
		final String sep = System.getProperty("path.separator");
		try {
			final JavaProject proj = new JavaProject(featureProject.getProject(), null);
			final IJavaElement[] elements = proj.getChildren();
			for (final IJavaElement e : elements) {
				final String path = e.getPath().toOSString();
				if (e.getPath().isAbsolute()) {
					classpath += sep + "\"" + path + "\"";
				} else {
					final IResource resource = e.getResource();
					if ((resource != null) && "jar".equals(resource.getFileExtension())) {
						classpath += sep + "\"" + resource.getLocation().toOSString() + "\"";
					} else {
						UIPlugin.getDefault().logWarning("ClassPath element " + e.toString() + " is missing.");
					}
				}
			}
		} catch (final JavaModelException e) {

		}
		classpath = classpath.length() > 0 ? classpath.substring(1) : classpath;
	}

	/**
	 * Creates new {@link Generator}s
	 *
	 * @param count The amount of Generators that will be created.
	 */
	private void newgeneratorJobs(int count) {
		if (count < 1) {
			return;
		}

		if (count == 1) {
			createNewGenerator(0);
			return;
		}

		for (; count > 0; count--) {
			createNewGenerator(count);
		}
	}

	/**
	 * Created a new {@link Generator} with the given number
	 *
	 * @param nr
	 */
	void createNewGenerator(int nr) {
		final Generator g = new Generator(nr, this);
		generatorJobs.add(g);
		g.start();
	}

	/**
	 * This is called if the main job is canceled and all {@link Builder} and {@link JavaCompiler} should finish.
	 */
	public void cancelGenerationJobs() {
		cancelGeneratorJobs = true;
	}

	/**
	 * This is called if the main job has finished and no more configurations will be added.
	 */
	public void finish() {
		finish = true;
	}

	/**
	 * @return a description of the state.
	 */
	public String getTaskName() {
		String t = "";
		if ((configurationNumber != 0) && (built != 0)) {
			long duration = System.currentTimeMillis() - time;
			duration = (duration / built) * (configurationNumber - built);
			final long s = (duration / 1000) % 60;
			final long min = (duration / (60 * 1000)) % 60;
			final long h = duration / (60 * 60 * 1000);
			t = " " + h + "h " + (min < 10 ? "0" + min : min) + "min " + (s < 10 ? "0" + s : s) + "s.";
		}
		final long buffer = sorter.getBufferSize();
		return "Built configurations: " + built + "/" + (configurationNumber == Integer.MAX_VALUE ? COUNTING___ : configurationNumber) + "(" + buffer
			+ " buffered)" + " Expected time: " + t;
	}

	/**
	 * Notification that one configuration has been built.
	 */
	public synchronized void builtConfiguration() {
		built++;
		((SubMonitor) globalMonitor).setWorkRemaining((int) configurationNumber - built);
		globalMonitor.setTaskName(getTaskName());
		globalMonitor.worked(1);
	}

	/**
	 * Removes the base markers from every file in the source folder
	 */
	public void RemoveBaseMarkerFromSourceFolderFiles() {
		try {
			final IMarker[] baseMarker = featureProject.getSourceFolder().findMarkers(IMarker.MARKER, true, IResource.DEPTH_INFINITE);
			for (final IMarker iMarker : baseMarker) {
				iMarker.delete();
			}
		} catch (final CoreException e) {
			UIPlugin.getDefault().logError(e);
		}
	}

}
