/*
 * Copyright 2015, Stratio.
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

package com.stratio.cassandra.lucene.schema.mapping.builder;

import com.stratio.cassandra.lucene.schema.mapping.BooleanMapper;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * {@link SingleColumnMapperBuilder} to build a new {@link BooleanMapper}.
 *
 * @author Andres de la Pena {@literal <adelapena@stratio.com>}
 */
public class BooleanMapperBuilder extends SingleColumnMapperBuilder<BooleanMapper> {

    @JsonProperty("indexed")
    private Boolean indexed;

    @JsonProperty("sorted")
    private Boolean sorted;

    /**
     * Sets if the field supports searching.
     *
     * @param indexed if the field supports searching.
     * @return This.
     */
    public BooleanMapperBuilder indexed(Boolean indexed) {
        this.indexed = indexed;
        return this;
    }

    /**
     * Sets if the field supports sorting.
     *
     * @param sorted if the field supports sorting.
     * @return This.
     */
    public BooleanMapperBuilder sorted(Boolean sorted) {
        this.sorted = sorted;
        return this;
    }

    /**
     * Returns the {@link BooleanMapper} represented by this {@link MapperBuilder}.
     *
     * @param name The name of the {@link BooleanMapper} to be built.
     * @return The {@link BooleanMapper} represented by this.
     */
    @Override
    public BooleanMapper build(String name) {
        return new BooleanMapper(name, column, indexed, sorted);
    }
}
