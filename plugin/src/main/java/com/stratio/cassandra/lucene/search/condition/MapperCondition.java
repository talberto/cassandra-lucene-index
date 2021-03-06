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

package com.stratio.cassandra.lucene.search.condition;

import com.google.common.base.Objects;
import com.stratio.cassandra.lucene.IndexException;
import com.stratio.cassandra.lucene.schema.Schema;
import com.stratio.cassandra.lucene.schema.mapping.Mapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Query;

/**
 * An abstract {@link Condition} directed to a specific mapper.
 *
 * @author Andres de la Pena {@literal <adelapena@stratio.com>}
 */
public abstract class MapperCondition<T extends Mapper> extends Condition {

    /** The name of the field to be matched. */
    public final String field;

    /** The type of the {@link Mapper}. */
    private final Class<T> type;

    /**
     * Constructor using the boost and the name of the mapper.
     *
     * @param boost The boost for this query clause. Documents matching this clause will (in addition to the normal
     *              weightings) have their score multiplied by {@code boost}. If {@code null}, then {@link
     *              #DEFAULT_BOOST} is used as default.
     * @param field The name of the field to be matched.
     * @param type  The type of the {@link Mapper}.
     */
    protected MapperCondition(Float boost, String field, Class<T> type) {
        super(boost);

        if (StringUtils.isBlank(field)) {
            throw new IndexException("Field name required");
        }

        this.field = field;
        this.type = type;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public final Query query(Schema schema) {
        Mapper mapper = schema.getMapper(field);
        if (mapper == null) {
            throw new IndexException("No mapper found for field '%s'", field);
        } else if (!type.isAssignableFrom(mapper.getClass())) {
            throw new IndexException("Field '%s' requires a mapper of type '%s' but found '%s'", field, type, mapper);
        }
        return query((T) mapper, schema.getAnalyzer());
    }

    /**
     * Returns the Lucene {@link Query} representation of this condition.
     *
     * @param mapper   The {@link Mapper} to be used.
     * @param analyzer The {@link Schema} {@link Analyzer}.
     * @return The Lucene {@link Query} representation of this condition.
     */
    public abstract Query query(T mapper, Analyzer analyzer);

    protected Objects.ToStringHelper toStringHelper(Object o) {
        return super.toStringHelper(o).add("field", field);
    }
}