/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.synapse.transport.util;

import org.apache.axis2.context.MessageContext;

import java.io.IOException;
import java.io.InputStream;
import javax.xml.stream.XMLStreamException;

/**
 * Message handler for different transports.
 */
public interface MessageHandler {

    public InputStream getMessageDataStream(MessageContext context) throws IOException;

    public void buildMessage(MessageContext messageContext) throws XMLStreamException, IOException;

    public void buildMessage(MessageContext messageContext, boolean earlyBuild) throws XMLStreamException, IOException;

}
