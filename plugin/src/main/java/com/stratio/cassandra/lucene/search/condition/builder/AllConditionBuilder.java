/*
 * Copyright 2014, Stratio.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stratio.cassandra.lucene.search.condition.builder;

import com.stratio.cassandra.lucene.search.condition.AllCondition;
import org.codehaus.jackson.annotate.JsonCreator;

/**
 * {@link ConditionBuilder} for building a new {@link AllCondition}.
 *
 * @author Andres de la Pena {@literal <adelapena@stratio.com>}
 */
public class AllConditionBuilder extends ConditionBuilder<AllCondition, AllConditionBuilder> {

    /**
     * Creates a new {@link AllConditionBuilder}.
     */
    @JsonCreator
    public AllConditionBuilder() {
    }

    /**
     * Returns the {@link AllCondition} represented by this builder.
     *
     * @return The {@link AllCondition} represented by this builder.
     */
    @Override
    public AllCondition build() {
        return new AllCondition(boost);
    }
}
