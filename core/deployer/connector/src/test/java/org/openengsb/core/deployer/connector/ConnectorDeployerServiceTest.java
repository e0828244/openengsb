/**
 * Licensed to the Austrian Association for Software Tool Integration (AASTI)
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. The AASTI licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openengsb.core.deployer.connector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.openengsb.core.common.ServiceManager;
import org.openengsb.core.common.validation.MultipleAttributeValidationResult;
import org.openengsb.core.deployer.connector.internal.DeployerStorage;
import org.openengsb.core.test.AbstractOsgiMockServiceTest;
import org.osgi.framework.Constants;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

public class ConnectorDeployerServiceTest extends AbstractOsgiMockServiceTest {

    private ConnectorDeployerService connectorDeployerService;
    private AuthenticationManager authManagerMock;
    private Authentication authMock;
    private ServiceManager serviceManagerMock;
    private DeployerStorage storageMock;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        connectorDeployerService = new ConnectorDeployerService();
        authManagerMock = mock(AuthenticationManager.class);
        authMock = mock(Authentication.class);
        serviceManagerMock = mock(ServiceManager.class);
        storageMock = mock(DeployerStorage.class);

        when(authManagerMock.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authMock);
        registerService(serviceManagerMock, ServiceManager.class, "(connector=a-connector )");

        connectorDeployerService.setAuthenticationManager(authManagerMock);
        connectorDeployerService.setBundleContext(bundleContext);
        connectorDeployerService.setDeployerStorage(storageMock);
    }

    @Test
    public void testConnectorFiles_shouldBeHandledByDeployer() throws IOException {
        File connectorFile = temporaryFolder.newFile("example.connector");

        assertThat(connectorDeployerService.canHandle(connectorFile), is(true));
    }

    @Test
    public void testUnknownFiles_shouldNotBeHandledByDeplyoer() throws IOException {
        File otherFile = temporaryFolder.newFile("other.txt");

        assertThat(connectorDeployerService.canHandle(otherFile), is(false));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testConnectorFile_shouldBeInstalled() throws Exception {
        File connectorFile = temporaryFolder.newFile("example.connector");
        FileUtils.writeStringToFile(connectorFile, "connector=a-connector \n id=service-id \n a-key=a-value");
        MultipleAttributeValidationResult updateResult = mock(MultipleAttributeValidationResult.class);

        when(updateResult.isValid()).thenReturn(true);
        when(serviceManagerMock.update(anyString(), anyMap())).thenReturn(updateResult);

        connectorDeployerService.install(connectorFile);

        verify(serviceManagerMock).update(anyString(), argThat(new IsSomething()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateConnectorFile_shouldBeUpdated() throws Exception {
        File connectorFile = temporaryFolder.newFile("example.connector");
        FileUtils.writeStringToFile(connectorFile, "connector=a-connector \n id=service-id \n a-key=a-value");
        MultipleAttributeValidationResult updateResult = mock(MultipleAttributeValidationResult.class);

        when(updateResult.isValid()).thenReturn(true);
        when(serviceManagerMock.update(anyString(), anyMap())).thenReturn(updateResult);

        connectorDeployerService.update(connectorFile);

        verify(serviceManagerMock).update(anyString(), argThat(new IsSomething()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRootService_shouldHaveLowerRanking() throws Exception {
        File connectorFile = new File(temporaryFolder.getRoot() + "/etc/a_root.connector");
        FileUtils.touch(connectorFile);
        FileUtils.writeStringToFile(connectorFile, "connector=a-connector \n id=service-id \n a-key=a-value");
        MultipleAttributeValidationResult updateResult = mock(MultipleAttributeValidationResult.class);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Map> attributesCaptor = ArgumentCaptor.forClass(Map.class);

        when(updateResult.isValid()).thenReturn(true);
        when(serviceManagerMock.update(anyString(), anyMap())).thenReturn(updateResult);

        connectorDeployerService.update(connectorFile);

        verify(serviceManagerMock).update(anyString(), attributesCaptor.capture());
        assertThat(attributesCaptor.getValue().containsKey(Constants.SERVICE_RANKING), is(true));
        assertThat(attributesCaptor.getValue().get(Constants.SERVICE_RANKING).toString(), is("-1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNormalService_shouldHaveNoRankingAdded() throws Exception {
        File connectorFile = new File(temporaryFolder.getRoot() + "/config/a_root.connector");
        FileUtils.touch(connectorFile);
        FileUtils.writeStringToFile(connectorFile, "connector=a-connector \n id=service-id \n a-key=a-value");
        MultipleAttributeValidationResult updateResult = mock(MultipleAttributeValidationResult.class);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Map> attributesCaptor = ArgumentCaptor.forClass(Map.class);

        when(updateResult.isValid()).thenReturn(true);
        when(serviceManagerMock.update(anyString(), anyMap())).thenReturn(updateResult);

        connectorDeployerService.update(connectorFile);

        verify(serviceManagerMock).update(anyString(), attributesCaptor.capture());
        assertThat(attributesCaptor.getValue().containsKey(Constants.SERVICE_RANKING), is(false));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOverridenRanking_shouldNotBeAltered() throws Exception {
        File connectorFile = new File(temporaryFolder.getRoot() + "/etc/a_root.connector");
        FileUtils.touch(connectorFile);
        FileUtils.writeStringToFile(connectorFile, "connector=a-connector \n id=service-id \n a-key=a-value \n "
                + Constants.SERVICE_RANKING + "=24");
        MultipleAttributeValidationResult updateResult = mock(MultipleAttributeValidationResult.class);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Map> attributesCaptor = ArgumentCaptor.forClass(Map.class);

        when(updateResult.isValid()).thenReturn(true);
        when(serviceManagerMock.update(anyString(), anyMap())).thenReturn(updateResult);

        connectorDeployerService.update(connectorFile);

        verify(serviceManagerMock).update(anyString(), attributesCaptor.capture());
        assertThat(attributesCaptor.getValue().containsKey(Constants.SERVICE_RANKING), is(true));
        assertThat(attributesCaptor.getValue().get(Constants.SERVICE_RANKING).toString(), is("24"));
    }

    @Test
    public void testRemoveConnectorFile_shouldRemoveConnector() throws Exception {
        File connectorFile = temporaryFolder.newFile("example.connector");

        when(storageMock.getConnectorType(connectorFile)).thenReturn("a-connector ");
        when(storageMock.getServiceId(any(File.class))).thenReturn("service-id");

        connectorDeployerService.uninstall(connectorFile);

        verify(serviceManagerMock).delete("service-id");
    }

    class IsSomething extends ArgumentMatcher<Map<String, String>> {
        @Override
        public boolean matches(Object o) {
            return true;
        }
    }

}
