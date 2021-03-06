++++++++++++++++++++++++++++++++
Stratio's Cassandra Lucene Index
++++++++++++++++++++++++++++++++

- `Overview <#overview>`__
    - `Features <#features>`__
    - `Requirements <#requirements>`__
    - `Installation <#installation>`__
    - `Example <#example>`__
- `Indexing <#indexing>`__
    - `Analysis <#analysis>`__
    - `Mapping <#mapping>`__
    - `Example <#example>`__
- `Searching <#searching>`__
    - `All search <#all-search>`__
    - `Bitemporal search <#bitemporal-search>`__
    - `Boolean search <#boolean-search>`__
    - `Contains search <#contains-search>`__
    - `Date range search <#date-range-search>`__
    - `Fuzzy search <#fuzzy-search>`__
    - `Geo bounding box search <#geo-bbox-search>`__
    - `Geo distance search <#geo-distance-search>`__
    - `Match search <#match-search>`__
    - `None search <#none-search>`__
    - `Phrase search <#phrase-search>`__
    - `Prefix search <#prefix-search>`__
    - `Range search <#range-search>`__
    - `Regexp search <#regexp-search>`__
    - `Wildcard search <#wildcard-search>`__
- `Spark and Hadoop <#spark-and-hadoop>`__
    - `Token range searches <#token-range-searches>`__
    - `Paging <#paging>`__
- `JMX interface <#jmx-interface>`__

Overview
********

`Cassandra <http://cassandra.apache.org/>`__ index functionality has
been extended to provide near real time search such as
`ElasticSearch <http://www.elasticsearch.org/>`__ or
`Solr <https://lucene.apache.org/solr/>`__, including full text search
capabilities and multivariable, geospatial and bitemporal search.

It is also fully compatible with `Apache
Spark <https://spark.apache.org/>`__ and `Apache
Hadoop <https://hadoop.apache.org/>`__, allowing you to filter data at
database level. This speeds up jobs reducing the amount of data to be
collected and processed.

Indexing is achieved through a Lucene based implementation of Cassandra
secondary indexes, where each node of the cluster indexes its own data.
Stratio Cassandra is one of the core modules on which Stratio's BigData
platform (SDS) is based.

Features
========

Lucene search technology integration into Cassandra provides:

Stratio’s Cassandra Lucene Index and its integration with Lucene search technology provides:

-  Full text search
-  Geospatial search
-  Bitemporal search
-  Boolean (and, or, not) search
-  Near real-time search
-  Relevance scoring and sorting
-  General top-k queries
-  Custom analyzers
-  CQL3 support
-  Third-party drivers compatibility
-  Spark compatibility
-  Hadoop compatibility

Not yet supported:

-  Thrift API
-  Legacy compact storage option
-  Indexing ``counter`` columns
-  Columns with TTL
-  CQL user defined types
-  Static columns

Requirements
============

-  Cassandra 2.1.8
-  Java >= 1.7 (OpenJDK and Sun have been tested)
-  Maven >= 3.0

Installation
============

SStratio’s Cassandra Lucene Index is distributed as a plugin for Apache Cassandra. Thus, you just need to build a JAR
containing the plugin and add it to the Cassandra’s classpath:

-  Build the plugin with Maven: ``mvn clean package``
-  Copy the generated JAR to the lib folder of your compatible Cassandra installation:
   ``cp plugin/target/cassandra-lucene-index-plugin-2.1.8.2.jar <CASSANDRA_HOME>/lib/``
-  Start/restart Cassandra as usual

Alternatively, patching can also be done with this Maven profile, specifying the path of your Cassandra installation,
 this task also delete previous plugin's JAR versions in CASSANDRA_HOME/lib/ directory:

.. code-block:: bash

    mvn clean package -Ppatch -Dcassandra_home=<CASSANDRA_HOME>

If you don’t have an installed version of Cassandra, there is also an alternative profile to let Maven download and
patch the proper version of Apache Cassandra:

.. code-block:: bash

    mvn clean package -Pdownload_and_patch -Dcassandra_home=<CASSANDRA_HOME>

Now you can run Cassandra and do some tests using the Cassandra Query Language:

.. code-block:: bash

    <CASSANDRA_HOME>/bin/cassandra -f
    <CASSANDRA_HOME>/bin/cqlsh

The Lucene’s index files will be stored in the same directories where the Cassandra’s will be. The default data
directory is ``/var/lib/cassandra/data``, and each index is placed next to the SSTables of its indexed column family.

For more details about Apache Cassandra please see its `documentation <http://cassandra.apache.org/>`__.

Example
=======

We will create the following table to store tweets:

.. code-block:: sql

    CREATE KEYSPACE demo
    WITH REPLICATION = {'class' : 'SimpleStrategy', 'replication_factor': 1};
    USE demo;
    CREATE TABLE tweets (
        id INT PRIMARY KEY,
        user TEXT,
        body TEXT,
        time TIMESTAMP,
        latitude FLOAT,
        longitude FLOAT,
        lucene TEXT
    );

We have created a column called *lucene* to link the index searches. This column will not store data. Now you can create
a custom Lucene index on it with the following statement:

.. code-block:: sql

    CREATE CUSTOM INDEX tweets_index ON tweets (lucene)
    USING 'com.stratio.cassandra.lucene.Index'
    WITH OPTIONS = {
        'refresh_seconds' : '1',
        'schema' : '{
            fields : {
                id    : {type : "integer"},
                user  : {type : "string"},
                body  : {type : "text", analyzer : "english"},
                time  : {type : "date", pattern : "yyyy/MM/dd", sorted : true},
                place : {type : "geo_point", latitude:"latitude", longitude:"longitude"}
            }
        }'
    };

This will index all the columns in the table with the specified types, and it will be refreshed once per second.
Alternatively, you can explicitly refresh all the index shards with an empty search with consistency ``ALL``:

