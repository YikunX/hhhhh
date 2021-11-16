![CLAVIN](https://github.com/Novetta/CLAVIN/blob/develop/img/clavinLogo.png?raw=true)

![CLAVIN Master](https://github.com/Novetta/CLAVIN/workflows/MasterCI/badge.svg?branch=master)

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)


CLAVIN (*Cartographic Location And Vicinity INdexer*) is an open source software package for document geoparsing and georesolution that employs context-based geographic entity resolution. It combines a variety of open source tools with natural language processing techniques to extract location names from unstructured text documents and resolve them against gazetteer records. Importantly, CLAVIN does not simply "look up" location names; rather, it uses intelligent heuristics-based combinatorial optimization in an attempt to identify precisely which "Springfield" (for example) was intended by the author, based on the context of the document. CLAVIN also employs fuzzy search to handle incorrectly-spelled location names, and it recognizes alternative names (e.g., "Ivory Coast" and "Côte d'Ivoire") as referring to the same geographic entity. By enriching text documents with structured geo data, CLAVIN enables hierarchical geospatial search and advanced geospatial analytics on unstructured data.

CLAVIN natively uses Apache OpenNLP for extracting place names in text as part of this library. CLAVIN now also integrates with Novetta's own [AdaptNLP](https://github.com/Novetta/adaptnlp) project for place name extraction. To use AdaptNLP, you'll need to follow the instructions on that repo to bring up an instance of the extractor. Lastly, we also maintain the [clavin-nerd](https://github.com/novetta/clavin-nerd) project (which will be updated in the near future), that enables CLAVIN to use Stanford NER.

Novetta also maintains the [CLAVIN-Rest](https://github.com/novetta/clavin-rest) project, which provides a RESTful microservice wrapper around CLAVIN or CLAVIN-NERD.  CLAVIN-Rest is configured (and provides instructions) to easily build and run this package as a docker image. 

## Breaking changes

This release includes breaking changes in the form of an update to all namespaces.  The namespaces have been changed from com.bericotech to com.novetta which reflects a change in corporate ownership, and re-alignment to our new domain.   

## How to build & use CLAVIN:

1. Check out a copy of the source code:

```
git clone https://github.com/Novetta/CLAVIN.git
```

2. Move into the newly-created CLAVIN directory:

```	
cd CLAVIN
```

3. Download the latest version of allCountries.zip gazetteer file from GeoNames.org:

```
curl -O http://download.geonames.org/export/dump/allCountries.zip
```

4. Unzip the GeoNames gazetteer file:

```
unzip allCountries.zip
```

5. Compile the source code:

```
mvn compile
```

6. Create the Lucene Index (this one-time process will take several minutes):

```
MAVEN_OPTS="-Xmx4g" mvn exec:java -Dexec.mainClass="com.novetta.clavin.index.IndexDirectoryBuilder"
```

6b. If the previous step failed with an error about the FeatureCode enum type, GeoNames may have a newer version of their feature codes file. You can update your local CLAVIN project to be compatible with the latest version by running `utils/featureCodeEnumeration.py` and copy/pasting the resulting file to replace FeatureCode.java located in /src/main/java/com/novetta/clavin/gazetteer/FeatureCode.java

7. Run the example program:

```
MAVEN_OPTS="-Xmx2g" mvn exec:java -Dexec.mainClass="com.novetta.clavin.WorkflowDemo"
```
	
If you encounter an error that looks like this:

```
... InvocationTargetException: Java heap space ...
```
	
Set the appropriate environmental variable controlling Maven's memory usage, and increase the size with `export MAVEN_OPTS=-Xmx4g` or similar.

Once that all runs successfully, feel free to modify the CLAVIN source code to suit your needs.

**N.B.**: Loading the worldwide gazetteer uses a non-trivial amount of memory. When using CLAVIN in your own programs, if you encounter `Java heap space` errors (like the one described in Step 7), bump up the maximum heap size for your JVM.

## Add CLAVIN to your project:

CLAVIN is published to Maven Central. You can add a dependency on the CLAVIN project:

```xml
<dependency>
   <groupId>com.novetta</groupId>
   <artifactId>CLAVIN</artifactId>
   <version>3.0.0</version>
</dependency>
```

You will still need to build the GeoNames Lucene Index as described in steps 3, 4, and 6 in "How to build & use CLAVIN".


## Choosing an Extractor

When using this library, you're now able to choose between two different extractors: Novetta AdaptNLP and Apache OpenNLP. For AdaptNLP

**AdaptNLP**

Creating an AdaptNlpExtractor: 

```
LocationExtractor extractor = new AdaptNlpExtractor();
```

**OpenNLP**

Creating an ApacheExtractor: 

```
LocationExtractor extractor = new ApacheExtractor();
```

There are also some convenience methods in the GeoParserFactory for Apache OpenNLP. 

So, for example, to set up the Gazetteer, AdaptNLP Extractor and GeoParser classes from scratch, it looks like this with default settings:   

```
// the maximum hit depth for CLAVIN searches
private int maxHitDepth = 3;

// the maximum context window for CLAVIN searches
private int maxContextWindow = 5;

// switch controlling use of fuzzy matching
private boolean fuzzy = false;

// adaptnlp host, port
private string host = "http://localhost";
private int port = 5000;

Gazetteer gazetteer = new LuceneGazetteer(new File(pathToLuceneIndex));
LocationExtractor extractor = new AdaptNlpExtractor(host, port);
Geoparser parser = new GeoParser(extractor, gazetteer, maxHitDepth, maxContentWindow, fuzzy);

```


## License:

Copyright (C) 2012-2020 Novetta

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
