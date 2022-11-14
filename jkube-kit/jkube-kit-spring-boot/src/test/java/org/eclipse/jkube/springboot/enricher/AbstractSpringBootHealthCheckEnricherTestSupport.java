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
package org.eclipse.jkube.springboot.enricher;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;

import io.fabric8.kubernetes.api.model.Probe;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.SpringBootConfigurationHelper;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.common.util.ProjectClassLoaders;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.springboot.enricher.SpringBootHealthCheckEnricher.REQUIRED_CLASSES;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Check various configurations for spring-boot health checks
 *
 * @author nicola
 */
public abstract class AbstractSpringBootHealthCheckEnricherTestSupport {

    @Rule
    public TemporaryFolder project = new TemporaryFolder();

    protected JKubeEnricherContext context;

    protected SpringBootConfigurationHelper propertyHelper;

    private ProjectClassLoaders projectClassLoaders;

    @Before
    public void init() throws IOException {
        projectClassLoaders = mock(ProjectClassLoaders.class, RETURNS_DEEP_STUBS);
        context = spy(JKubeEnricherContext.builder()
          .log(new KitLogger.SilentLogger())
          .project(JavaProject.builder()
            .baseDirectory(project.getRoot())
            .outputDirectory(project.newFolder("target"))
            .groupId("com.example")
            .artifactId("foo")
            .dependenciesWithTransitive(Collections.singletonList(Dependency.builder()
                .groupId("org.springframework.boot")
                .artifactId("spring-boot")
                .version(getSpringBootVersion())
              .build()))
            .build())
          .processorConfig(new ProcessorConfig())
          .build());
        when(context.getProjectClassLoaders()).thenReturn(projectClassLoaders);
        propertyHelper = new SpringBootConfigurationHelper(Optional.of(getSpringBootVersion()));
    }
    protected abstract String getSpringBootVersion();