.. code-block:: sql

    CONSISTENCY ALL
    SELECT * FROM tweets WHERE lucene = '{refresh:true}';
    CONSISTENCY QUORUM

Now, to search for tweets within a certain date range:

.. code-block:: sql

    SELECT * FROM tweets WHERE lucene='{
        filter : {type:"range", field:"time", lower:"2014/04/25", upper:"2014/05/1"}
    }' limit 100;

The same search can be performed forcing an explicit refresh of the involved index shards:

.. code-block:: sql

    SELECT * FROM tweets WHERE lucene='{
        filter : {type:"range", field:"time", lower:"2014/04/25", upper:"2014/05/1"},
        refresh : true
    }' limit 100;

Now, to search the top 100 more relevant tweets where *body* field contains the phrase “big data gives organizations”
within the aforementioned date range:

.. code-block:: sql

    SELECT * FROM tweets WHERE lucene='{
        filter : {type:"range", field:"time", lower:"2014/04/25", upper:"2014/05/1"},
        query  : {type:"phrase", field:"body", value:"big data gives organizations", slop:1}
    }' limit 100;

To refine the search to get only the tweets written by users whose name starts with "a":

.. code-block:: sql

    SELECT * FROM tweets WHERE lucene='{
        filter : {type:"boolean", must:[
                       {type:"range", field:"time", lower:"2014/04/25", upper:"2014/05/1"},
                       {type:"prefix", field:"user", value:"a"} ] },
        query  : {type:"phrase", field:"body", value:"big data gives organizations", slop:1}
    }' limit 100;

To get the 100 more recent filtered results you can use the *sort* option:

.. code-block:: sql

    SELECT * FROM tweets WHERE lucene='{
        filter : {type:"boolean", must:[
                       {type:"range", field:"time", lower:"2014/04/25", upper:"2014/05/1"},
                       {type:"prefix", field:"user", value:"a"} ] },
        query  : {type:"phrase", field:"body", value:"big data gives organizations", slop:1},
        sort   : {fields: [ {field:"time", reverse:true} ] }
    }' limit 100;

The previous search can be restricted to a geographical bounding box:

.. code-block:: sql

    SELECT * FROM tweets WHERE lucene='{
        filter : {type:"boolean", must:[
                       {type:"range", field:"time", lower:"2014/04/25", upper:"2014/05/1"},
                       {type:"prefix", field:"user", value:"a"},
                       {type:"geo_bbox",
                        field:"place",
                        min_latitude:40.225479,
                        max_latitude:40.560174,
                        min_longitude:-3.999278,
                        max_longitude:-3.378550} ] },
        query  : {type:"phrase", field:"body", value:"big data gives organizations", slop:1},
        sort   : {fields: [ {field:"time", reverse:true} ] }
    }' limit 100;

Alternatively, you can restrict the search to retrieve tweets that are within a specific distance from a geographical position:

.. code-block:: sql

    SELECT * FROM tweets WHERE lucene='{
        filter : {type:"boolean", must:[
                       {type:"range", field:"time", lower:"2014/04/25", upper:"2014/05/1"},
                       {type:"prefix", field:"user", value:"a"},
                       {type:"geo_distance",
                        field:"place",
                        latitude:40.393035,
                        longitude:-3.732859,
                        max_distance:"10km",
                        min_distance:"100m"} ] },
        query  : {type:"phrase", field:"body", value:"big data gives organizations", slop:1},
        sort   : {fields: [ {field:"time", reverse:true} ] }
    }' limit 100;

Indexing
********

Syntax:

.. code-block:: sql

    CREATE CUSTOM INDEX (IF NOT EXISTS)? <index_name>
                                      ON <table_name> ( <magic_column> )
                                   USING 'com.stratio.cassandra.lucene.Index'
                            WITH OPTIONS = <options>

where:

-  <magic\_column> is the name of a text column that does not contain
   any data and will be used to show the scoring for each resulting row
   of a search.
-  <options> is a JSON object:

.. code-block:: sql

    <options> := { ('refresh_seconds'      : '<int_value>',)?
                   ('ram_buffer_mb'        : '<int_value>',)?
                   ('max_merge_mb'         : '<int_value>',)?
                   ('max_cached_mb'        : '<int_value>',)?
                   ('directory_path'       : '<string_value>',)?
                   'schema'                : '<schema_definition>'};

Options, except “schema” and “directory\_path”, take a positive integer
value enclosed in single quotes:

-  **refresh\_seconds**: number of seconds before auto-refreshing the
   index reader. It is the max time taken for writes to be searchable
   without forcing an index refresh. Defaults to '60'.
-  **ram\_buffer\_mb**: size of the write buffer. Its content will be
   committed to disk when full. Defaults to '64'.
-  **max\_merge\_mb**: defaults to '5'.
-  **max\_cached\_mb**: defaults to '30'.
-  **directory\_path**: The path of the directory where the  Lucene index
   will be stored.
-  **schema**: see below

.. code-block:: sql

    <schema_definition> := {
        (analyzers : { <analyzer_definition> (, <analyzer_definition>)* } ,)?
        (default_analyzer : "<analyzer_name>",)?
        fields : { <field_definition> (, <field_definition>)* }
    }

Where default\_analyzer defaults to
‘org.apache.lucene.analysis.standard.StandardAnalyzer’.

.. code-block:: sql

    <analyzer_definition> := <analyzer_name> : {
        type : "<analyzer_type>" (, <option> : "<value>")*
    }

.. code-block:: sql

    <field_definition> := <column_name> : {
        type : "<field_type>" (, <option> : "<value>")*
    }

Analysis
========

Analyzer definition options depend on the analyzer type. Details and
default values are listed in the table below.

