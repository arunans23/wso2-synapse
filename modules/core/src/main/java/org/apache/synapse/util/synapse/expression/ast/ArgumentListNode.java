/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.util.synapse.expression.ast;

import org.apache.synapse.util.synapse.expression.context.EvaluationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * This class handles the list of arguments passed to a function.
 */
public class ArgumentListNode implements ExpressionNode {
    private final List<ExpressionNode> arguments = new ArrayList<>();

    public ArgumentListNode() {
    }

    public void addArgument(ExpressionNode argument) {
        arguments.add(argument);
    }

    public List<ExpressionNode> getArguments() {
        return arguments;
    }

    @Override
    public ExpressionResult evaluate(EvaluationContext context, boolean isObjectValue) {
        return null;
    }

}
