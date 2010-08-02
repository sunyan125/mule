/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointMessageProcessorChainFactory;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.routing.filter.Filter;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connector;
import org.mule.routing.MessageFilter;
import org.mule.util.ClassUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.emory.mathcs.backport.java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.util.CollectionUtils;

/**
 * <code>ImmutableMuleEndpoint</code> describes a Provider in the Mule Server. A
 * endpoint is a grouping of an endpoint, an endpointUri and a transformer.
 */
public abstract class AbstractEndpoint implements ImmutableEndpoint
{

    private static final long serialVersionUID = -1650380871293160973L;

    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(AbstractEndpoint.class);

    /**
     * The endpoint used to communicate with the external system
     */
    private final Connector connector;

    /**
     * The endpointUri on which to send or receive information
     */
    private final EndpointURI endpointUri;

    private final EndpointMessageProcessorChainFactory messageProcessorsFactory;

    private final List <MessageProcessor> messageProcessors;

    private final List <MessageProcessor> responseMessageProcessors;
    
    private MessageProcessor messageProcessorChain;

    /**
     * The name for the endpoint
     */
    private final String name;

    /**
     * Any additional properties for the endpoint
     * // TODO This should be final. See MULE-3105
     * // TODO Shouldn't this be guarded from concurrent writes?
     */
    private Map properties = new HashMap();

    /**
     * The transaction configuration for this endpoint
     */
    private final TransactionConfig transactionConfig;

    /**
     * determines whether unaccepted filtered events should be removed from the
     * source. If they are not removed its up to the Message receiver to handle
     * recieving the same message again
     */
    private final boolean deleteUnacceptedMessages;

    private final MessageExchangePattern messageExchangePattern;
    
    /**
     * How long to block when performing a remote synchronisation to a remote host.
     * This property is optional and will be set to the default Synchonous MuleEvent
     * time out value if not set
     */
    private final int responseTimeout;

    /**
     * The state that the endpoint is initialised in such as started or stopped
     */
    private final String initialState;

    private final String endpointEncoding;

    private final MuleContext muleContext;

    protected RetryPolicyTemplate retryPolicyTemplate;

    private String endpointBuilderName;

    private final String endpointMimeType;

    private boolean disableTransportTransformer = false;
    
