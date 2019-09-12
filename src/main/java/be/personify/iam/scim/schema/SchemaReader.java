package be.personify.iam.scim.schema;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import be.personify.iam.scim.util.Constants;

public class SchemaReader {
	
	private static final Logger logger = LoggerFactory.getLogger(SchemaReader.class);
	
	private Map<String,Schema> schemaMap = new HashMap<String, Schema>();
	
	private static SchemaReader _instance = null;
	
		
	/**
	 * Creates an instance if not yet present
	 * @return
	 */
	public static synchronized SchemaReader getInstance() {
		if ( _instance == null ) {
			_instance = new SchemaReader();
			try {
				_instance.read();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				_instance = null;
			}
		}
		return _instance;
	}
	
	
	private void read() throws Exception {
		JsonNode root = Constants.objectMapper.readTree(SchemaReader.class.getResourceAsStream("/disc_schemas.json"));
		if ( root.isArray() ) {
			Iterator<JsonNode> iterator = root.elements();
			Schema schema = null;
			while ( iterator.hasNext() ) {
				schema = Constants.objectMapper.treeToValue(iterator.next(), Schema.class);
				logger.info("storing schema with id [" + schema.getId() + "]");
				schemaMap.put(schema.getId(), schema);
			}
		}
		else {
			logger.info("it's no array");
			throw new Exception("no array found in the schema");
		}
	}
	
	
	/**
	 * Gets the schema from the cache
	 * @param id
	 * @return
	 */
	public Schema getSchema( String id) {
		return schemaMap.get(id);
	}
	
	
	public Schema getSchemaByType( String type) {
		if ( type.equals(Constants.RESOURCE_TYPE_USER)) {
			return getSchema(Constants.SCHEMA_USER);
		}
		else if ( type.equals(Constants.RESOURCE_TYPE_GROUP)) {
			return getSchema(Constants.SCHEMA_GROUP);
		}
		return null;
	}
	
	
	
	/**
	 * Validates the object against the schema
	 * @param schemaId
	 * @param map
	 * @return
	 * @throws SchemaException
	 */
	public Map<String,Object> validate( String schemaId, Map<String,Object> map, boolean checkRequired ) throws SchemaException {
		Schema schema = getSchema(schemaId);
		if ( schema != null ) {
			for ( SchemaAttribute attribute : schema.getAttributes() ) {
				validateAttribute( map.get(attribute.getName()), attribute, checkRequired);
			}
			return map;
		}
		else {
			throw new SchemaException("schema with id " + schemaId + " not found");
		}
	}


	
	
	
	private void validateAttribute(Object o, SchemaAttribute attribute, boolean checkRequired) throws SchemaException {
		try {
			if ( o == null ) {
				if ( attribute.isRequired() && checkRequired ) {
					throw new SchemaException("attribute with name [" + attribute.getName() + "] is required");
				}
			}
			else {
				SchemaAttributeType type = SchemaAttributeType.fromString(attribute.getType());
				if ( type.equals(SchemaAttributeType.STRING)) {
					if ( attribute.isMultiValued()) {
						List<String> ll = (List<String>)o;
					}
					else {
						String s = (String)o;
						if ( attribute.getCanonicalValues() != null && attribute.getCanonicalValues().length > 0) {
							boolean found = false;
							for ( String value : attribute.getCanonicalValues() ) {
								if ( s.equals(value)) {
									found = true;
									break;
								}
							}
							if ( !found ) {
								throw new SchemaException("only one of " + Arrays.toString(attribute.getCanonicalValues()) + " is allowed" );
							}
						}
					}
				}
				else if ( type.equals(SchemaAttributeType.COMPLEX) ) {
					if ( attribute.isMultiValued()) {
						for ( Map<String,Object> mm : (List<Map<String,Object>>)o) {
							validateMap(attribute, mm,checkRequired);
						}
					}
					else {
						validateMap(attribute, (Map<String,Object>)o, checkRequired);
					}
				}
				else if ( type.equals(SchemaAttributeType.BOOLEAN) ) {
					Boolean.valueOf(o.toString());
				}
			}
		}
		catch (Exception e ) {
			throw new SchemaException("schema validation for attribute [" + attribute.getName() + "] with value [" + o + "] " + e.getMessage());
		}
		
		
	}


	private void validateMap(SchemaAttribute attribute, Map<String, Object> mm, boolean checkRequired) throws SchemaException {
		for ( String k : mm.keySet() ) {
			boolean keyFoundInSchema = false;
			for ( SchemaAttribute subAttribute : attribute.getSubAttributes()) {
				if ( k.equals(subAttribute.getName())){
					validateAttribute(mm.get(k), subAttribute, checkRequired);
					keyFoundInSchema = true;
				}
			}
			if ( !keyFoundInSchema ) {
				throw new SchemaException("unsupported attribute name [" + k + "]");
			}
		}
	}
	
	

}
