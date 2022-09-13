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
package org.eclipse.jkube.kit.enricher.api;

import java.util.Collections;
import java.util.Map;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author roland
 */
public class EnricherConfigTest {

  private enum Config implements Configs.Config {
    TYPE
  }

  @Test
  public void simple() {
    EnricherContext context = mock(EnricherContext.class,RETURNS_DEEP_STUBS);
    Map<String, Map<String, Object>> configMap = Collections.singletonMap("default.service",
        Collections.singletonMap("TYPE", "LoadBalancer"));
    when(context.getConfiguration().getProcessorConfig()).thenReturn(new ProcessorConfig(null, null, configMap));
    EnricherConfig config = new EnricherConfig("default.service", context);
    assertEquals("LoadBalancer", config.get(EnricherConfigTest.Config.TYPE));
  }
}