    public AbstractEndpoint(Connector connector,
                            EndpointURI endpointUri,
                            String name,
                            Map properties,
                            TransactionConfig transactionConfig,
                            boolean deleteUnacceptedMessages,
                            MessageExchangePattern messageExchangePattern,
                            int responseTimeout,
                            String initialState,
                            String endpointEncoding,
                            String endpointBuilderName,
                            MuleContext muleContext,
                            RetryPolicyTemplate retryPolicyTemplate,
                            EndpointMessageProcessorChainFactory messageProcessorsFactory,
                            List <MessageProcessor> messageProcessors,
                            List <MessageProcessor> responseMessageProcessors,
                            boolean disableTransportTransformer,
                            String endpointMimeType)
    {
        this.connector = connector;
        this.endpointUri = endpointUri;
        this.name = name;
        // TODO Properties should be immutable. See MULE-3105
        // this.properties = Collections.unmodifiableMap(properties);
        this.properties.putAll(properties);
        this.transactionConfig = transactionConfig;
        this.deleteUnacceptedMessages = deleteUnacceptedMessages;

        this.responseTimeout = responseTimeout;
        this.initialState = initialState;
        this.endpointEncoding = endpointEncoding;
        this.endpointBuilderName = endpointBuilderName;
        this.muleContext = muleContext;
        this.retryPolicyTemplate = retryPolicyTemplate;
        this.endpointMimeType = endpointMimeType;
        this.disableTransportTransformer = disableTransportTransformer;

        if (transactionConfig != null && transactionConfig.getFactory() != null &&
            transactionConfig.getAction() != TransactionConfig.ACTION_NONE &&
            transactionConfig.getAction() != TransactionConfig.ACTION_NEVER)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Endpoint has a transaction configuration. Defaulting to REQUEST_RESPONSE. Endpoint is: " + toString());
            }
            this.messageExchangePattern = MessageExchangePattern.REQUEST_RESPONSE;
        }
        else
        {
            this.messageExchangePattern = messageExchangePattern;
        }

        this.messageProcessorsFactory = messageProcessorsFactory;
        if (messageProcessors == null)
        {
            this.messageProcessors = Collections.unmodifiableList(java.util.Collections.EMPTY_LIST);
        }
        else
        {
            messageProcessors = injectSelfIntoMessageProcessors(messageProcessors);
            this.messageProcessors = Collections.unmodifiableList(messageProcessors);
        }
        if (responseMessageProcessors == null)
        {
            this.responseMessageProcessors = Collections.unmodifiableList(java.util.Collections.EMPTY_LIST);
        }
        else
        {
            this.responseMessageProcessors = Collections.unmodifiableList(responseMessageProcessors);
        }
    }

    public EndpointURI getEndpointURI()
    {
        return endpointUri;
    }

    public String getEncoding()
    {
        return endpointEncoding;
    }

    public String getMimeType()
    {
        return endpointMimeType;
    }

    public Connector getConnector()
    {
        return connector;
    }

    public String getName()
    {
        return name;
    }

    public EndpointMessageProcessorChainFactory getMessageProcessorsFactory()
    {
        return messageProcessorsFactory;
    }

    public List <MessageProcessor> getMessageProcessors()
    {
        return messageProcessors;
    }

    public List <MessageProcessor> getResponseMessageProcessors()
    {
        return responseMessageProcessors;
    }

    /** @deprecated use getMessageProcessors() */
    public List<Transformer> getTransformers()
    {
        List transformers = new LinkedList();
        for (MessageProcessor processor : messageProcessors)
        {
            if (processor instanceof Transformer)
            {
                transformers.add(processor);
            }
        }
        return transformers;
    }

    public Map getProperties()
    {
        return properties;
    }

    public boolean isReadOnly()
    {
        return true;
    }

    @Override
    public String toString()
    {
        // Use the interface to retrieve the string and set
        // the endpoint uri to a default value
        String sanitizedEndPointUri = null;
        URI uri = null;
        if (endpointUri != null)
        {
            sanitizedEndPointUri = endpointUri.toString();
            uri = endpointUri.getUri();
        }
        // The following will further sanitize the endpointuri by removing
        // the embedded password. This will only remove the password if the
        // uri contains all the necessary information to successfully rebuild the url
        if (uri != null && (uri.getRawUserInfo() != null) && (uri.getScheme() != null) && (uri.getHost() != null)
                && (uri.getRawPath() != null))
        {
            // build a pattern up that matches what we need tp strip out the password
            Pattern sanitizerPattern = Pattern.compile("(.*):.*");
            Matcher sanitizerMatcher = sanitizerPattern.matcher(uri.getRawUserInfo());
            if (sanitizerMatcher.matches())
            {
                sanitizedEndPointUri = new StringBuffer(uri.getScheme()).append("://")
                        .append(sanitizerMatcher.group(1))
                        .append(":<password>")
                        .append("@")
                        .append(uri.getHost())
                        .append(uri.getRawPath())
                        .toString();
            }
            if (uri.getRawQuery() != null)
            {
                sanitizedEndPointUri = sanitizedEndPointUri + "?" + uri.getRawQuery();
            }

        }

        return ClassUtils.getClassName(getClass()) + "{endpointUri=" + sanitizedEndPointUri + ", connector="
                + connector + ",  name='" + name + "', mep=" + messageExchangePattern + ", properties=" + properties
                + ", transactionConfig=" + transactionConfig + ", deleteUnacceptedMessages=" + deleteUnacceptedMessages
                + ", initialState=" + initialState + ", responseTimeout="
                + responseTimeout + ", endpointEncoding=" + endpointEncoding + ", disableTransportTransformer="
                + disableTransportTransformer + "}";
    }

    public String getProtocol()
    {
        return connector.getProtocol();
    }

    public TransactionConfig getTransactionConfig()
    {
        return transactionConfig;
    }

    protected static boolean equal(Object a, Object b)
    {
        return ClassUtils.equal(a, b);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null || getClass() != obj.getClass())
        {
            return false;
        }

        final AbstractEndpoint other = (AbstractEndpoint) obj;
        return equal(retryPolicyTemplate, other.retryPolicyTemplate)
                && equal(connector, other.connector)
                && deleteUnacceptedMessages == other.deleteUnacceptedMessages
                && equal(endpointEncoding, other.endpointEncoding)
                && equal(endpointUri, other.endpointUri)
                && equal(initialState, other.initialState)
                // don't include lifecycle state as lifecycle code includes hashing
                // && equal(initialised, other.initialised)
                && equal(messageExchangePattern, other.messageExchangePattern)
                && equal(name, other.name) 
                && equal(properties, other.properties)
                && responseTimeout == other.responseTimeout
                && equal(messageProcessors, other.messageProcessors)
                && equal(responseMessageProcessors, other.responseMessageProcessors)
                && equal(transactionConfig, other.transactionConfig)
                && disableTransportTransformer == other.disableTransportTransformer;
    }

    @Override
    public int hashCode()
    {
        return ClassUtils.hash(new Object[]{this.getClass(), retryPolicyTemplate, connector,
                deleteUnacceptedMessages ? Boolean.TRUE : Boolean.FALSE,
                endpointEncoding,
                endpointUri,
                initialState,
                // don't include lifecycle state as lifecycle code includes hashing
                // initialised,
                messageExchangePattern,
                name,
                properties, 
                Integer.valueOf(responseTimeout),
                responseMessageProcessors,
                transactionConfig,
                messageProcessors,
                disableTransportTransformer ? Boolean.TRUE : Boolean.FALSE});
    }

    public Filter getFilter()
    {
        // Call the first MessageFilter in the chain "the filter".
        for (MessageProcessor mp : messageProcessors)
        {
            if (mp instanceof MessageFilter)
            {
                return ((MessageFilter) mp).getFilter();
            }
        }
        return null;
    }

    public boolean isDeleteUnacceptedMessages()
    {
        return deleteUnacceptedMessages;
    }

    /**
     * Returns an EndpointSecurityFilter for this endpoint. If one is not set, there
     * will be no authentication on events sent via this endpoint
     *
     * @return EndpointSecurityFilter responsible for authenticating message flow via
     *         this endpoint.
     * @see org.mule.api.security.EndpointSecurityFilter
     */
    public EndpointSecurityFilter getSecurityFilter()
    {
        for (MessageProcessor mp : messageProcessors)
        {
            if (mp instanceof SecurityFilterMessageProcessor)
            {
                return ((SecurityFilterMessageProcessor)mp).getFilter();
            }
        }

        return null;
    }

    public MessageExchangePattern getExchangePattern()
    {
        return messageExchangePattern;
    }

    /**
     * The timeout value for remoteSync invocations
     *
     * @return the timeout in milliseconds
     */
    public int getResponseTimeout()
    {
        return responseTimeout;
    }

    /**
     * Sets the state the endpoint will be loaded in. The States are 'stopped' and
     * 'started' (default)
     *
     * @return the endpoint starting state
     */
    public String getInitialState()
    {
        return initialState;
    }

    /** @deprecated use getResponseMessageProcessors() */
    public List<Transformer> getResponseTransformers()
    {
        List transformers = new LinkedList();
        for (MessageProcessor processor : responseMessageProcessors)
        {
            if (processor instanceof Transformer)
            {
                transformers.add(processor);
            }
        }
        return transformers;
    }

    public Object getProperty(Object key)
    {
        return properties.get(key);
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    public RetryPolicyTemplate getRetryPolicyTemplate()
    {
        return retryPolicyTemplate;
    }

    public String getEndpointBuilderName()
    {
        return endpointBuilderName;
    }

    public boolean isProtocolSupported(String protocol)
    {
        return connector.supportsProtocol(protocol);
    }
    
    public boolean isDisableTransportTransformer() 
    {
        return disableTransportTransformer;
    }
    
    public MessageProcessor getMessageProcessorChain() throws MuleException
    {
        if (messageProcessorChain == null)
        {
            messageProcessorChain = createMessageProcessorChain();
        }
        return messageProcessorChain;
    }

    abstract protected MessageProcessor createMessageProcessorChain() throws MuleException;

    private List injectSelfIntoMessageProcessors(List<MessageProcessor> messageProcessors)
    {
        if (!CollectionUtils.containsType(messageProcessors, EndpointAwareMessageProcessor.class))
        {
            return messageProcessors;
        }
        List<MessageProcessor> newMessageProcessors = new ArrayList<MessageProcessor>(messageProcessors.size());
        for (MessageProcessor mp : messageProcessors)
        {
            if (!(mp instanceof EndpointAwareMessageProcessor))
            {
                newMessageProcessors.add(mp);
            }
            else
            {
                MessageProcessor newMp = ((EndpointAwareMessageProcessor)mp).injectEndpoint(this);
                if (newMp != null)
                {
                    newMessageProcessors.add(newMp);
                }
            }
        }
        return newMessageProcessors;
    }
}
