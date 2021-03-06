/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.issues;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class AsyncComponentLifecycleIssue5649TestCase extends FunctionalTestCase implements FunctionalTestComponent.LifecycleCallback
{
    List<String> componentPhases = new ArrayList<String>();

    @Override
    protected MuleContext createMuleContext() throws Exception
    {
        componentPhases.clear();
        FunctionalTestComponent.addLifecycleCallback(this);
        return super.createMuleContext();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected void doTearDown() throws Exception
    {
        FunctionalTestComponent.removeLifecycleCallback(this);
    }

    @Override
    protected String getConfigResources()
    {
        return "org/mule/issues/async-component-lifecycle-5649.xml";
    }

    @Test
    public void testConfig() throws Exception
    {
        muleContext.stop();
        muleContext.dispose();
        FunctionalTestComponent.removeLifecycleCallback(this);
        System.out.println(componentPhases);
        assertEquals(4, componentPhases.size());
        assertEquals(Initialisable.PHASE_NAME, componentPhases.get(0));
        assertEquals(Startable.PHASE_NAME, componentPhases.get(1));
        assertEquals(Stoppable.PHASE_NAME, componentPhases.get(2));
        assertEquals(Disposable.PHASE_NAME, componentPhases.get(3));
    }

    public void onTransition(String name, String newPhase)
    {
        componentPhases.add(newPhase);
    }
}
