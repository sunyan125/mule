/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.PostProcessor;
import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.routing.MessageFilter;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class MessageFilterDefinitionParser extends MessageProcessorDefinitionParser implements PostProcessor
{
    public MessageFilterDefinitionParser()
    {
        super(MessageFilter.class);
        addIgnored(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME);
        addIgnored("onNotAccepted");
        registerPostProcessor(this);
    }

    public void postProcess(ParserContext context, BeanAssembler assembler, Element element)
    {
        String onNotAccepted = element.getAttribute("onNotAccepted");
        if (onNotAccepted != null)
        {
            assembler.extendBean("unacceptedMessageProcessor", onNotAccepted, true);
        }
    }
}
