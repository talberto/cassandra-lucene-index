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

package com.stratio.cassandra.lucene.schema.mapping;

import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.util.BytesRef;

/**
 * A {@link Mapper} to map a string, not tokenized field.
 *
 * @author Andres de la Pena {@literal <adelapena@stratio.com>}
 */
public abstract class KeywordMapper extends SingleColumnMapper<String> {

    static final FieldType FIELD_TYPE = new FieldType();

    static {
        FIELD_TYPE.setOmitNorms(true);
        FIELD_TYPE.setIndexOptions(IndexOptions.DOCS);
        FIELD_TYPE.setTokenized(true);
        FIELD_TYPE.freeze();
    }

    /**
     * Builds  a new {@link KeywordMapper}.
     *
     * @param name           The name of the mapper.
     * @param column         The name of the column to be mapped.
     * @param indexed        If the field supports searching.
     * @param sorted         If the field supports sorting.
     * @param supportedTypes The supported Cassandra types for indexing.
     */
    KeywordMapper(String name, String column, Boolean indexed, Boolean sorted, AbstractType<?>... supportedTypes) {
        super(name, column, indexed, sorted, supportedTypes);
    }

    /** {@inheritDoc} */
    @Override
    public String getAnalyzer() {
        return KEYWORD_ANALYZER;
    }

    /** {@inheritDoc} */
    @Override
    public Field indexedField(String name, String value) {
        return new Field(name, value, FIELD_TYPE);
    }

    /** {@inheritDoc} */
    @Override
    public Field sortedField(String name, String value) {
        return new SortedDocValuesField(name, new BytesRef(value));
    }

    /** {@inheritDoc} */
    @Override
    public final SortField sortField(String name, boolean reverse) {
        return new SortField(name, Type.STRING_VAL, reverse);
    }

    /** {@inheritDoc} */
    @Override
    public final Class<String> baseClass() {
        return String.class;
    }
}