+-----------------+-------------+--------------+-----------------+
| Analyzer type   | Option      | Value type   | Default value   |
+=================+=============+==============+=================+
| classpath       | class       | string       | null            |
+-----------------+-------------+--------------+-----------------+
| snowball        | language    | string       | null            |
|                 +-------------+--------------+-----------------+
|                 | stopwords   | string       | null            |
+-----------------+-------------+--------------+-----------------+

Mapping
=======

Field mapping definition options specify how the CQL rows will be mapped to Lucene documents.
Several mappers can be applied to the same CQL column/s.
Details and default values are listed in the table below.

+-----------------+-----------------+-----------------+--------------------------------+-----------+
| Mapper type     | Option          | Value type      | Default value                  | Mandatory |
+=================+=================+=================+================================+===========+
| bigdec          | column          | string          | mapper_name of the schema      | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | indexed         | boolean         | true                           | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | sorted          | boolean         | false                          | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | integer_digits  | integer         | 32                             | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | decimal_digits  | integer         | 32                             | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+
| bigint          | column          | string          | mapper_name of the schema      | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | indexed         | boolean         | true                           | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | sorted          | boolean         | false                          | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | digits          | integer         | 32                             | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+
| bitemporal      | vt_from         | string          |                                | Yes       |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | vt_to           | string          |                                | Yes       |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | tt_from         | string          |                                | Yes       |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | tt_to           | string          |                                | Yes       |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | pattern         | string          | yyyy/MM/dd HH:mm:ss.SSS Z      | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | now_value       | object          | Long.MAX_VALUE                 | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+
| blob            | column          | string          | mapper_name of the schema      | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | indexed         | boolean         | true                           | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | sorted          | boolean         | false                          | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+
| boolean         | column          | string          | mapper_name of the schema      | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | indexed         | boolean         | true                           | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | sorted          | boolean         | false                          | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+
| date            | column          | string          | mapper_name of the schema      | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | indexed         | boolean         | true                           | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | sorted          | boolean         | false                          | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | pattern         | string          | yyyy/MM/dd HH:mm:ss.SSS Z      | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+
| date_range      | from            | string          |                                | Yes       |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | to              | string          |                                | Yes       |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | pattern         | string          | yyyy/MM/dd HH:mm:ss.SSS Z      | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+
| double          | column          | string          | mapper_name of the schema      | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | indexed         | boolean         | true                           | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | sorted          | boolean         | false                          | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | boost           | integer         | 0.1f                           | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+
| float           | column          | string          | mapper_name of the schema      | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | indexed         | boolean         | true                           | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | sorted          | boolean         | false                          | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | boost           | integer         | 0.1f                           | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+
| geo_point       | latitude        | string          |                                | Yes       |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | longitude       | string          |                                | Yes       |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | max_levels      | integer         | 11                             | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+
| inet            | column          | string          | mapper_name of the schema      | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | indexed         | boolean         | true                           | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | sorted          | boolean         | false                          | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+
| integer         | column          | string          | mapper_name of the schema      | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | indexed         | boolean         | true                           | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | sorted          | boolean         | false                          | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | boost           | integer         | 0.1f                           | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+
| long            | column          | string          | mapper_name of the schema      | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | indexed         | boolean         | true                           | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | sorted          | boolean         | false                          | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | boost           | integer         | 0.1f                           | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+
| string          | column          | string          | mapper_name of the schema      | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | indexed         | boolean         | true                           | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | sorted          | boolean         | false                          | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+
| text            | column          | string          | mapper_name of the schema      | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | indexed         | boolean         | true                           | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | sorted          | boolean         | false                          | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | analyzer        | string          | default_analyzer of the schema | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+
| uuid            | column          | string          | mapper_name of the schema      | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | indexed         | boolean         | true                           | No        |
|                 +-----------------+-----------------+--------------------------------+-----------+
|                 | sorted          | boolean         | false                          | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+

Most mapping definitions have an ``indexed`` option indicating if
the field is searchable, it is true by default. There is also a ``sorted`` option
specifying if it is possible to sort rows by the corresponding field, false by default. List and set
columns can't be sorted because they produce multivalued fields.
These options should be set to false when no needed in order to have a smaller and faster index.

Note that Cassandra allows one custom index per table. On the other
hand, Cassandra does not allow a modify operation on indexes. To modify
an index it needs to be deleted first and created again.

Example
=======

This code below and the one for creating the corresponding keyspace and
table is available in a CQL script that can be sourced from the
Cassandra shell:
`test-users-create.cql </doc/resources/test-users-create.cql>`__.

.. code-block:: sql

    CREATE CUSTOM INDEX IF NOT EXISTS users_index
    ON test.users (stratio_col)
    USING 'com.stratio.cassandra.lucene.Index'
    WITH OPTIONS = {
        'refresh_seconds'      : '60',
        'ram_buffer_mb'        : '64',
        'max_merge_mb'         : '5',
        'max_cached_mb'        : '30',
        'schema' : '{
            analyzers : {
                  my_custom_analyzer : {
                      type:"snowball",
                      language:"Spanish",
                      stopwords : "el,la,lo,loas,las,a,ante,bajo,cabe,con,contra"}
            },
            default_analyzer : "english",
            fields : {
                name   : {type     : "string"},
                gender : {type     : "string", sorted: true},
                animal : {type     : "string"},
                age    : {type     : "integer"},
                food   : {type     : "string"},
                number : {type     : "integer"},
                bool   : {type     : "boolean"},
                date   : {type     : "date", pattern  : "yyyy/MM/dd"},
                mapz   : {type     : "string", sorted: true},
                setz   : {type     : "string"},
                listz  : {type     : "string"},
                phrase : {type     : "text", analyzer : "my_custom_analyzer"}
            }
        }'
    };

Searching
*********

Syntax:

