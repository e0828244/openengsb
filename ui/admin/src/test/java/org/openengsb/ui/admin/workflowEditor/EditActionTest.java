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

package org.openengsb.ui.admin.workflowEditor;

import static junit.framework.Assert.assertEquals;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.apache.wicket.spring.test.ApplicationContextMock;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openengsb.core.common.Domain;
import org.openengsb.core.common.DomainProvider;
import org.openengsb.core.common.context.ContextCurrentService;
import org.openengsb.core.common.service.DomainService;
import org.openengsb.core.common.workflow.editor.Action;
import org.openengsb.core.common.workflow.editor.WorkflowEditorService;
import org.openengsb.core.test.NullDomain;
import org.openengsb.ui.admin.model.OpenEngSBVersion;
import org.openengsb.ui.admin.workflowEditor.action.EditAction;

public class EditActionTest {

    private WicketTester tester;

    private FormTester formTester;

    private Action action;

    private Action parent;

    private ApplicationContextMock mock;

    @Before
    public void setup() {
        parent = new Action();
        action = new Action();
        action.setLocation("test");
        tester = new WicketTester();
        mock = new ApplicationContextMock();
        mock.putBean(mock(ContextCurrentService.class));
        mock.putBean("openengsbVersion", new OpenEngSBVersion());
        mock.putBean("workflowEditorService", mock(WorkflowEditorService.class));
        List<DomainProvider> domainProviders = new ArrayList<DomainProvider>();
        DomainProvider provider = mock(DomainProvider.class);
        when(provider.getDomainInterface()).thenAnswer(new Answer<Class<? extends Domain>>() {
            @Override
            public Class<? extends Domain> answer(InvocationOnMock invocation) throws Throwable {
                return NullDomain.class;
            }
        });
        domainProviders.add(provider);
        DomainService domainServiceMock = mock(DomainService.class);
        when(domainServiceMock.domains()).thenReturn(domainProviders);
        mock.putBean("domainService", domainServiceMock);
        tester.getApplication().addComponentInstantiationListener(
            new SpringComponentInjector(tester.getApplication(), mock, true));
        tester.startPage(new EditAction(parent, action));
        formTester = tester.newFormTester("actionForm");
    }

    @Test
    public void editForm_shouldUpdateAction() {
        assertThat(parent.getActions().size(), equalTo(0));
        String location = "location";
        assertThat(action.getLocation(), equalTo(formTester.getTextComponentValue(location)));
        tester.dumpPage();
        formTester.setValue(location, location);
        formTester.select("domainSelect", 0);
        formTester.submit();
        formTester = tester.newFormTester("actionForm");
        formTester.select("methodSelect", 1);
        formTester.submit();
        tester.assertRenderedPage(WorkflowEditor.class);
        assertThat(action.getLocation(), equalTo(location));
        assertEquals(action.getDomain(), NullDomain.class);
        assertThat(action.getMethodName(), equalTo(NullDomain.class.getMethods()[1].getName()));
        assertThat(parent.getActions().size(), equalTo(1));
        assertThat(parent.getActions().get(0), sameInstance(action));
    }

    @Test
    public void cancelButton_shouldWhoWorkflowPage() {
        formTester.submit("cancel-button");
        assertThat(parent.getActions().size(), equalTo(0));
        tester.assertRenderedPage(WorkflowEditor.class);
    }
}
