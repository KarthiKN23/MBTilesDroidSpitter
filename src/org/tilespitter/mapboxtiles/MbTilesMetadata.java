package org.tilespitter.mapboxtiles;

/** @author Simon Thépot aka djcoin <simon.thepot@gmail.com, simon.thepot@makina-corpus.net> */

import java.util.HashMap;
import java.util.LinkedHashMap;

import android.database.Cursor;


public class MbTilesMetadata {
	
	//  * name: The plain-english name of the tileset.
	//  * type: overlay or baselayer
	//  * version: The version of the tileset, as a plain number.
	//  * description: A description of the layer as plain text.
	//  * format: The image file format of the tile data: png or jpg
	public final String name, description, type, version, format;
	public final Float[] bounds;
	public final HashMap<String, String> extra;

	// MBTiles Specification
	public static final MetadataValidatorFactory metadataValidatorFactory = new MetadataValidatorFactory();	

	public MbTilesMetadata(String name, String description,  String type, String version, String format, Float[] bounds, HashMap<String, String> extra) {
		this.name = name;
		this.type = type;
		this.version = version;
		this.description = description;
		this.format = format;
		this.bounds = bounds;
		this.extra = extra;
	}
	
	@Override
	public String toString() {		
		String none = " - ";
		String separator = " | ";
		StringBuilder sb = new StringBuilder("Metadata:\n");
		sb.append("Name: "        + (name == null        ? none : name) + separator);
		sb.append("Type: "        + (type == null        ? none : type) + separator);
		sb.append("Version: "     + (version == null     ? none : version) + separator);
		sb.append("Description: " + (description == null ? none : description) + separator);
		sb.append("Format: "      + (format == null      ? none : format) + separator);
		sb.append("Bounds: "      + (bounds == null      ? none : bounds));
		sb.append("\n\n");
		
		sb.append("Extra: " + this.extra.toString() + "");
		return sb.toString();
	}

	public static MbTilesMetadata createFromCursor(Cursor c, int idx_col_key, int idx_col_value, MetadataValidator validator) throws MetadataParseException {
		HashMap<String, String> dumped =  new LinkedHashMap<String, String>(); 			
		c.moveToFirst();
		do {			
			dumped.put(c.getString(idx_col_key), c.getString(idx_col_value));
		} while (c.moveToNext());
		c.close();
		return validator.validate(dumped);
	}
	
	
	public static interface MetadataValidator {
		MbTilesMetadata validate(HashMap<String, String> hm) throws MetadataParseException;
	}
	
	@SuppressWarnings("serial")
	public static class MetadataParseException extends Exception {
		public MetadataParseException(String errorMessage) {
			super(errorMessage);
		}
	}
	
	public static class MetadataValidatorFactory {

		/* Validators for parsing Metadata */
		private final static MetadataValidator VALIDATOR_1_0 = new MetadataValidator_1_0();
		private final static MetadataValidator VALIDATOR_1_1 = new MetadataValidator_1_1();
		
		public static MetadataValidator getMetadataValidatorFromVersion(String version) {		
			if (version.equals("1.0"))
				return VALIDATOR_1_0;
			if (version.equals("1.1"))
				return VALIDATOR_1_1;
			return null;
		}
	}
	
	
	// https://github.com/mapbox/mbtiles-spec/blob/master/1.1/spec.md
	public static class MetadataValidator_1_1 implements MetadataValidator {

		@Override
		public MbTilesMetadata validate(HashMap<String, String> hm) throws MetadataParseException {
			String tmp;

			String name = hm.remove("name");
			if (name == null)
				throw new MetadataParseException("No mandatory field 'name'.");
			
			String description = hm.remove("description");
			if (description == null)
				throw new MetadataParseException("No mandatory field 'description'.");
			
			String type = hm.remove("type");
			if (type == null || !(type.equals("overlay") || type.equals("baselayer")) )
				throw new MetadataParseException("No mandatory field 'type' or not in [ overlay, baselayer ].");

			String version; 
			tmp = hm.remove("version");
			if (tmp == null)
				throw new MetadataParseException("No mandatory field 'version'");
			try {				
				Double.parseDouble(tmp);
				version = tmp;
			} catch (NumberFormatException e) {
				throw new MetadataParseException("Invalid syntax for mandatory field 'version'. Must be a plain number.");
			}

			String format = hm.remove("format");
			if (format == null || !(type.equals("png") || type.equals("jpg")) )
				throw new MetadataParseException("No mandatory field 'format' or not in [ png, jpg ].");

			// optional
			tmp = hm.remove("bounds");
			Float[] bounds = null;
			if (tmp != null) {
				bounds = ValidatorHelper.parseBounds(tmp);
				if (bounds == null)
					throw new MetadataParseException("Invalid syntax for optional field 'bounds'." +
							  "Should be latitude and longitude values in OpenLayers Bounds format - left, bottom, right, top."
							+ "Example of the full earth: -180.0,-85,180,85");
			}			

			return new MbTilesMetadata(name, description, type, version, format, bounds, hm);
		}		
	}
	
	
	// https://github.com/mapbox/mbtiles-spec/blob/master/1.0/spec.md
	public static class MetadataValidator_1_0 implements MetadataValidator {

		@Override
		public MbTilesMetadata validate(HashMap<String, String> hm) throws MetadataParseException {
			String tmp;
			
			String name = hm.remove("name");
			if (name == null)
				throw new MetadataParseException("No mandatory field 'name'.");
			
			String description = hm.remove("description");
			if (description == null)
				throw new MetadataParseException("No mandatory field 'description'.");
			
			String type = hm.remove("type");
			if (type == null || !(type.equals("overlay") || type.equals("baselayer")) )
				throw new MetadataParseException("No mandatory field 'type' or not in [ overlay, baselayer ].");

			String version; 
			tmp = hm.remove("version");
			if (tmp == null)
				throw new MetadataParseException("No mandatory field 'version'");
			try {				
				Double.parseDouble(tmp);
				version = tmp;
			} catch (NumberFormatException e) {
				throw new MetadataParseException("Invalid syntax for mandatory field 'version'. Must be a plain number.");
			}
			
			return new MbTilesMetadata(name, description, type, version, null, null, hm);
		}		
	}
	

	public static class ValidatorHelper {
		// left, bottom, right, top | Full earth: -180.0,-85,180,85
		public static Float[] parseBounds(String tmp) {
			Float[] bounds;

			if (tmp == null)
				return null;

			String[] splitted = tmp.split(",");		
			if (splitted.length != 4)
				return null;
			
			bounds = new Float[4];
			try {
				for (int i = 0; i < splitted.length; i++) {
					bounds[i] = Float.parseFloat(splitted[i]);
				}
			} catch (NumberFormatException e) {
				return null;
			}

			if (bounds[0] >= -180 && bounds[0] <= 0 && 
					bounds[2] <= 180 && bounds[2] >= 0 &&
					bounds[1] >= -85 && bounds[1] <= 0 &&
					bounds[3] <= 85 && bounds[3] >= 0
			) 	return bounds;
			else
				return null;
		}
	}
	
}