.. code-block:: sql

    SELECT ( <fields> | * )
    FROM <table_name>
    WHERE <magic_column> = '{ (   filter  : <filter>  )?
                              ( , query   : <query>   )?
                              ( , sort    : <sort>    )?
                              ( , refresh : ( true | false ) )?
                            }';

where <filter> and <query> are a JSON object:

.. code-block:: sql

    <filter> := { type : <type> (, <option> : ( <value> | <value_list> ) )+ }
    <query>  := { type : <type> (, <option> : ( <value> | <value_list> ) )+ }

and <sort> is another JSON object:

.. code-block:: sql

        <sort> := { fields : <sort_field> (, <sort_field> )* }
        <sort_field> := { field : <field> (, reverse : <reverse> )? }

When searching by ``filter``, without any ``query`` or ``sort`` defined,
then the results are returned in the Cassandra’s natural order, which is
defined by the partitioner and the column name comparator. When searching
by ``query``, results are returned sorted by descending relevance. The
scores will be located in the column ``magic_column``. Sort option is used
to specify the order in which the indexed rows will be traversed. When
sorting is used, the query scoring is delayed.

Relevance queries must touch all the nodes in the ring in order to find
the globally best results, so you should prefer filters over queries
when no relevance nor sorting are needed.

The ``refresh`` boolean option indicates if the search must commit pending
writes and refresh the Lucene IndexSearcher before being performed. This
way a search with ``refresh`` set to true will view the most recent changes
done to the index, independently of the index auto-refresh time.
Please note that it is a costly operation, so you should not use it
unless it is strictly necessary. The default value is false. You can
explicitly refresh all the index shards with an empty search with consistency
``ALL``, and the return to your desired consistency level:

.. code-block:: sql

    CONSISTENCY ALL
    SELECT * FROM <table> WHERE <magic_column> = '{refresh:true}';
    CONSISTENCY QUORUM

This way the subsequent searches will view all the writes done before this
operation, without needing to wait for the index auto refresh. It is useful to
perform this operation before searching after a bulk data load.

Types of search and their options are summarized in the table below.
Details for each of them are available in individual sections and the
examples can be downloaded as a CQL script:
`extended-search-examples.cql </doc/resources/extended-search-examples.cql>`__.

In addition to the options described in the table, all search types have
a “\ **boost**\ ” option that acts as a weight on the resulting score.

+-----------------------------------------+-----------------+-----------------+--------------------------------+-----------+
| Search type                             | Option          | Value type      | Default value                  | Mandatory |
+=========================================+=================+=================+================================+===========+
| `All <#all-search>`__                   |                 |                 |                                |           |
+-----------------------------------------+-----------------+-----------------+--------------------------------+-----------+
| `Bitemporal <#bitemporal-search>`__     | field           | string          |                                | Yes       |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | vt_from         | string/long     | 0L                             | No        |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | vt_to           | string/long     | Long.MAX_VALUE                 | No        |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | tt_from         | string/long     | 0L                             | No        |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | tt_to           | string/long     | Long.MAX_VALUE                 | No        |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | operation       | string          | intersects                     | No        |
+-----------------------------------------+-----------------+-----------------+--------------------------------+-----------+
| `Boolean <#boolean-search>`__           | must            | search          |                                | No        |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | should          | search          |                                | No        |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | not             | search          |                                | No        |
+-----------------------------------------+-----------------+-----------------+--------------------------------+-----------+
| `Contains <#contains-search>`__         | field           | string          |                                | Yes       |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | values          | array           |                                | Yes       |
+-----------------------------------------+-----------------+-----------------+--------------------------------+-----------+
| `Date range <#date-range-search>`__     | field           | string          |                                | Yes       |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | from            | string/long     | 0                              | No        |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | to              | string/long     | Long.MAX_VALUE                 | No        |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | operation       | string          | is_within                      | No        |
+-----------------------------------------+-----------------+-----------------+--------------------------------+-----------+
| `Fuzzy <#fuzzy-search>`__               | field           | string          |                                | Yes       |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | value           | string          |                                | Yes       |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | max_edits       | integer         | 2                              | No        |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | prefix_length   | integer         | 0                              | No        |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | max_expansions  | integer         | 50                             | No        |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | transpositions  | boolean         | true                           | No        |
+-----------------------------------------+-----------------+-----------------+--------------------------------+-----------+
| `Geo bounding box <#geo-bbox-search>`__ | field           | string          |                                | Yes       |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | min_latitude    | double          |                                | Yes       |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | max_latitude    | double          |                                | Yes       |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | min_longitude   | double          |                                | Yes       |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | max_longitude   | double          |                                | Yes       |
+-----------------------------------------+-----------------+-----------------+--------------------------------+-----------+
| `Geo distance <#geo-distance-search>`__ | field           | string          |                                | Yes       |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | latitude        | double          |                                | Yes       |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | longitude       | double          |                                | Yes       |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | max_distance    | string          |                                | Yes       |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | min_distance    | string          |                                | No        |
+-----------------------------------------+-----------------+-----------------+--------------------------------+-----------+
| `Match <#match-search>`__               | field           | string          |                                | Yes       |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | value           | any             |                                | Yes       |
+-----------------------------------------+-----------------+-----------------+--------------------------------+-----------+
| `None <#none-search>`__                 |                 |                 |                                |           |
+-----------------------------------------+-----------------+-----------------+--------------------------------+-----------+
| `Phrase <#phrase-search>`__             | field           | string          |                                | Yes       |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | value           | string          |                                | Yes       |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | slop            | integer         | 0                              | No        |
+-----------------------------------------+-----------------+-----------------+--------------------------------+-----------+
| `Prefix <#prefix-search>`__             | field           | string          |                                | Yes       |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | value           | string          |                                | Yes       |
+-----------------------------------------+-----------------+-----------------+--------------------------------+-----------+
| `Range <#range-search>`__               | field           | string          |                                | Yes       |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | lower           | any             |                                | No        |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | upper           | any             |                                | No        |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | include_lower   | boolean         | false                          | No        |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | include_upper   | boolean         | false                          | No        |
+-----------------------------------------+-----------------+-----------------+--------------------------------+-----------+
| `Regexp <#regexp-search>`__             | field           | string          |                                | Yes       |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | value           | string          |                                | Yes       |
+-----------------------------------------+-----------------+-----------------+--------------------------------+-----------+
| `Wildcard <#wildcard-search>`__         | field           | string          |                                | Yes       |
|                                         +-----------------+-----------------+--------------------------------+-----------+
|                                         | value           | string          |                                | Yes       |
+-----------------------------------------+-----------------+-----------------+--------------------------------+-----------+

