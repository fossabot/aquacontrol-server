/**
 * Copyright © 2017 albahrani (https://github.com/albahrani)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.albahrani.aquacontrol.server;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.mockito.InOrder;

import com.github.albahrani.aquacontrol.core.LightEnvironment;
import com.github.albahrani.aquacontrol.core.environment.LightEnvironmentChannelConfiguration;
import com.github.albahrani.aquacontrol.core.environment.LightEnvironmentConfiguration;
import com.github.albahrani.aquacontrol.core.environment.PWMControllerConnector;
import com.github.albahrani.aquacontrol.core.environment.production.PCA9685Connector;
import com.pi4j.io.gpio.Pin;

public class LightServerTest {

	@Test
	public void testStartLightServer(){
		PWMControllerConnector pwmControllerConnector = mock(PWMControllerConnector.class);
		Optional<LightServerController> optionalServer = LightServer.initLightServer(new String[]{}, pwmControllerConnector);
		assertNotNull(optionalServer);
		assertTrue(optionalServer.isPresent());
		LightServerController server = optionalServer.get();
		server.start();
		server.shutdown();
		
		verify(pwmControllerConnector).shutdown();
		verifyNoMoreInteractions(pwmControllerConnector);
	}
	
	@Test
	public void testLoadConfigNull() {
		PCA9685Connector connector = mock(PCA9685Connector.class);
		try {
			LightServer.createEnvironmentFromConfiguration(null, connector);
			fail("Expected NullPointerException");
		} catch (NullPointerException e) {
			// ignore
		}
	}

	@Test
	public void testLoadConfigWithChannelConfigNull() {
		PCA9685Connector connector = mock(PCA9685Connector.class);

		LightEnvironmentConfiguration config = mock(LightEnvironmentConfiguration.class);
		when(config.getChannelConfig()).thenReturn(null);
		LightServer.createEnvironmentFromConfiguration(config, connector);
		// expect no exception
	}

	@Test
	public void testLoadConfigWithChannelConfigEmpty() {
		PCA9685Connector connector = mock(PCA9685Connector.class);

		LightEnvironmentConfiguration config = mock(LightEnvironmentConfiguration.class);

		List<LightEnvironmentChannelConfiguration> channelConfig = new ArrayList<>();

		when(config.getChannelConfig()).thenReturn(channelConfig);
		LightServer.createEnvironmentFromConfiguration(config, connector);
		// expect no exception
	}

	@Test
	public void testLoadConfig() {
		PCA9685Connector connector = mock(PCA9685Connector.class);

		LightEnvironmentConfiguration config = mock(LightEnvironmentConfiguration.class);

		LightEnvironmentChannelConfiguration entry = mock(LightEnvironmentChannelConfiguration.class);
		when(entry.getId()).thenReturn("Channel1");
		List<String> pins = new ArrayList<>();
		pins.add("PWM 0");
		when(entry.getPins()).thenReturn(pins);

		List<LightEnvironmentChannelConfiguration> channelConfig = new ArrayList<>();
		channelConfig.add(entry);

		when(config.getChannelConfig()).thenReturn(channelConfig);
		LightEnvironment env = LightServer.createEnvironmentFromConfiguration(config, connector);

		env.channel("Channel1").percentage(100);
		env.channel("Channel1").percentage(50);
		env.channel("Channel1").percentage(0);

		verify(connector).provisionPwmOutputPin(any(Pin.class));
		InOrder inOrder = inOrder(connector);
		inOrder.verify(connector).setPwmValue(anySet(), eq(4095));
		inOrder.verify(connector).setPwmValue(anySet(), eq(2047));
		inOrder.verify(connector).setPwmValue(anySet(), eq(0));
	}

}
