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
package org.eclipse.jkube.springboot.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringBootGeneratorTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private GeneratorContext context;

  @Before
  public void setUp() throws Exception {
    context = GeneratorContext.builder()
      .logger(new KitLogger.SilentLogger())
      .project(JavaProject.builder()
        .outputDirectory(temporaryFolder.newFolder("target"))
        .version("1.0.0")
        .build())
      .build();
  }

  @Test
  public void isApplicable_withNoImageConfigurations_shouldReturnFalse() {
    // When
    final boolean result = new SpringBootGenerator(context).isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isFalse();
  }

  @Test
  public void isApplicable_withNoImageConfigurationsAndMavenPlugin_shouldReturnTrue() {
    // Given
    withPlugin(Plugin.builder()
        .groupId("org.springframework.boot")
        .artifactId("spring-boot-maven-plugin")
        .build());
    // When
    final boolean result = new SpringBootGenerator(context).isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isTrue();
  }

  @Test
  public void isApplicable_withNoImageConfigurationsAndGradlePlugin_shouldReturnTrue() {
    // Given
    withPlugin(Plugin.builder()
        .groupId("org.springframework.boot")
        .artifactId("org.springframework.boot.gradle.plugin")
        .build());
    // When
    final boolean result = new SpringBootGenerator(context).isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isTrue();
  }

  @Test
  public void getExtraJavaOptions_withDefaults_shouldBeEmpty() {
    // When
    final List<String> result = new SpringBootGenerator(context).getExtraJavaOptions();
    // Then
    assertThat(result).isNotNull().isEmpty();
  }

  @Test
  public void customize_withEmptyList_shouldReturnAddedImage() {
    // When
    final List<ImageConfiguration> configs = new SpringBootGenerator(context).customize(new ArrayList<>(), true);
    // Then
    assertThat(configs)
        .singleElement()
        .extracting(ImageConfiguration::getBuildConfiguration)
        .hasFieldOrPropertyWithValue("ports", Arrays.asList("8080", "8778", "9779"))
        .extracting(BuildConfiguration::getEnv)
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .hasSize(1)
        .containsEntry("JAVA_APP_DIR", "/deployments");
  }

  private void withPlugin(Plugin plugin) {
    context = context.toBuilder()
      .project(JavaProject.builder().plugin(plugin).build())
      .build();
  }
}