All search
==========

Syntax:

.. code-block:: sql

    SELECT ( <fields> | * )
    FROM <table>
    WHERE <magic_column> = '{ (filter | query) : { type  : "all"} }';

Example: will return all the indexed rows

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{filter : { type  : "all" } }';

Bitemporal search
=================

Syntax:

.. code-block:: sql

    SELECT ( <fields> | * )
    FROM <table>
    WHERE <magic_column> = '{ (filter | query) : {
                                type  : "bitemporal",
                                (vt_from : <vt_from> ,)?
                                (vt_to   : <vt_to> ,)?
                                (tt_from : <tt_from> ,)?
                                (tt_to   : <tt_to> ,)?
                                (operation: <operation> )?
                              }}';

where:

-  **vt\_from** (default = 0L): a string or a number being the beginning of the valid date range.
-  **vt\_to** (default = Long.MAX_VALUE): a string or a number being the end of the valid date range.
-  **tt\_from** (default = 0L): a string or a number being the beginning of the transaction date range.
-  **tt\_to** (default = Long.MAX_VALUE): a string or a number being the end of the transaction date range.
-  **operation** (default = intersects): the spatial operation to be performed, it can be **intersects**,
   **contains** and **is\_within**.

Bitemporal searching is so complex that we want to stay an example.

We want to implement a system for census bureau to track where resides a citizen and when the censyus bureau knows this.

First we create the table where all this data resides:

.. code-block:: sql

    CREATE KEYSPACE test with replication = {'class':'SimpleStrategy', 'replication_factor': 1};
    USE test;

    CREATE TABLE census (
        name text,
        city text,
        vt_from text,
        vt_to text,
        tt_from text,
        tt_to text,
        lucene text,
        PRIMARY KEY (name, vt_from, tt_from)
    );


Second, we create the index:

.. code-block:: sql

    CREATE CUSTOM INDEX census_index on census(lucene)
    USING 'com.stratio.cassandra.lucene.Index'
    WITH OPTIONS = {
        'refresh_seconds' : '1',
        'schema' : '{
            fields : {
                bitemporal : {
                    type : "bitemporal",
                    tt_from : "tt_from",
                    tt_to : "tt_to",
                    vt_from : "vt_from",
                    vt_to : "vt_to",
                    pattern : "yyyy/MM/dd",
                    now_value : "2200/12/31"}
            }
    }'};

We insert the population of 5 citizens lives in each city from 2015/01/01 until now


.. code-block:: sql

    INSERT INTO census(citizen_name, city, vt_from, vt_to, tt_from, tt_to)
    VALUES ('John', 'Madrid', '2015/01/01', '2200/12/31', '2015/01/01', '2200/12/31');

    INSERT INTO census(citizen_name, city, vt_from, vt_to, tt_from, tt_to)
    VALUES ('Margaret', 'Barcelona', '2015/01/01', '2200/12/31', '2015/01/01', '2200/12/31');

    INSERT INTO census(citizen_name, city, vt_from, vt_to, tt_from, tt_to)
    VALUES ('Cristian', 'Ceuta', '2015/01/01', '2200/12/31', '2015/01/01', '2200/12/31');

    INSERT INTO census(citizen_name, city, vt_from, vt_to, tt_from, tt_to)
    VALUES ('Edward', 'New York','2015/01/01', '2200/12/31', '2015/01/01', '2200/12/31');

    INSERT INTO census(citizen_name, city, vt_from, vt_to, tt_from, tt_to)
    VALUES ('Johnatan', 'San Francisco', '2015/01/01', '2200/12/31', '2015/01/01', '2200/12/31');


John moves to Amsterdam in '2015/03/05' but he does not comunicate this to census bureau until '2015/06/29' because he need it to apply for taxes reduction.

So, the system need to update last information from John,and insert the new. This is done with batch execution updating the transaction time end of previous data and inserting new.


.. code-block:: sql

    BEGIN BATCH
        UPDATE census SET tt_to = '2015/06/29'
        WHERE citizen_name = 'John' AND vt_from = '2015/01/01' AND tt_from = '2015/01/01'
        IF tt_to = '2200/12/31';

        INSERT INTO census(citizen_name, city, vt_from, vt_to, tt_from, tt_to)
        VALUES ('John', 'Amsterdam', '2015/03/05', '2200/12/31', '2015/06/30', '2200/12/31');
    APPLY BATCH;

Now , we can see the main difference between valid time and transaction time. The system knows from '2015/01/01' to '2015/06/29' that John resides in Madrid from '2015/01/01' until now, and resides in Amsterdam from '2015/03/05' until now.

There are several types of queries concerning this type of indexing

If its needed to get all the data in the table:

.. code-block:: sql

    SELECT name, city, vt_from, vt_to, tt_from, tt_to FROM census ;


If you want to know what is the last info about where John resides, you perform a query with tt_from and tt_to setted to now_value:

