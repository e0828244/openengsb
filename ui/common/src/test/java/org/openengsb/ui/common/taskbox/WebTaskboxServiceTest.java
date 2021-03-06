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

package org.openengsb.ui.common.taskbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.Before;
import org.junit.Test;
import org.openengsb.core.common.persistence.PersistenceException;
import org.openengsb.core.common.persistence.PersistenceManager;
import org.openengsb.core.common.persistence.PersistenceService;
import org.openengsb.core.common.taskbox.TaskboxException;
import org.openengsb.core.common.taskbox.model.Task;
import org.openengsb.core.common.workflow.WorkflowService;
import org.openengsb.ui.common.taskbox.web.CustomTaskPanel;
import org.openengsb.ui.common.taskbox.web.TaskPanel;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class WebTaskboxServiceTest {
    private WebTaskboxServiceImpl service;
    private PersistenceService persistenceService;
    private WorkflowService workflowService;

    @SuppressWarnings("unused")
    private WicketTester tester;

    @Before
    public void init() throws Exception {
        tester = new WicketTester();

        workflowService = mock(WorkflowService.class);
        persistenceService = mock(PersistenceService.class);
        PersistenceManager persistenceManager = mock(PersistenceManager.class);
        when(persistenceManager.getPersistenceForBundle(any(Bundle.class))).thenReturn(persistenceService);

        service = new WebTaskboxServiceImpl();
        service.setBundleContext(mock(BundleContext.class));
        service.setWorkflowService(workflowService);
        service.setPersistenceManager(persistenceManager);
        service.init();
    }

    @Test
    public void testGetTaskPanel_shouldReturnDefaultPanel() throws PersistenceException {
        Task t = new Task();
        t.setTaskType("Type1");
        Panel p = null;

        try {
            p = service.getTaskPanel(t, "panel");
        } catch (TaskboxException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertEquals(p.getClass(), TaskPanel.class);
    }

    @Test
    public void testGetRegisteredTaskPanel_shouldReturnCustomPanel() throws PersistenceException {
        List<PanelRegistryEntry> list = new ArrayList<PanelRegistryEntry>();
        list.add(new PanelRegistryEntry("Type1", CustomTaskPanel.class));

        when(persistenceService.query(any(PanelRegistryEntry.class))).thenReturn(list);

        Task t = new Task();
        t.setTaskType("Type1");
        Panel p = null;

        try {
            p = service.getTaskPanel(t, "panel");
        } catch (TaskboxException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertEquals(p.getClass(), CustomTaskPanel.class);
    }
}
