/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.test.contexts.session.tests;

import java.util.ArrayList;
import java.util.Collection;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;

import org.junit.Assert;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.contexts.session.common.AppScopedBean;
import org.apache.webbeans.test.contexts.session.common.PersonalDataBean;
import org.apache.webbeans.test.injection.circular.beans.CircularApplicationScopedBean;
import org.apache.webbeans.test.injection.circular.beans.CircularDependentScopedBean;
import org.junit.Test;

public class SessionContextTest extends AbstractUnitTest
{
    public SessionContextTest()
    {
        
    }
    
    @Test
    public void testPersonalDataBean()
    {
        Collection<Class<?>> classes = new ArrayList<Class<?>>();
        classes.add(PersonalDataBean.class);
        classes.add(CircularDependentScopedBean.class);
        classes.add(CircularApplicationScopedBean.class);
        
        startContainer(classes);
        
        PersonalDataBean.POST_CONSTRUCT = false;
        PersonalDataBean.PRE_DESTROY = false;

        PersonalDataBean dataBean = getInstance(PersonalDataBean.class);
        Assert.assertNotNull(dataBean);
        
        dataBean.business();
        
        Assert.assertTrue(PersonalDataBean.POST_CONSTRUCT);
                
        shutDownContainer();
        
        Assert.assertTrue(PersonalDataBean.PRE_DESTROY);
    }

    @Test
    public void testInstanceCreation()
    {
        Collection<Class<?>> classes = new ArrayList<Class<?>>();
        classes.add(PersonalDataBean.class);
        classes.add(AppScopedBean.class);
        classes.add(CircularDependentScopedBean.class);
        classes.add(CircularApplicationScopedBean.class);

        AppScopedBean.appContextInitializedEvent.clear();
        AppScopedBean.appContextDestroyedEvent.clear();
        AppScopedBean.sessionContextInitializedEvent.clear();
        AppScopedBean.sessionContextDestroyedEvent.clear();
        AppScopedBean.requestContextInitializedEvent.clear();
        AppScopedBean.requestContextDestroyedEvent.clear();

        startContainer(classes);
        
        AppScopedBean appBeanInstance = getInstance(AppScopedBean.class);
        Assert.assertNotNull(appBeanInstance);
        PersonalDataBean pdb1 = appBeanInstance.getPdb().getInstance();
        Assert.assertNotNull(pdb1);
        
        // now we reset the session Context so we should get a new contextual instance.
        getWebBeansContext().getContextsService().endContext(SessionScoped.class, null);
        getWebBeansContext().getContextsService().endContext(RequestScoped.class, null);
        getWebBeansContext().getContextsService().startContext(RequestScoped.class, null);
        getWebBeansContext().getContextsService().startContext(SessionScoped.class, null);

        PersonalDataBean pdb2 = appBeanInstance.getPdb().getInstance();
        Assert.assertNotNull(pdb2);
        
        // pdb1 and pdb2 are in different sessions, so they must not be the same instance!
        Assert.assertTrue(pdb1 != pdb2);

        Assert.assertEquals(1, AppScopedBean.appContextInitializedEvent.size());
        Assert.assertEquals(2, AppScopedBean.sessionContextInitializedEvent.size());
        Assert.assertEquals(1, AppScopedBean.sessionContextDestroyedEvent.size());
        Assert.assertEquals(3, AppScopedBean.requestContextInitializedEvent.size());
        Assert.assertEquals(2, AppScopedBean.requestContextDestroyedEvent.size());

        shutDownContainer();
        Assert.assertEquals(4, AppScopedBean.requestContextDestroyedEvent.size());
        Assert.assertEquals(2, AppScopedBean.sessionContextDestroyedEvent.size());

        Assert.assertEquals(1, AppScopedBean.appContextDestroyedEvent.size());
    }
}