.. code-block:: sql

    SELECT name, city, vt_from, vt_to, tt_from, tt_to FROM census
    WHERE lucene = '{
        filter : {
            type : "bitemporal",
            field : "bitemporal",
            vt_from : 0,
            vt_to : "2200/12/31",
            tt_from : "2200/12/31",
            tt_to : "2200/12/31"
        }
    }'
    AND name='John';


If the test case needs to know what the system was thinking at '2015/03/01' about where John resides.

.. code-block:: sql

    SELECT name, city, vt_from, vt_to, tt_from, tt_to FROM census
    WHERE lucene = '{
        filter : {
            type : "bitemporal",
            field : "bitemporal",
            tt_from : "2015/03/01",
            tt_to : "2015/03/01"
        }
    }'
    AND name = 'John';

This code is available in CQL script here: `example_bitemporal.cql </doc/resources/example_bitemporal.cql>`__.

Boolean search
==============

Syntax:

.. code-block:: sql

    SELECT ( <fields> | * )
    FROM <table>
    WHERE <magic_column> = '{ (filter | query) : {
                               type     : "boolean",
                               ( must   : [(search,)?] , )?
                               ( should : [(search,)?] , )?
                               ( not    : [(search,)?] , )? } }';

where:

-  **must**: represents the conjunction of searches: search_1 AND search_2
   AND … AND search_n
-  **should**: represents the disjunction of searches: search_1 OR search_2
   OR … OR search_n
-  **not**: represents the negation of the disjunction of searches:
   NOT(search_1 OR search_2 OR … OR search_n)

Since "not" will be applied to the results of a "must" or "should"
condition, it can not be used in isolation.

Example 1: will return rows where name ends with “a” AND food starts
with “tu”

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{filter : {
                            type : "boolean",
                            must : [{type : "wildcard", field : "name", value : "*a"},
                                    {type : "wildcard", field : "food", value : "tu*"}]}}';

Example 2: will return rows where food starts with “tu” but name does
not end with “a”

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{filter : {
                            type : "boolean",
                            not  : [{type : "wildcard", field : "name", value : "*a"}],
                            must : [{type : "wildcard", field : "food", value : "tu*"}]}}';

Example 3: will return rows where name ends with “a” or food starts with
“tu”

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{filter : {
                            type   : "boolean",
                            should : [{type : "wildcard", field : "name", value : "*a"},
                                      {type : "wildcard", field : "food", value : "tu*"}]}}';

Example 4: will return zero rows independently of the index contents

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{filter : { type   : "boolean"} }';

Example 5: will return rows where name does not end with “a”, which is
a resource-intensive pure negation search

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{filter : {
                            not  : [{type : "wildcard", field : "name", value : "*a"}]}}';

Contains search
===============

Syntax:

.. code-block:: sql

    SELECT ( <fields> | * )
    FROM <table>
    WHERE <magic_column> = '{ (filter | query) : {
                                type  : "contains",
                                field : <fieldname> ,
                                values : <value_list> }}';

Example 1: will return rows where name matches “Alicia” or “mancha”

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{filter : {
                            type   : "contains",
                            field  : "name",
                            values : ["Alicia","mancha"] }}';

Example 2: will return rows where date matches “2014/01/01″,
“2014/01/02″ or “2014/01/03″

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{filter : {
                            type   : "contains",
                            field  : "date",
                            values : ["2014/01/01", "2014/01/02", "2014/01/03"] }}';

Date range search
=================

Syntax:

.. code-block:: sql

    SELECT ( <fields> | * )
    FROM <table>
    WHERE <magic_column> = '{ (filter | query) : {
                                type  : "date_range",
                                (from : <from> ,)?
                                (to   : <to> ,)?
                                (operation: <operation> )?
                              }}';

where:

-  **from**: a string or a number being the beginning of the date
   range.
-  **to**: a string or a number being the end of the date range.
-  **operation**: the spatial operation to be performed, it can be
   **intersects**, **contains** and **is\_within**.

Example 1: will return rows where duration intersects "2014/01/01" and
"2014/12/31"

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{ filter : {
                        type      : "date_range",
                        field     : "duration",
                        from      : "2014/01/01",
                        to        : "2014/12/31",
                        operation : "intersects"}}';

Example 2: will return rows where duration contains "2014/06/01" and
"2014/06/02"

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{ filter : {
                        type      : "date_range",
                        field     : "duration",
                        from      : "2014/06/01",
                        to        : "2014/06/02",
                        operation : "contains"}}';

Example 3: will return rows where duration is within "2014/01/01" and
"2014/12/31"

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{ filter : {
                        type      : "date_range",
                        field     : "duration",
                        from      : "2014/01/01",
                        to        : "2014/12/31",
                        operation : "is_within"}}';


Example 3:

Fuzzy search
============

Syntax:

.. code-block:: sql

    SELECT ( <fields> | * )
    FROM <table>
    WHERE <magic_column> = '{ (filter | query) : {
                                type  : "fuzzy",
                                field : <fieldname> ,
                                value : <value>
                                (, max_edits     : <max_edits> )?
                                (, prefix_length : <prefix_length> )?
                                (, max_expansions: <max_expansion> )?
                                (, transpositions: <transposition> )?
                              }}';

where:

-  **max\_edits** (default = 2): a integer value between 0 and 2. Will
   return rows which distance from <value> to <field> content has a
   distance of at most <max\_edits>. Distance will be interpreted
   according to the value of “transpositions”.
-  **prefix\_length** (default = 0): an integer value being the length
   of the common non-fuzzy prefix
-  **max\_expansions** (default = 50): an integer for the maximum number
   of terms to match
-  **transpositions** (default = true): if transpositions should be
   treated as a primitive edit operation (`Damerau-Levenshtein
   distance <http://en.wikipedia.org/wiki/Damerau%E2%80%93Levenshtein_distance>`__).
   When false, comparisons will implement the classic `Levenshtein
   distance <http://en.wikipedia.org/wiki/Levenshtein_distance>`__.

