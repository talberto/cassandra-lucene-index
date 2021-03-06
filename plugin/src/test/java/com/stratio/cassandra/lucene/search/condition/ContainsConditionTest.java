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

package com.stratio.cassandra.lucene.search.condition;

import com.stratio.cassandra.lucene.IndexException;
import com.stratio.cassandra.lucene.schema.Schema;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.junit.Test;

import static com.stratio.cassandra.lucene.schema.SchemaBuilders.*;
import static org.junit.Assert.*;

/**
 * @author Andres de la Pena {@literal <adelapena@stratio.com>}
 */
public class ContainsConditionTest {

    @Test
    public void testBuild() {
        Float boost = 0.7f;
        String field = "test";
        Object[] values = new Object[]{1, 2, 3};
        ContainsCondition condition = new ContainsCondition(boost, field, values);
        assertEquals(boost, condition.boost, 0);
        assertEquals(field, condition.field);
        assertArrayEquals(values, condition.values);
    }

    @Test
    public void testBuildWithDefaults() {
        String field = "test";
        Object[] values = new Object[]{1, 2, 3};
        ContainsCondition condition = new ContainsCondition(null, field, values);
        assertEquals(Condition.DEFAULT_BOOST, condition.boost, 0);
        assertEquals(field, condition.field);
        assertArrayEquals(values, condition.values);
    }

    @Test(expected = IndexException.class)
    public void testBuildWithNullField() {
        new ContainsCondition(0.7f, null, 1, 2, 3);
    }

    @Test(expected = IndexException.class)
    public void testBuildWithBlankField() {
        new ContainsCondition(0.7f, " ", 1, 2, 3);
    }

    @Test(expected = IndexException.class)
    public void testBuildWithNullValues() {
        new ContainsCondition(0.7f, "values");
    }

    @Test(expected = IndexException.class)
    public void testBuildWithEmptyValues() {
        new ContainsCondition(0.7f, "values");
    }

    @Test
    public void testQueryNumeric() {

        Float boost = 0.7f;
        Object[] values = new Object[]{1, 2, 3};

        Schema schema = schema().mapper("name", integerMapper()).build();

        ContainsCondition condition = new ContainsCondition(boost, "name", values);
        Query query = condition.query(schema);
        assertNotNull(query);

        assertEquals(BooleanQuery.class, query.getClass());
        assertEquals(0.7f, query.getBoost(), 0);
        BooleanClause[] booleanClauses = ((BooleanQuery) query).getClauses();
        assertEquals(values.length, booleanClauses.length);
        for (int i = 0; i < values.length; i++) {
            NumericRangeQuery<?> numericRangeQuery = (NumericRangeQuery<?>) booleanClauses[i].getQuery();
            assertEquals(values[i], numericRangeQuery.getMin());
            assertEquals(values[i], numericRangeQuery.getMax());
        }
    }

    @Test
    public void testQueryString() {

        Float boost = 0.7f;
        Object[] values = new Object[]{"houses", "cats"};

        Schema schema = schema().mapper("name", stringMapper()).build();

        ContainsCondition condition = new ContainsCondition(boost, "name", values);
        Query query = condition.query(schema);
        assertNotNull(query);

        assertEquals(BooleanQuery.class, query.getClass());
        assertEquals(0.7f, query.getBoost(), 0);
        BooleanClause[] booleanClauses = ((BooleanQuery) query).getClauses();
        assertEquals("houses", ((TermQuery) booleanClauses[0].getQuery()).getTerm().bytes().utf8ToString());
        assertEquals("cats", ((TermQuery) booleanClauses[1].getQuery()).getTerm().bytes().utf8ToString());
    }

    @Test
    public void testQueryText() {

        Float boost = 0.7f;
        Object[] values = new Object[]{"houses", "cats"};

        Schema schema = schema().mapper("name", textMapper()).defaultAnalyzer("english").build();

        ContainsCondition condition = new ContainsCondition(boost, "name", values);
        Query query = condition.query(schema);
        assertNotNull(query);

        assertEquals(BooleanQuery.class, query.getClass());
        assertEquals(0.7f, query.getBoost(), 0);
        BooleanClause[] booleanClauses = ((BooleanQuery) query).getClauses();
        assertEquals("hous", ((TermQuery) booleanClauses[0].getQuery()).getTerm().bytes().utf8ToString());
        assertEquals("cat", ((TermQuery) booleanClauses[1].getQuery()).getTerm().bytes().utf8ToString());
    }

    @Test
    public void testToString() {
        ContainsCondition condition = new ContainsCondition(0.7f, "field", "value1", "value2");
        assertEquals("ContainsCondition{boost=0.7, field=field, values=[value1, value2]}", condition.toString());
    }

}
