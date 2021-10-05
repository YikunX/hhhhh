package com.novetta.clavin.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;

import com.novetta.clavin.index.IndexDirectoryBuilder.AlternateName;

public class IndexDirectoryBuilderTest {
	@Test
	public void testBestName() throws Exception {
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
		
		String testLine1 = String.join("\t", List.of("1", "1", "eng", "The United States", "1", "1", "0", "0"));
		String testLine2 = String.join("\t", List.of("2", "1", "eng", "The United States of America", "1", "0", "0", "0"));
		String testLine3 = String.join("\t", List.of("5", "1", "eng", "The USA", "0", "1", "0", "0"));
		String testLine4 = String.join("\t", List.of("3", "1", "eng", "'Murca", "0", "0", "1", "0"));
		List<IndexDirectoryBuilder.AlternateName> names = List.of(
				new IndexDirectoryBuilder.AlternateName(testLine1),
				new IndexDirectoryBuilder.AlternateName(testLine2),
				new IndexDirectoryBuilder.AlternateName(testLine3),
				new IndexDirectoryBuilder.AlternateName(testLine4)
		);
		
		for (int i=0; i < names.size(); ++i) {
			for (int j=0; j < names.size(); ++j) {
				AlternateName result = names.get(i).bestName(names.get(j));
				if (i < j) {
					assertEquals(names.get(i), result);
				} else if (i > j) {
					assertEquals(names.get(j), result);
				} else {
					assertNotNull(result);
				}
			}
		}
	}
}