Example 1: will return any rows where “phrase” contains a word that
differs in one edit operation from “puma”, such as “pumas”.

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{filter : { type      : "fuzzy",
                                     field     : "phrase",
                                     value     : "puma",
                                     max_edits : 1 }}';

Example 2: same as example 1 but will limit the results to rows where
phrase contains a word that starts with “pu”.

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{filter : { type          : "fuzzy",
                                     field         : "phrase",
                                     value         : "puma",
                                     max_edits     : 1,
                                     prefix_length : 2 }}';

Geo bbox search
===============

Syntax:

.. code-block:: sql

    SELECT ( <fields> | * )
    FROM <table>
    WHERE <magic_column> = '{ (filter | query) : {
                                type           : "geo_bbox",
                                field          : <fieldname>,
                                min_latitude   : <min_latitude> ,
                                max_latitude   : <max_latitude> ,
                                min_longitude  : <min_longitude> ,
                                max_longitude  : <max_longitude>
                              }}';

where:

-  **min\_latitude** : a double value between -90 and 90 being the min
   allowed latitude.
-  **max\_latitude** : a double value between -90 and 90 being the max
   allowed latitude.
-  **min\_longitude** : a double value between -180 and 180 being the
   min allowed longitude.
-  **max\_longitude** : a double value between -180 and 180 being the
   max allowed longitude.

Example 1: will return any rows where “place” is formed by a latitude
between -90.0 and 90.0, and a longitude between -180.0 and
180.0.

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{filter : { type : "geo_bbox",
                                     field : "place",
                                     min_latitude : -90.0,
                                     max_latitude : 90.0,
                                     min_longitude : -180.0,
                                     max_longitude : 180.0 }}';

Example 2: will return any rows where “place” is formed by a latitude
between -90.0 and 90.0, and a longitude between 0.0 and
10.0.

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{filter : { type : "geo_bbox",
                                     field : "place",
                                     min_latitude : -90.0,
                                     max_latitude : 90.0,
                                     min_longitude : 0.0,
                                     max_longitude : 10.0 }}';


Example 3: will return any rows where “place” is formed by a latitude
between 0.0 and 10.0, and a longitude between -180.0 and
180.0.

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{filter : { type : "geo_bbox",
                                     field : "place",
                                     min_latitude : 0.0,
                                     max_latitude : 10.0,
                                     min_longitude : -180.0,
                                     max_longitude : 180.0 }}';


Geo distance search
===================

Syntax:

.. code-block:: sql

    SELECT ( <fields> | * )
    FROM <table>
    WHERE <magic_column> = '{ (filter | query) : {
                                type  : "geo_distance",
                                field : <fieldname> ,
                                latitude : <latitude> ,
                                longitude : <longitude> ,
                                max_distance : <max_distance>
                                (, min_distance : <min_distance> )?
                              }}';

where:

-  **latitude** : a double value between -90 and 90 being the latitude
   of the reference point.
-  **longitude** : a double value between -180 and 180 being the
   longitude of the reference point.
-  **max\_distance** : a string value being the max allowed distance
   from the reference point.
-  **min\_distance** : a string value being the min allowed distance
   from the reference point.

Example 1: will return any rows where “place” is within one kilometer
from the geo point (40.225479, -3.999278).

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{filter : { type : "geo_distance",
                                     field : "place",
                                     latitude : 40.225479,
                                     longitude : -3.999278,
                                     max_distance : "1km"}}';

Example 2: will return any rows where “place” is within one yard and ten
yards from the geo point (40.225479, -3.999278).

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{filter : { type : "geo_distance",
                                     field : "place",
                                     latitude : 40.225479,
                                     longitude : -3.999278,
                                     max_distance : "10yd" ,
                                     min_distance : "1yd" }}';

Match search
============

Syntax:

.. code-block:: sql

    SELECT ( <fields> | * )
    FROM <table>
    WHERE <magic_column> = '{ (filter | query) : {
                                  type  : "match",
                                  field : <fieldname> ,
                                  value : <value> }}';

Example 1: will return rows where name matches “Alicia”

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{filter : {
                           type  : "match",
                           field : "name",
                           value : "Alicia" }}';

Example 2: will return rows where phrase contains “mancha”

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{filter : {
                           type  : "match",
                           field : "phrase",
                           value : "mancha" }}';

Example 3: will return rows where date matches “2014/01/01″

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{filter : {
                           type  : "match",
                           field : "date",
                           value : "2014/01/01" }}';

None search
===========

Syntax:

.. code-block:: sql

    SELECT ( <fields> | * )
    FROM <table>
    WHERE <magic_column> = '{ (filter | query) : { type  : "none"} }';

Example: will return no one of the indexed rows

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{filter : { type  : "none" } }';

Phrase search
=============

Syntax:

.. code-block:: sql

    SELECT ( <fields> | * )
    FROM <table>
    WHERE <magic_column> = '{ (filter | query) : {
                                type  :"phrase",
                                field : <fieldname> ,
                                value : <value>
                                (, slop : <slop> )?
                            }}';

where:

-  **values**: an ordered list of values.
-  **slop** (default = 0): number of words permitted between words.

Example 1: will return rows where “phrase” contains the word “camisa”
followed by the word “manchada”.

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{filter : {
                          type   : "phrase",
                          field  : "phrase",
                          values : "camisa manchada" }}';

Example 2: will return rows where “phrase” contains the word “mancha”
followed by the word “camisa” having 0 to 2 words in between.

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{filter : {
                          type   : "phrase",
                          field  : "phrase",
                          values : "mancha camisa",
                          slop   : 2 }}';

Prefix search
=============

Syntax:

