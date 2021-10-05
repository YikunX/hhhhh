package com.novetta.clavin.index;

import static com.novetta.clavin.index.IndexField.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.novetta.clavin.gazetteer.BasicGeoName;
import com.novetta.clavin.gazetteer.CountryCode;
import com.novetta.clavin.gazetteer.FeatureClass;
import com.novetta.clavin.gazetteer.FeatureCode;
import com.novetta.clavin.gazetteer.GeoName;

/*#####################################################################
 *
 * CLAVIN (Cartographic Location And Vicinity INdexer)
 * ---------------------------------------------------
 *
 * Copyright (C) 2012-2013 Berico Technologies
 * http://clavin.bericotechnologies.com
 *
 * ====================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * ====================================================================
 *
 * IndexDirectoryBuilder.java
 *
 *###################################################################*/

/**
 * Builds a Lucene index of geographic entries based on
 * the GeoNames gazetteer.
 *
 * This program is run one-time before CLAVIN can be used.
 *
 */
public class IndexDirectoryBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(IndexDirectoryBuilder.class);
    private static final String HELP_OPTION = "help";
    private static final String FULL_ANCESTRY_OPTION = "with-full-ancestry";
    private static final String GAZETTEER_FILES_OPTION = "gazetteer-files";
    private static final String INDEX_PATH_OPTION = "index-path";
    private static final String REPLACE_INDEX_OPTION = "replace-index";
    private static final String ALTERNATE_NAMES_OPTION = "alt-names-file";

    private static final String[] DEFAULT_GAZETTEER_FILES = new String[] {
        "./allCountries.txt",
        "./src/main/resources/SupplementaryGazetteer.txt"
    };
    private static final String DEFAULT_INDEX_DIRECTORY = "./IndexDirectory";

    private final Map<String, GeoName> adminMap;
    private final Map<String, Set<GeoName>> unresolvedMap;
    private final Map<Integer, AlternateName> alternateNameMap;
    private final boolean fullAncestry;

    private IndexWriter indexWriter;
    private int indexCount;

    protected IndexDirectoryBuilder(final boolean fullAncestryIn) {
        adminMap = new TreeMap<>();
        unresolvedMap = new TreeMap<>();
        alternateNameMap = new HashMap<>();
        this.fullAncestry = fullAncestryIn;
    }

    /*
     * Builds the index using a gazetteer.
     * 
     * @param indexDir     		index directory location
     * @param gazetteerFiles	list of gazetteer files to process
     * @param altNamesFile		alternate names file to adjust gazetteer entries
     * @throws IOException		throws exception when building index
     */
    public void buildIndex(final File indexDir, final List<File> gazetteerFiles, final File altNamesFile) throws IOException {
        LOG.info("Indexing... please wait.");

        indexCount = 0;

        // Create a new index file on disk, allowing Lucene to choose
        // the best FSDirectory implementation given the environment.
        FSDirectory index = FSDirectory.open(indexDir.toPath());

        // indexing by lower-casing & tokenizing on whitespace
        Analyzer indexAnalyzer = new StandardAnalyzer(Reader.nullReader());

        // create the object that will actually build the Lucene index
        indexWriter = new IndexWriter(index, new IndexWriterConfig(indexAnalyzer));

        // let's see how long this takes...
        Date start = new Date();

        // if we were given an alternate names file, process it
        if (altNamesFile != null) {
            loadAlternateNames(altNamesFile);
        }

        // load GeoNames gazetteer into Lucene index
        String line;
        int count = 0;
        for (File gazetteer : gazetteerFiles) {
            LOG.info("Processing Gazetteer: {}", gazetteer.getAbsolutePath());
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(gazetteer), "UTF-8"));
            while ((line = reader.readLine()) != null) {
                try {
                    count += 1;
                    // print progress update to console
                    if (count % 100000 == 0 ) {
                        LOG.info("rowcount: {}", count);
                    }
                    GeoName geoName = BasicGeoName.parseFromGeoNamesRecord(line);
                    resolveAncestry(geoName);
                } catch (IOException|RuntimeException e) {
                    LOG.info("{} ({})", e.getCause(), e.getMessage());
                }
            }
            reader.close();
        }

        // that wasn't so long, was it?
        Date stop = new Date();

        LOG.info("Unresolved GeoNames (Pre-resolution)");
        logUnresolved();

        resolveUnresolved();

        LOG.info("Unresolved GeoNames (Post-resolution)");
        logUnresolved();

        LOG.info("Indexing unresolved GeoNames.");
        for (Set<GeoName> geos : unresolvedMap.values()) {
            for (GeoName nm : geos) {
                indexGeoName(nm);
            }
        }

        LOG.info("[DONE]");
        LOG.info("{} geonames added to index. ({} records)", indexWriter.getDocStats().maxDoc, indexCount);
        LOG.info("Merging indices... please wait.");

        indexWriter.close();
        index.close();

        LOG.info("[DONE]");

        DateFormat df = new SimpleDateFormat("HH:mm:ss");
        long elapsedTime = stop.getTime() - start.getTime();
        LOG.info("Process started: {}, ended: {}; elapsed time: {} seconds.",
        		df.format(start), df.format(stop), MILLISECONDS.toSeconds(elapsedTime));
    }

    private static final int ALT_NAMES_ID_FIELD = 1;
    private static final int ALT_NAMES_LANG_FIELD = 2;
    private static final int ALT_NAMES_NAME_FIELD = 3;
    private static final int ALT_NAMES_PREFERRED_FIELD = 4;
    private static final int ALT_NAMES_SHORT_FIELD = 5;
    private static final String ALT_NAMES_TRUE = "1";
    private static final String ISO2_ENGLISH = "en";
    private static final String ISO3_ENGLISH = "eng";

    private void loadAlternateNames(final File altNamesFile) throws IOException {
        LOG.info("Reading alternate names file: {}", altNamesFile.getAbsolutePath());

        // parse all lines of the alternate names database and store only the 'en' names
        // marked as preferred or short names for each location
        //
        // Column format (see http://download.geonames.org/export/dump/)
        // ------------------------------------------------------
        // alternateNameId   : the id of this alternate name, int
        // geonameid         : geonameId referring to id in table 'geoname', int
        // isolanguage       : iso 639 language code 2- or 3-characters; 4-characters 'post' for postal
        //                     codes and 'iata','icao' and faac for airport codes, fr_1793 for French
        //                     Revolution names,  abbr for abbreviation, link for a website, varchar(7)
        // alternate name    : alternate name or name variant, varchar(200)
        // isPreferredName   : '1', if this alternate name is an official/preferred name
        // isShortName       : '1', if this is a short name like 'California' for 'State of California'
        // isColloquial      : '1', if this alternate name is a colloquial or slang term
        // isHistoric        : '1', if this alternate name is historic and was used in the past

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(altNamesFile), "UTF-8"));
        String line;
        int lineNum = 0;
        while ((line = reader.readLine()) != null) {
            lineNum++;
            AlternateName name = new AlternateName(line);
            if (name.isEnglish() && name.isPrefOrShort()) {
                alternateNameMap.put(name.geonameId, name.bestName(alternateNameMap.get(name.geonameId)));
            }
        }
        reader.close();

        LOG.info("Processed {} alternate names.  Found {} names.", lineNum, alternateNameMap.size());
    }

    private void resolveAncestry(final GeoName geoname) throws IOException {
        // set this GeoName's parent if it is known
        String parentKey = geoname.getParentAncestryKey();
        
        // if we cannot successfully set the parent, add to the unresolved map,
        // waiting for a parent to be set
        if (parentKey != null && (!geoname.setParent(adminMap.get(parentKey)) || !geoname.isAncestryResolved())) {
            Set<GeoName> unresolved = unresolvedMap.computeIfAbsent(parentKey, k -> new HashSet<>());
            unresolved.add(geoname);
        }
        // if this geoname is fully resolved, add it to the index
        if (geoname.isAncestryResolved()) {
            indexGeoName(geoname);
        }

        // if this is an administrative division, configure the parent of any waiting
        // GeoNames and notify all 2nd level and further descendants their tree has been
        // updated
        String myKey = geoname.getAncestryKey();
        if (myKey != null) {
            GeoName conflict = adminMap.get(myKey);
            if (conflict != null) {
                LOG.error(String.format("Resolved duplicate admin key [%s] for GeoNames (%d %s:%s %s) and (%d %s:%s %s)",
                        myKey, conflict.getGeonameID(), conflict.getFeatureClass(), conflict.getFeatureCode(), conflict.getName(),
                        geoname.getGeonameID(), geoname.getFeatureClass(), geoname.getFeatureCode(), geoname.getName()));
            }
            adminMap.put(myKey, geoname);
            checkDescendantsResolved(geoname, true);
        }
    }

    private void checkDescendantsResolved(final GeoName geoname, final boolean setParent) throws IOException {
        String key = geoname.getAncestryKey();
        if (key != null) {
            Set<GeoName> descendants = unresolvedMap.get(key);
            if (descendants != null) {
                // use an iterator so we can remove elements
                Iterator<GeoName> iter = descendants.iterator();
                while (iter.hasNext()) {
                    GeoName desc = iter.next();
                    if (setParent && !desc.setParent(geoname)) {
                    	LOG.error("Error setting parent [{}] of GeoName [{}].", geoname, desc);
                    }
                    if (desc.isAncestryResolved()) {
                        checkDescendantsResolved(desc, false);
                        indexGeoName(desc);
                        iter.remove();
                    }
                }
                if (descendants.isEmpty()) {
                    unresolvedMap.remove(key);
                }
            }
        }
    }

    private void resolveUnresolved() throws IOException {
        // sort keys in ascending order by level of specificity and name
        Set<String> keys = new TreeSet<>(new Comparator<String>() {
            @Override
            public int compare(final String strA, final String strB) {
                int specA = strA.split("\\.").length;
                int specB = strB.split("\\.").length;
                return specA != specB ? specA - specB : strA.compareTo(strB);
            }
        });
        keys.addAll(unresolvedMap.keySet());

        // iterate over keys, attempting to resolve less specific keys first; if
        // they are resolved, this may result in more specific keys being resolved
        // as well
        for (String key : keys) {
            String subKey = key;
            GeoName parent = null;
            int lastDot;
            while (parent == null && (lastDot = subKey.lastIndexOf(".")) > 0) {
                subKey = key.substring(0, lastDot);
                parent = adminMap.get(subKey);
            }
            if (parent != null) {
                Set<GeoName> unresolved = unresolvedMap.get(key);
                if (unresolved == null) {
                    // resolving a higher-level key also resolved this key; do nothing
                    break;
                }
                Iterator<GeoName> iter = unresolved.iterator();
                // use iterator so we can remove
                while (iter.hasNext()) {
                    GeoName geoName = iter.next();
                    // first check to see if a previous loop resolved all parents
                    if (geoName.isAncestryResolved()) {
                        indexGeoName(geoName);
                        iter.remove();
                    } else if (geoName.setParent(parent)) {
                        if (geoName.isAncestryResolved()) {
                            // ancestry has been resolved, remove from the unresolved collection
                            indexGeoName(geoName);
                            iter.remove();
                        } else {
                            LOG.error("GeoName [{}] should be fully resolved. (parent: {})", geoName, parent);
                        }
                    } else {
                        LOG.error("Unable to set parent of {} to {}", geoName, parent);
                    }
                }
                if (unresolved.isEmpty()) {
                    unresolvedMap.remove(key);
                }
            } else {
                LOG.error("Unable to resolve parent for GeoName key: {}", key);
            }
        }
    }

    /**
     * Builds a set of Lucene documents for the provided GeoName, indexing
     * each using all available names and storing the entire ancestry path
     * for each GeoName in the index.  See {@link IndexField} for descriptions
     * of the fields indexed for each document.
     *
     * @param geoName       the GeoName to index
     * @throws IOException  if an error occurs while indexing
     */
    private void indexGeoName(final GeoName geoName) throws IOException {
        indexCount++;
        
        // find all unique names for this GeoName
        String nm = geoName.getName();
        String asciiNm = geoName.getAsciiName();
        Set<String> names = new HashSet<>();
        names.add(nm);
        names.add(asciiNm);
        names.addAll(geoName.getAlternateNames());
        
        // if this is a top-level administrative division, add its primary and alternate country codes
        // if they are not already found in the name or alternate names
        if (geoName.isTopLevelAdminDivision()) {
            if (geoName.getPrimaryCountryCode() != null) {
                names.add(geoName.getPrimaryCountryCode().name());
            }
            for (CountryCode cc : geoName.getAlternateCountryCodes()) {
                names.add(cc.name());
            }
        }
        AlternateName preferredName = alternateNameMap.get(geoName.getGeonameID());
        // ensure preferred name is found in alternate names
        if (preferredName != null) {
            names.add(preferredName.name);
        }
        names.remove(null);
        names.remove("");

        // reuse a single Document and field instances
        Document doc = new Document();
        doc.add(new StoredField(GEONAME.key(), fullAncestry ? geoName.getGazetteerRecordWithAncestry() : geoName.getGazetteerRecord()));
        doc.add(new StoredField(GEONAME_ID.key(), geoName.getGeonameID()));				// store the value
        doc.add(new IntPoint(GEONAME_ID.key(), geoName.getGeonameID()));				// allow range queries
        doc.add(new NumericDocValuesField(GEONAME_ID.key(), geoName.getGeonameID()));	// allow sorting and scoring
        
        // if the alternate names file was loaded and we found a preferred name for this GeoName, store it
        if (preferredName != null) {
            doc.add(new StoredField(PREFERRED_NAME.key(), preferredName.name));
        }
        // index the direct parent ID in the PARENT_ID field
        GeoName parent = geoName.getParent();
        if (parent != null) {
            doc.add(new StoredField(PARENT_ID.key(), parent.getGeonameID()));
            doc.add(new IntPoint(PARENT_ID.key(), parent.getGeonameID()));
            doc.add(new NumericDocValuesField(PARENT_ID.key(), parent.getGeonameID()));
        }
        // index all ancestor IDs in the ANCESTOR_IDS field; this is a secondary field
        // so it can be used to restrict searches and PARENT_ID can be used for ancestor
        // resolution
        while (parent != null) {
            doc.add(new StoredField(ANCESTOR_IDS.key(), parent.getGeonameID()));
            doc.add(new IntPoint(ANCESTOR_IDS.key(), parent.getGeonameID()));
            //doc.add(new NumericDocValuesField(ANCESTOR_IDS.key(), parent.getGeonameID()));
            parent = parent.getParent();
        }
        doc.add(new StoredField(POPULATION.key(), geoName.getGeonameID()));
        doc.add(new LongPoint(POPULATION.key(), geoName.getGeonameID()));
        doc.add(new NumericDocValuesField(POPULATION.key(), geoName.getGeonameID()));
        
        // set up sort field based on population and geographic feature type
        // TODO: remove temporary hack once GeoNames.org fixes the population for City of London
        int populationBoost = 1;
        if ((geoName.getFeatureClass().equals(FeatureClass.P) || geoName.getFeatureCode().name().startsWith("PCL"))
        		&& geoName.getGeonameID() != 2643741) {
        	// boost cities and countries when sorting results by population
        	populationBoost = 11;
        } else {
            // don't boost anything else, because people rarely talk about other stuff
            // (e.g., Washington State's population is more than 10x that of Washington, DC
            // but Washington, DC is mentioned far more frequently than Washington State)
        	// although honestly there's probably a lot of room for improvement here
        }
        doc.add(new StoredField(SORT_POP.key(), geoName.getPopulation() * populationBoost));
        doc.add(new LongPoint(SORT_POP.key(), geoName.getPopulation() * populationBoost));
        doc.add(new NumericDocValuesField(SORT_POP.key(), geoName.getPopulation() * populationBoost));
        
        int isHistorical = IndexField.getBooleanIndexValue(geoName.getFeatureCode().isHistorical());
        doc.add(new IntPoint(HISTORICAL.key(), isHistorical));
        doc.add(new NumericDocValuesField(HISTORICAL.key(), isHistorical));
        doc.add(new StringField(FEATURE_CODE.key(), geoName.getFeatureCode().name(), Field.Store.NO));

        // create a unique Document for each name of this GeoName
        TextField nameField = new TextField(INDEX_NAME.key(), "", Field.Store.YES);
        doc.add(nameField);
        for (String name : names) {
            nameField.setStringValue(name);
            indexWriter.addDocument(doc);
        }
    }

    private void logUnresolved() {
        int unresolvedGeoCount = 0;
        Map<String, Integer> unresolvedCodeMap = new TreeMap<>();
        Map<String, Integer> missingCodeMap = new TreeMap<>();
        for (Map.Entry<String, Set<GeoName>> entry : unresolvedMap.entrySet()) {
            LOG.trace("{}: {} unresolved GeoNames", entry.getKey(), entry.getValue().size());
            unresolvedGeoCount += entry.getValue().size();
            FeatureCode code;
            switch (entry.getKey().split("\\.").length) {
                case 1:
                    code = FeatureCode.PCL;
                    break;
                case 2:
                    code = FeatureCode.ADM1;
                    break;
                case 3:
                    code = FeatureCode.ADM2;
                    break;
                case 4:
                    code = FeatureCode.ADM3;
                    break;
                case 5:
                    code = FeatureCode.ADM4;
                    break;
                default:
                    LOG.error("Unexpected ancestry key: {}", entry.getKey());
                    code = FeatureCode.NULL;
                    break;
            }
            if (missingCodeMap.containsKey(code.name())) {
                missingCodeMap.put(code.name(), missingCodeMap.get(code.name())+1);
            } else {
                missingCodeMap.put(code.name(), 1);
            }

            for (GeoName geo : entry.getValue()) {
                String featKey = String.format("%s:%s", geo.getFeatureClass(), geo.getFeatureCode());
                if (unresolvedCodeMap.containsKey(featKey)) {
                    unresolvedCodeMap.put(featKey, unresolvedCodeMap.get(featKey)+1);
                } else {
                    unresolvedCodeMap.put(featKey, 1);
                }
            }
        }
        LOG.info("Found {} administrative divisions.", adminMap.size());
        LOG.info("Found {} missing administrative keys.", unresolvedMap.size());
        for (Map.Entry<String, Integer> missingCode : missingCodeMap.entrySet()) {
            LOG.info("{}: {}", missingCode.getKey(), missingCode.getValue());
        }
        LOG.info("{} total unresolved GeoNames", unresolvedGeoCount);
        for (Map.Entry<String, Integer> unresolvedCode : unresolvedCodeMap.entrySet()) {
            LOG.trace("{}: {}", unresolvedCode.getKey(),
                    unresolvedCode.getValue());
        }
    }

    /**
     * Turns a GeoNames gazetteer file into a Lucene index, and adds
     * some supplementary gazetteer records at the end.
     *
     * @param args              not used
     * @throws IOException		throws exception on error when building index
     */
    public static void main(String[] args) throws IOException {
        Options options = getOptions();
        CommandLine cmd = null;
        CommandLineParser parser = new DefaultParser();
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException pe) {
            LOG.error(pe.getMessage());
            printHelp(options);
            System.exit(-1);
        }

        if (cmd.hasOption(HELP_OPTION)) {
            printHelp(options);
            System.exit(0);
        }

        String indexPath = cmd.getOptionValue(INDEX_PATH_OPTION, DEFAULT_INDEX_DIRECTORY);
        String[] gazetteerPaths = cmd.getOptionValues(GAZETTEER_FILES_OPTION);
        if (gazetteerPaths == null || gazetteerPaths.length == 0) {
            gazetteerPaths = DEFAULT_GAZETTEER_FILES;
        }
        boolean replaceIndex = cmd.hasOption(REPLACE_INDEX_OPTION);
        boolean fullAncestry = cmd.hasOption(FULL_ANCESTRY_OPTION);

        File idir = new File(indexPath);
        // if the index directory exists, delete it if we are replacing, otherwise
        // exit gracefully
        if (idir.exists() ) {
            if (replaceIndex) {
                LOG.info("Replacing index: {}", idir.getAbsolutePath());
                FileUtils.deleteDirectory(idir);
            } else {
                LOG.info("{} exists. Remove the directory and try again.", idir.getAbsolutePath());
                System.exit(-1);
            }
        }

        List<File> gazetteerFiles = new ArrayList<>();
        for (String gp : gazetteerPaths) {
            File gf = new File(gp);
            if (gf.isFile() && gf.canRead()) {
                gazetteerFiles.add(gf);
            } else {
                LOG.info("Unable to read Gazetteer file: {}", gf.getAbsolutePath());
            }
        }
        if (gazetteerFiles.isEmpty()) {
            LOG.error("No Gazetteer files found.");
            System.exit(-1);
        }

        String altNamesPath = cmd.getOptionValue(ALTERNATE_NAMES_OPTION);
        File altNamesFile = altNamesPath != null ? new File(altNamesPath) : null;
        if (altNamesFile != null && !(altNamesFile.isFile() && altNamesFile.canRead())) {
            LOG.error("Unable to read alternate names file: {}", altNamesPath);
            System.exit(-1);
        }

        new IndexDirectoryBuilder(fullAncestry).buildIndex(idir, gazetteerFiles, altNamesFile);
    }

    
	private static Options getOptions() {
        Options options = new Options();

        options.addOption(Option.builder("?")
                .longOpt(HELP_OPTION)
                .desc("Print help")
                .build());

        options.addOption(Option.builder()
                .longOpt(FULL_ANCESTRY_OPTION)
                .desc("Store the gazetteer records for the full ancestry tree of each element."
                        + " This will increase performance at the expense of a larger index.")
                .build());

        options.addOption(Option.builder("i")
                .longOpt(GAZETTEER_FILES_OPTION)
                .desc(String.format("The ':'-separated list of input Gazetteer files to parse.  Default: %s",
                        StringUtils.join(DEFAULT_GAZETTEER_FILES, ':')))
                .hasArgs()
                .valueSeparator(':')
                .build());

        options.addOption(Option.builder()
                .longOpt(ALTERNATE_NAMES_OPTION)
                .desc("When provided, the path to the GeoNames.org alternate names file for resolution of common and "
                        + "short names for each location. If not provided, the default name for each location will be used.")
                .hasArg()
                .build());

        options.addOption(Option.builder("o")
                .longOpt(INDEX_PATH_OPTION)
                .desc(String.format("The path to the output index directory. Default: %s", DEFAULT_INDEX_DIRECTORY))
                .hasArg()
                .build());

        options.addOption(Option.builder("r")
                .longOpt(REPLACE_INDEX_OPTION)
                .desc("Replace an existing index if it exists. If this option is not specified,"
                        + "index processing will fail if an index already exists at the specified location.")
                .build());

        return options;
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("run", options, true);
    }

    protected static class AlternateName implements Comparable<AlternateName> {
        private final int geonameId;
        private final String name;
        private final String lang;
        private final boolean preferredName;
        private final boolean shortName;

        public AlternateName(final String line) {
            String[] fields = line.split("\t");

            geonameId = Integer.parseInt(fields[ALT_NAMES_ID_FIELD]);
            lang = fields[ALT_NAMES_LANG_FIELD];
            name = fields[ALT_NAMES_NAME_FIELD];
            preferredName = fields.length > ALT_NAMES_PREFERRED_FIELD && ALT_NAMES_TRUE.equals(fields[ALT_NAMES_PREFERRED_FIELD].trim());
            shortName = fields.length > ALT_NAMES_SHORT_FIELD && ALT_NAMES_TRUE.equals(fields[ALT_NAMES_SHORT_FIELD].trim());
        }

        public boolean isEnglish() {
            return ISO2_ENGLISH.equalsIgnoreCase(lang) || ISO3_ENGLISH.equalsIgnoreCase(lang);
        }

        public boolean isPrefOrShort() {
            return preferredName || shortName;
        }

        @Override
		public String toString() {
			return "AlternateName [geonameId=" + geonameId + ", name=" + name + ", lang=" + lang + ", preferredName="
					+ preferredName + ", shortName=" + shortName + "]";
		}

		@Override
        public int compareTo(final AlternateName other) {
            int comp = geonameId - other.geonameId;
            comp = comp == 0 ? Boolean.compare(preferredName, other.preferredName) : comp;
            comp = comp == 0 ? Boolean.compare(shortName, other.shortName) : comp;
            comp = comp == 0 ? name.compareTo(other.name) : comp;
            return comp;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + geonameId;
            result = prime * result + (preferredName ? 1231 : 1237);
            result = prime * result + (shortName ? 1231 : 1237);
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            AlternateName other = (AlternateName) obj;
            if (geonameId != other.geonameId)
                return false;
            if (preferredName != other.preferredName)
                return false;
            if (shortName != other.shortName)
                return false;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            return true;
        }

        /**
         * Get the "best" alternate name for the target GeoName.  The best name
         * is selected in the following order:
         *
         * 1. non-null
         * 2. preferred AND short
         * 3. preferred only
         * 4. short only
         * 5. this
         *
         * Note that if the preferred and short name flags are identical, this method
         * returns the object on which it was called.
         *
         * @param other the object to compare to
         * @return the "best" AlternateName determined by the criteria listed above
         */
        public AlternateName bestName(final AlternateName other) {
            if (other == null) {
                return this;
            }

            // if one name is preferred and the other is not, use the preferred name
            int comp = Boolean.compare(preferredName, other.preferredName);
            // if preferred is the same, use a short name over a non-short name
            comp = comp == 0 ? Boolean.compare(shortName, other.shortName) : comp;
            // if all things are still equal, use this
            return comp >= 0 ? this : other;
        }
    }
}
