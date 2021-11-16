import requests

# run this code and copy/paste the resulting file to replace FeatureCode.java located in
# /src/main/java/com/novetta/clavin/gazetteer/FeatureCode.java
data = requests.get('http://download.geonames.org/export/dump/featureCodes_en.txt').text
with open('FeatureCode.java', 'w') as outfile:
	outfile.write('''package com.novetta.clavin.gazetteer;
		
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
		 * FeatureCode.java
		 *
		 *###################################################################*/
		/**
		 * Individual feature codes used by GeoNames. See http://www.geonames.org/export/codes.html
		 * or raw text file at http://download.geonames.org/export/dump/featureCodes_en.txt
		 *
		 */
		public enum FeatureCode {
	'''.replace("\t", ""))

	for row in data.split('\n'):
		try:
			classAndKey, longName, description = tuple(row.split("\t"))
			codeClass, codeKey = tuple(classAndKey.split("."))
			isHistorical = str(codeKey[-1] == 'H' and 'historical' in longName).lower()
		except Exception:
			if row and not row.startswith("null"):
				print(f"failed to parse row: {row}")
		else:
			outfile.write(f'\t{codeKey}(FeatureClass.{codeClass}, "{longName}", "{description}", {isHistorical}),\n')
	outfile.write('''
		// manually added to identify territories that can contain other administrative divisions
		TERRI(FeatureClass.A, "independent territory", "a territory that acts as an independent political entity", false),
		
		// manually added for locations not assigned to a feature code
		NULL(FeatureClass.NULL, "not available", "", false);
		
		// the feature class this feature code belongs to
		private final FeatureClass featureClass;
	
		// name of feature code
		private final String type;
	
		// description of feature code
		private final String description;
	
		// does this feature code represent a historical location
		private final boolean historical;
	
		/**
		 * Constructor for {@link FeatureCode} enum type.
		 *
		 * @param featureClass class this code belongs to
		 * @param type			name of code
		 * @param description	description of code
		 * @param historical	is this feature class a historical location
		 */
		private FeatureCode(FeatureClass featureClass, String type, String description, boolean historical) {
			this.featureClass = featureClass;
			this.type = type;
			this.description = description;
			this.historical = historical;
		}
	
		public FeatureClass getFeatureClass() {
			return featureClass;
		}
	
		public String getType() {
			return type;
		}
	
		public String getDescription() {
			return description;
		}
	
		public boolean isHistorical() {
			return historical;
		}'''.replace('\n\t', '\n'))
	outfile.write('\n}\n')
