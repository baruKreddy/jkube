/**
 * Copyright (c) 2019 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at:
 *
 *     https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.jkube.maven.plugin.mojo.develop;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jkube.kit.common.util.ResourceUtil;
import org.eclipse.jkube.maven.plugin.mojo.ManifestProvider;
import org.eclipse.jkube.maven.plugin.mojo.build.AbstractJKubeMojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import static org.eclipse.jkube.maven.plugin.mojo.build.ApplyMojo.DEFAULT_KUBERNETES_MANIFEST;

/**
 * Undeploys (deletes) the kubernetes resources generated by the current project.
 * <br>
 * This goal is the opposite to the <code>k8s:run</code> or <code>k8s:deploy</code> goals.
 */
@Mojo(name = "undeploy", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.INSTALL)
public class UndeployMojo extends AbstractJKubeMojo implements ManifestProvider {

  /**
   * The generated kubernetes YAML file
   */
  @Parameter(property = "jkube.kubernetesManifest", defaultValue = DEFAULT_KUBERNETES_MANIFEST)
  protected File kubernetesManifest;

  /**
   * Folder where to find project specific files
   */
  @Parameter(property = "jkube.resourceDir", defaultValue = "${basedir}/src/main/jkube")
  protected File resourceDir;

  /**
   * Environment name where resources are placed. For example, if you set this property to dev and resourceDir is the default one, jkube will look at src/main/jkube/dev
   * Same applies for resourceDirOpenShiftOverride property.
   */
  @Parameter(property = "jkube.environment")
  private String environment;

  @Parameter(property = "jkube.skip.undeploy", defaultValue = "false")
  protected boolean skipUndeploy;

  @Override
  public File getKubernetesManifest() {
    return kubernetesManifest;
  }

  @Override
  public final void executeInternal() throws MojoExecutionException {
    try {
      undeploy();
    } catch (IOException ex) {
      throw new MojoExecutionException(ex.getMessage(), ex);
    }
  }

  protected void undeploy() throws IOException {
    final List<File> environmentResourceDirs = ResourceUtil.getFinalResourceDirs(resourceDir, environment);
    jkubeServiceHub.getUndeployService()
        .undeploy(environmentResourceDirs, resources, getManifestsToUndeploy().toArray(new File[0]));
  }

  protected List<File> getManifestsToUndeploy() {
    final List<File> ret = new ArrayList<>();
    ret.add(getManifest(jkubeServiceHub.getClient()));
    return ret;
  }

  @Override
  protected boolean canExecute() {
    return super.canExecute() && !skipUndeploy;
  }
}
