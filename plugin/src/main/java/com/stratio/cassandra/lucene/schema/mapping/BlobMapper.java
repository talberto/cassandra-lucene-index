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

import com.stratio.cassandra.lucene.IndexException;
import com.stratio.cassandra.lucene.util.ByteBufferUtils;
import org.apache.cassandra.db.marshal.AsciiType;
import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.db.marshal.UTF8Type;
import org.apache.cassandra.utils.Hex;

import java.nio.ByteBuffer;

/**
 * A {@link Mapper} to map blob values.
 *
 * @author Andres de la Pena {@literal <adelapena@stratio.com>}
 */
public class BlobMapper extends KeywordMapper {

    /**
     * Builds a new {@link BlobMapper}.
     *
     * @param name    The name of the mapper.
     * @param column  The name of the column to be mapped.
     * @param indexed If the field supports searching.
     * @param sorted  If the field supports sorting.
     */
    public BlobMapper(String name, String column, Boolean indexed, Boolean sorted) {
        super(name, column, indexed, sorted, AsciiType.instance, UTF8Type.instance, BytesType.instance);
    }

    /** {@inheritDoc} */
    @Override
    public String base(String name, Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof ByteBuffer) {
            return base((ByteBuffer) value);
        } else if (value instanceof byte[]) {
            return base((byte[]) value);
        } else if (value instanceof String) {
            return base((String) value);
        }
        throw new IndexException("Field '%s' requires a byte array, but found '%s'", name, value);
    }

    private String base(ByteBuffer value) {
        return ByteBufferUtils.toHex(value);
    }

    private String base(byte[] value) {
        return ByteBufferUtils.toHex(value);
    }

    private String base(String value) throws NumberFormatException {
        try {
            value = value.replaceFirst("0x", "");
            byte[] bytes = Hex.hexToBytes(value);
            return Hex.bytesToHex(bytes);
        } catch (NumberFormatException e) {
            throw new IndexException("Field '%s' requires an hex string, but found '%s'", name, value);
        }
    }
}
