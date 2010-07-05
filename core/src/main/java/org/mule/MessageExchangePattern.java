/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

public enum MessageExchangePattern
{    
    one_way
    {
        @Override
        public boolean hasResponse()
        {
            return false;
        }
    }, 
    
    request_response
    {
        @Override
        public boolean hasResponse()
        {
            return true;
        }
    }; 
    
    public abstract boolean hasResponse();

    public static MessageExchangePattern fromSyncFlag(boolean sync)
    {
        if (sync)
        {
            return request_response;
        }
        else
        {
            return one_way;
        }
    }
}
