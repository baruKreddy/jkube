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
package org.eclipse.jkube.generator.javaexec;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.FileUtil;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Checking how the JavaExecGenerator checks the need to set a main class as environment variable JAVA_MAIN_CLASS
 * in various situations
 *
 * @author Oliver Weise
 */
class JavaExecGeneratorMainClassDeterminationTest {
    private KitLogger log;
    private JavaProject project;
    private FatJarDetector fatJarDetector;
    private FatJarDetector.Result fatJarDetectorResult;
    private MainClassDetector mainClassDetector;
    private ProcessorConfig processorConfig;
    @BeforeEach
    public void setUp() {
        log = mock(KitLogger.class);
        project = mock(JavaProject.class);
        fatJarDetector = mock(FatJarDetector.class);
        fatJarDetectorResult = mock(FatJarDetector.Result.class);
        mainClassDetector = mock(MainClassDetector.class);
        processorConfig = new ProcessorConfig();
        when(project.getVersion()).thenReturn("1.33.7-SNAPSHOT");
        when(project.getOutputDirectory()).thenReturn(new File("/the/output/directory"));
    }


    /**
     * The main class is determined via config in a non-fat-jar deployment
     *
     */
    @Test
    void testMainClassDeterminationFromConfig() {
        // Given
        final Map<String, Object> configurations = new HashMap<>();
        configurations.put("mainClass", "the.main.ClassName");
        configurations.put("name", "TheImageName");
        processorConfig.getConfig().put("java-exec", configurations);
        final GeneratorContext generatorContext = GeneratorContext.builder()
                .project(project)
                .config(processorConfig)
                .strategy(JKubeBuildStrategy.docker)
                .logger(log)
                .build();

        JavaExecGenerator generator = new JavaExecGenerator(generatorContext);

        List<ImageConfiguration> customized = generator.customize(new ArrayList<>(), false);

        assertThat(customized).singleElement()
                        .hasFieldOrPropertyWithValue("name", "TheImageName")
                        .extracting(ImageConfiguration::getBuildConfiguration)
                        .extracting(BuildConfiguration::getEnv)
                        .asInstanceOf(InstanceOfAssertFactories.MAP)
                        .as("Main Class set as environment variable")
                        .containsEntry("JAVA_MAIN_CLASS", "the.main.ClassName");
    }

    /**
     * The main class is determined via main class detection in a non-fat-jar deployment
     *
     */
    @Test
    void testMainClassDeterminationFromDetectionOnNonFatJar() {
        File baseDir = mock(File.class);
        processorConfig.getConfig().put("java-exec", Collections.singletonMap("name", "TheNonFatJarImageName"));
        when(project.getBaseDirectory()).thenReturn(baseDir);
        when(fatJarDetector.scan()).thenReturn(null);
        when(mainClassDetector.getMainClass()).thenReturn("the.detected.MainClass");
        final GeneratorContext generatorContext = GeneratorContext.builder()
                .project(project)
                .config(processorConfig)
                .strategy(JKubeBuildStrategy.docker)
                .logger(log)
                .build();

        JavaExecGenerator generator = new JavaExecGenerator(generatorContext);

        List<ImageConfiguration> customized = generator.customize(new ArrayList<>(), false);

        assertThat(customized).singleElement()
                .hasFieldOrPropertyWithValue("name", "TheNonFatJarImageName")
                .extracting(ImageConfiguration::getBuildConfiguration)
                .extracting(BuildConfiguration::getEnv)
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .as("Main Class set as environment variable")
                .containsEntry("JAVA_MAIN_CLASS", "the.detected.MainClass");
    }

    /**
     * The main class is determined as the Main-Class of a fat jar
     *
     */
    @Test
    void testMainClassDeterminationFromFatJar() {
        try(MockedStatic<FileUtil> fileUtil = Mockito.mockStatic(FileUtil.class)){
            File baseDir = mock(File.class);
            File fatJarArchive = mock(File.class);
            processorConfig.getConfig().put("java-exec", Collections.singletonMap("name", "TheFatJarImageName"));
            when(project.getBaseDirectory()).thenReturn(baseDir);
            fileUtil.when(() -> FileUtil.getRelativePath(any(File.class), any(File.class))).thenReturn(baseDir);
            when(fatJarDetector.scan()).thenReturn(fatJarDetectorResult);
            when(fatJarDetectorResult.getArchiveFile()).thenReturn(fatJarArchive);
            when(fatJarDetector.scan()).thenReturn(fatJarDetectorResult);
            when(fatJarDetectorResult.getArchiveFile()).thenReturn(fatJarArchive);
            final GeneratorContext generatorContext = GeneratorContext.builder()
                    .project(project)
                    .config(processorConfig)
                    .strategy(JKubeBuildStrategy.docker)
                    .logger(log)
                    .build();

            JavaExecGenerator generator = new JavaExecGenerator(generatorContext);

            List<ImageConfiguration> customized = generator.customize(new ArrayList<>(), false);

            assertThat(customized).singleElement()
                    .hasFieldOrPropertyWithValue("name", "TheFatJarImageName")
                    .extracting(ImageConfiguration::getBuildConfiguration)
                    .extracting(BuildConfiguration::getEnv)
                    .asInstanceOf(InstanceOfAssertFactories.MAP)
                    .as("Main Class is NOT set as environment variable#")
                    .doesNotContainEntry("JAVA_MAIN_CLASS", null);
        }
    }

}
