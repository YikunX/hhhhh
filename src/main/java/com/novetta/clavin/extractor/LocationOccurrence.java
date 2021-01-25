package com.novetta.clavin.extractor;

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
 * LocationOccurrence.java
 *
 *###################################################################*/

/**
 * Container class representing a location name found in a document.
 * Stores the text of the location name itself, as well as its position
 * in the text in which it was found (measured in UTF-16 code points
 * from the start of the document).
 *
 */
public class LocationOccurrence {
    // text representation of the location (i.e., its name)
    private final String text;

    // number of UTF-16 code units from the start of the document at
    // which the location name starts
    private final int startOffset;
    private final int endOffset;

    /**
     * Sole construction for {@link LocationOccurrence} class.
     *
     * Represents a location name found in a document.
     *
     * @param text      text of the location name
     * @param position  where it was found
     */
    public LocationOccurrence(String text, int startOffset, int endOffset) {
        this.text = text;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    /**
     * Get the text of the location name.
     * @return the text of the location name
     */
    public String getText() {
        return text;
    }

    /**
     * Get the position in the text where the location name starts.
     * @return the number of UTF-16 code units from the start of the
     * document at which the location name starts
     */
    public int getStartOffset() {
        return startOffset;
    }
    
    /**
     * Get the position in the text where the location name ends.
     * @return the number of UTF-16 code units from the start of the
     * document at which the location name starts
     */
    public int getEndOffset() {
        return endOffset;
    }
    
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LocationOccurrence other = (LocationOccurrence) obj;
		if (endOffset != other.endOffset)
			return false;
		if (startOffset != other.startOffset)
			return false;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		return true;
	}

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + endOffset;
		result = prime * result + startOffset;
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		return result;
	}

    @Override
    public String toString() {
        return String.format("\"%s\":(%d, %d)", text, startOffset, endOffset);
    }
}