    @Test
    public void testZeroConfig() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(propertyHelper.getActuatorDefaultBasePath() + "/health").isEqualTo(probe.getHttpGet().getPath());
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8080);
    }

    @Test
    public void testWithServerPort() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(propertyHelper.getActuatorDefaultBasePath() + "/health").isEqualTo(probe.getHttpGet().getPath());
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8282);
    }

    @Test
    public void testWithServerPortAndManagementPort() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(propertyHelper.getActuatorDefaultBasePath() + "/health").isEqualTo(probe.getHttpGet().getPath());
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8383);
    }

    @Test
    public void testWithServerPortAndManagementPortAndServerContextPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p1");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(propertyHelper.getActuatorDefaultBasePath() + "/health").isEqualTo(probe.getHttpGet().getPath());
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8383);
    }

    @Test
    public void testWithServerPortAndManagementPortAndServerContextPathAndActuatorDefaultBasePath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/p2");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/p2" + "/health");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8383);
    }

    @Test
    public void testWithServerPortAndManagementPortAndServerContextPathAndActuatorDefaultBasePathSlash() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/health");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8383);
    }

    @Test
    public void testWithServerPortAndManagementPortAndManagementContextPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p2");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat( probe.getHttpGet().getPath()).isEqualTo("/p2" + propertyHelper.getActuatorDefaultBasePath() + "/health");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8383);
    }

    @Test
    public void testWithServerPortAndManagementPortAndManagementContextPathAndActuatorDefaultBasePath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p2");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/p3");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/p2/p3" + "/health");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8383);
    }

    @Test
    public void testWithServerPortAndManagementPortAndManagementContextPathAndActuatorDefaultBasePathSlash() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/health");
        assertThat( probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8383);
    }

    @Test
    public void testWithServerPortAndManagementPortAndManagementContextPathAndServletPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p2");
        props.put(propertyHelper.getServletPathPropertyKey(), "/servlet");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/p2" + propertyHelper.getActuatorDefaultBasePath() + "/health");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8383);
    }

    @Test
    public void testWithServerPortAndManagementPortAndManagementContextPathAndServletPathAndActuatorDefaultBasePath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p2");
        props.put(propertyHelper.getServletPathPropertyKey(), "/servlet");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/p3");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/p2/p3" + "/health");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8383);
    }

    @Test
    public void testWithServerPortAndManagementPortAndManagementContextPathAndServletPathAndActuatorDefaultBasePathSlash() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/");
        props.put(propertyHelper.getServletPathPropertyKey(), "/servlet");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/health");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8383);
    }

    @Test
    public void testWithServerPortAndManagementContextPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p1");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/p1" + propertyHelper.getActuatorDefaultBasePath() +"/health");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8282);
    }

    @Test
    public void testWithServerPortAndManagementContextPathAndActuatorDefaultBasePath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/p2");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/p1/p2" +"/health");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8282);
    }

    @Test
    public void testWithServerPortAndManagementContextPathAndActuatorDefaultBasePathSlash() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/health");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8282);
    }

    @Test
    public void testWithServerPortAndServerContextPathAndManagementContextPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p2");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/p2/p1" + propertyHelper.getActuatorDefaultBasePath() + "/health");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8282);
    }

    @Test
    public void testWithServerPortAndServerContextPathAndManagementContextPathAndActuatorDefaultBasePath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p2");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/p3");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/p2/p1/p3" + "/health");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8282);
    }

    @Test
    public void testWithServerPortAndServerContextPathAndManagementContextPathAndActuatorDefaultBasePathSlash() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/health");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8282);
    }

    @Test
    public void testWithServerPortAndServletPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getServletPathPropertyKey(), "/servlet");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/servlet" + propertyHelper.getActuatorDefaultBasePath() + "/health");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8282);
    }

    @Test
    public void testWithServerPortAndServletAndActuatorDefaultBasePathPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getServletPathPropertyKey(), "/servlet");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/p1");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/servlet/p1" + "/health");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8282);
    }

    @Test
    public void testWithServerPortAndServletPathAndActuatorDefaultBasePathSlash() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getServletPathPropertyKey(), "/");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/health");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8282);
    }

    @Test
    public void testWithServerPortAndManagementContextPathAndServletPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getServletPathPropertyKey(), "/servlet");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p1");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/servlet/p1" + propertyHelper.getActuatorDefaultBasePath() + "/health");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8282);
    }

    @Test
    public void testWithServerPortAndManagementContextPathAndServletPathAndActuatorDefaultBasePath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getServletPathPropertyKey(), "/servlet");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/p2");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/servlet/p1/p2" + "/health");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8282);
    }

    @Test
    public void testWithServerPortAndManagementContextPathAndServletPathAndActuatorDefaultBasePathSlash() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getServletPathPropertyKey(), "/");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/health");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8282);
    }

    @Test
    public void testWithServerPortAndServerContextPathAndManagementContextPathAndServletPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getServletPathPropertyKey(), "/servlet");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p2");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/p2/servlet/p1" + propertyHelper.getActuatorDefaultBasePath() + "/health");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8282);
    }

    @Test
    public void testWithServerPortAndServerContextPathAndManagementContextPathAndServletPathAndActuatorDefaultBasePath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getServletPathPropertyKey(), "/servlet");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p2");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/p3");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/p2/servlet/p1/p3" + "/health");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8282);
    }

    @Test
    public void testWithServerPortAndServerContextPathAndManagementContextPathAndServletPathAndActuatorDefaultBasePathSlash() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getServletPathPropertyKey(), "/");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/health");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8282);
    }

    @Test
    public void testSchemeWithServerPort() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8443");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getHttpGet().getScheme()).isEqualTo("HTTP");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8443);
    }

    @Test
    public void testSchemeWithServerPortAndManagementKeystore() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8080");
        props.put(propertyHelper.getManagementKeystorePropertyKey(), "classpath:keystore.p12");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getHttpGet().getScheme()).isEqualTo("HTTP");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8080);
    }

    @Test
    public void testSchemeWithServerPortAndServerKeystore() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8443");
        props.put(propertyHelper.getServerKeystorePropertyKey(), "classpath:keystore.p12");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getHttpGet().getScheme()).isEqualTo("HTTPS");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8443);
    }

    @Test
    public void testSchemeWithServerPortAndManagementPortAndServerKeystore() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8443");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8081");
        props.put(propertyHelper.getServerKeystorePropertyKey(), "classpath:keystore.p12");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat( probe.getHttpGet().getScheme()).isEqualTo("HTTP");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8081);
    }

    @Test
    public void testSchemeWithServerPortAndManagementPortAndManagementKeystore() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8080");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8443");
        props.put(propertyHelper.getManagementKeystorePropertyKey(), "classpath:keystore.p12");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getHttpGet().getScheme()).isEqualTo("HTTPS");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8443);
    }

    @Test
    public void testDefaultInitialDelayForLivenessAndReadiness() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        when(context.getProjectClassLoaders().isClassInCompileClasspath(true, REQUIRED_CLASSES))
          .thenReturn(true);
        when(context.getProjectClassLoaders().getCompileClassLoader())
          .thenReturn(new URLClassLoader(new URL[0], AbstractSpringBootHealthCheckEnricherTestSupport.class.getClassLoader()));
        Probe probe = enricher.getReadinessProbe();
        assertThat(probe).isNotNull();
        assertThat(probe.getInitialDelaySeconds().intValue()).isEqualTo(10);

        probe = enricher.getLivenessProbe();
        assertThat(probe).isNotNull();
        assertThat(probe.getInitialDelaySeconds().intValue()).isEqualTo(180);
    }

    @Test
    public void testCustomInitialDelayForLivenessAndReadinessAndTimeout() {
        TreeMap<String, Object> enricherConfig = new TreeMap<>();
        enricherConfig.put("readinessProbeInitialDelaySeconds", "20");
        enricherConfig.put("livenessProbeInitialDelaySeconds", "360");
        enricherConfig.put("timeoutSeconds", "120");
        context.getConfiguration().getProcessorConfig()
          .setConfig(Collections.singletonMap("jkube-healthcheck-spring-boot", enricherConfig));
        when(projectClassLoaders.isClassInCompileClasspath(true, REQUIRED_CLASSES))
          .thenReturn(true);
        when(projectClassLoaders.getCompileClassLoader())
          .thenReturn(new URLClassLoader(new URL[0], AbstractSpringBootHealthCheckEnricherTestSupport.class.getClassLoader()));

        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);

        Probe probe = enricher.getReadinessProbe();
        assertThat(probe).isNotNull();
        assertThat(probe.getInitialDelaySeconds().intValue()).isEqualTo(20);
        assertThat(probe.getPeriodSeconds()).isNull();
        assertThat(probe.getTimeoutSeconds().intValue()).isEqualTo(120);
        probe = enricher.getLivenessProbe();
        assertThat(probe).isNotNull();
        assertThat(probe.getInitialDelaySeconds().intValue()).isEqualTo(360);
        assertThat(probe.getPeriodSeconds()).isNull();
        assertThat(probe.getTimeoutSeconds().intValue()).isEqualTo(120);
    }

    @Test
    public void testCustomPropertiesForLivenessAndReadiness() {
        TreeMap<String, Object> enricherConfig = new TreeMap<>();
        enricherConfig.put("readinessProbeInitialDelaySeconds", "30");
        enricherConfig.put("readinessProbePeriodSeconds", "40");
        enricherConfig.put("livenessProbeInitialDelaySeconds", "460");
        enricherConfig.put("livenessProbePeriodSeconds", "50");
        context.getConfiguration().getProcessorConfig()
          .setConfig(Collections.singletonMap("jkube-healthcheck-spring-boot", enricherConfig));
        when(projectClassLoaders.isClassInCompileClasspath(true, REQUIRED_CLASSES))
          .thenReturn(true);
        when(projectClassLoaders.getCompileClassLoader())
          .thenReturn(new URLClassLoader(new URL[0], AbstractSpringBootHealthCheckEnricherTestSupport.class.getClassLoader()));

        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);

        Probe probe = enricher.getReadinessProbe();
        assertThat(probe).isNotNull();
        assertThat(probe.getInitialDelaySeconds().intValue()).isEqualTo(30);
        assertThat(probe.getPeriodSeconds().intValue()).isEqualTo(40);

        probe = enricher.getLivenessProbe();
        assertThat(probe).isNotNull();
        assertThat(probe.getInitialDelaySeconds().intValue()).isEqualTo(460);
        assertThat(probe.getPeriodSeconds().intValue()).isEqualTo(50);
    }

}
