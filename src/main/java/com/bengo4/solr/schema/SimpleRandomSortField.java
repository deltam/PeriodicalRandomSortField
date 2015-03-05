package com.bengo4.solr.schema;


import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;

import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.QParser;
import org.apache.solr.response.TextResponseWriter;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.docvalues.IntDocValues;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.search.SortField;

import org.apache.solr.common.SolrException;

/**
 * SimpleRandomSortField
 *
 * Unlike RandomSortField, this is does not depend on docBase and version.
 *
 * Usage: srandom_{SEED} asc|desc
 *   {SEED} : random seed
 *
 */
public class SimpleRandomSortField extends FieldType
{

    /**
     * This method is th same as RandomSortField
     */
    private static int hash(int key) {
        key = ~key + (key << 15); // key = (key << 15) - key - 1;
        key = key ^ (key >>> 12);
        key = key + (key << 2);
        key = key ^ (key >>> 4);
        key = key * 2057; // key = (key + (key << 3)) + (key << 11);
        key = key ^ (key >>> 16);
        return key >>> 1;
    }

    /**
     * field
     */
    private static int getSeed(String fieldName, AtomicReaderContext context) {
        return fieldName.hashCode();
    }



    ///////////////////////////////////////////////////////////////////////////////////////////
    /* under code is the same as RandomSortField */

    @Override
    public SortField getSortField(SchemaField field, boolean reverse) {
        return new SortField(field.getName(), randomComparatorSource, reverse);
    }

    @Override
    public ValueSource getValueSource(SchemaField field, QParser qparser) {
        return new RandomValueSource(field.getName());
    }

    @Override
    public void write(TextResponseWriter writer, String name, IndexableField f) throws IOException { }



    private static FieldComparatorSource randomComparatorSource = new FieldComparatorSource() {
            @Override
            public FieldComparator<Integer> newComparator(final String fieldname, final int numHits, int sortPos, boolean reversed) {
                return new FieldComparator<Integer>() {
                    int seed;
                    private final int[] values = new int[numHits];
                    int bottomVal;

                    @Override
                    public int compare(int slot1, int slot2) {
                        return values[slot1] - values[slot2];  // values will be positive... no overflow possible.
                    }

                    @Override
                    public void setBottom(int slot) {
                        bottomVal = values[slot];
                    }

                    @Override
                    public int compareBottom(int doc) {
                        return bottomVal - hash(doc+seed);
                    }

                    @Override
                    public void copy(int slot, int doc) {
                        values[slot] = hash(doc+seed);
                    }

                    @Override
                    public FieldComparator<Integer> setNextReader(AtomicReaderContext context) { // unchecked警告が出るため返り値に型指定を追加
                        seed = getSeed(fieldname, context);
                        return this;
                    }

                    @Override
                    public Integer value(int slot) {
                        return values[slot];
                    }

                    @Override
                    public int compareDocToValue(int doc, Integer valueObj) {
                        // values will be positive... no overflow possible.
                        return hash(doc+seed) - valueObj.intValue();
                    }
                };
            }
        };


    public class RandomValueSource extends ValueSource {
        private final String field;

        public RandomValueSource(String field) {
            this.field = field;
        }

        @Override
        public String description() {
            return field;
        }

        @Override
        public FunctionValues getValues(Map context, final AtomicReaderContext readerContext) throws IOException {
            return new IntDocValues(this) {
                private final int seed = getSeed(field, readerContext);
                @Override
                public int intVal(int doc) {
                    return hash(doc+seed);
                }
            };
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof RandomValueSource)) return false;
            RandomValueSource other = (RandomValueSource)o;
            return this.field.equals(other.field);
        }

        @Override
        public int hashCode() {
            return field.hashCode();
        };
    }
}