.. code-block:: sql

    SELECT ( <fields> | * )
    FROM <table>
    WHERE <magic_column> = '{ (filter | query) : {
                                type  : "prefix",
                                field : <fieldname> ,
                                value : <value> }}';

Example: will return rows where “phrase” contains a word starting with
“lu”. If the column is indexed as “text” and uses an analyzer, words
ignored by the analyzer will not be retrieved.

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{filter : {
                           type  : "prefix",
                           field : "phrase",
                           value : "lu" }}';

Range search
============

Syntax:

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{(filter | query) : {
                            type     : "range",
                            field    : <fieldname>
                            (, lower : <min> , include_lower : <min_included> )?
                            (, upper : <max> , include_upper : <max_included> )?
                         }}';

where:

-  **lower**: lower bound of the range.
-  **include\_lower** (default = false): if the lower bound is included
   (left-closed range).
-  **upper**: upper bound of the range.
-  **include\_upper** (default = false): if the upper bound is included
   (right-closed range).

Lower and upper will default to :math:`-/+\\infty` for number. In the
case of byte and string like data (bytes, inet, string, text), all
values from lower up to upper will be returned if both are specified. If
only “lower” is specified, all rows with values from “lower” will be
returned. If only “upper” is specified then all rows with field values
up to “upper” will be returned. If both are omitted than all rows will
be returned.

Example 1: will return rows where *age* is in [1, ∞)

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{filter : {
                            type          : "range",
                            field         : "age",
                            lower         : 1,
                            include_lower : true }}';

Example 2: will return rows where *age* is in (-∞, 0]

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{filter : {
                            type          : "range",
                            field         : "age",
                            upper         : 0,
                            include_upper : true }}';

Example 3: will return rows where *age* is in [-1, 1]

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{filter : {
                            type          : "range",
                            field         : "age",
                            lower         : -1,
                            upper         : 1,
                            include_lower : true,
                            include_upper : true }}';

Example 4: will return rows where *date* is in [2014/01/01, 2014/01/02]

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{filter : {
                            type          : "range",
                            field         : "date",
                            lower         : "2014/01/01",
                            upper         : "2014/01/02",
                            include_lower : true,
                            include_upper : true }}';

Regexp search
=============

Syntax:

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{(filter | query) : {
                            type  : "regexp",
                            field : <fieldname>,
                            value : <regexp>
                         }}';

where:

-  **value**: a regular expression. See
   `org.apache.lucene.util.automaton.RegExp <http://lucene.apache.org/core/4_6_1/core/org/apache/lucene/util/automaton/RegExp.html>`__
   for syntax reference.

Example: will return rows where name contains a word that starts with
“p” and a vowel repeated twice (e.g. “pape”).

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{filter : {
                           type  : "regexp",
                           field : "name",
                           value : "[J][aeiou]{2}.*" }}';

Wildcard search
===============

Syntax:

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{(filter | query) : {
                            type  : "wildcard" ,
                            field : <fieldname> ,
                            value : <wildcard_exp>
                         }}';

where:

-  **value**: a wildcard expression. Supported wildcards are \*, which
   matches any character sequence (including the empty one), and ?,
   which matches any single character. ” is the escape character.

Example: will return rows where food starts with or is “tu”.

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{filter : {
                           type  : "wildcard",
                           field : "food",
                           value : "tu*" }}';

Spark and Hadoop
****************

Spark and Hadoop integrations are fully supported because Lucene searches
can be combined with token range restrictions and pagination, which are the
basis of MapReduce frameworks support.

Token Range Searches
====================

The token function allows computing the token for a given partition key.
The primary key of the example table “users” is ((name, gender), animal,
age) where (name, gender) is the partition key. When combining the token
function and a Lucene-based filter in a where clause, the filter on
tokens is applied first and then the condition of the filter clause.

Example: will retrieve rows which tokens are greater than (‘Alicia’,
‘female’) and then test them against the match condition.

.. code-block:: sql

    SELECT name,gender
      FROM test.users
     WHERE stratio_col='{filter : {type : "match", field : "food", value : "chips"}}'
       AND token(name, gender) > token('Alicia', 'female');

Paging
======

Paging filtered results is fully supported. You can retrieve
the rows starting from a certain key. For example, if the primary key is
(userid, createdAt), you can search:

.. code-block:: sql

    SELECT *
      FROM tweets
      WHERE stratio_col = ‘{ filter : {type:”match",  field:”text", value:”cassandra”} }’
        AND userid = 3543534
        AND createdAt > 2011-02-03 04:05+0000
      LIMIT 5000;

JMX Interface
*************

The existing Lucene indexes expose some attributes and operations
through JMX, using the same MBean server as Apache Cassandra. The MBeans
provided by Stratio are under the domain
**com.stratio.cassandra.lucene**.

Please note that all the JMX attributes and operations refer to the
index shard living inside the local JVM, and not to the globally
distributed index.


+-------------------+-----------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| Name              | Type      | Notes                                                                                                                                                                                 |
+===================+===========+=======================================================================================================================================================================================+
| NumDeletedDocs    | Attribute | Total number of documents in the index.                                                                                                                                               |
+-------------------+-----------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| NumDocs           | Attribute | Total number of documents in the index.                                                                                                                                               |
+-------------------+-----------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| Commit            | Operation | Commits all the pending index changes to disk.                                                                                                                                        |
+-------------------+-----------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| Refresh           | Operation | Reopens all the readers and searchers to provide a recent view of the index.                                                                                                          |
+-------------------+-----------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| forceMerge        | Operation | Optimizes the index forcing merge segments leaving the specified number of segments. It also includes a boolean parameter to block until all merging completes.                       |
+-------------------+-----------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| forceMergeDeletes | Operation | Optimizes the index forcing merge segments containing deletions, leaving the specified number of segments. It also includes a boolean parameter to block until all merging completes. |
+-------------------+-----------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+

