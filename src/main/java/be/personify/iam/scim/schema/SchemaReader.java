/*
*     Copyright 2019-2022 Wouter Van der Beken @ https://personify.be
*
* Generated software by personify.be

* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*          http://www.apache.org/licenses/LICENSE-2.0
*
 * Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package be.personify.iam.scim.schema;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;

import com.fasterxml.jackson.databind.JsonNode;

import be.personify.iam.scim.util.Constants;

public class SchemaReader {

	private static final Logger logger = LogManager.getLogger(SchemaReader.class);

	private Map<String, Schema> schemaMap = new HashMap<String, Schema>();

	private Map<String, String> schemaMapper = new HashMap<String, String>();

	@Value("${scim.schema.location}")
	private String schemaLocation;

	@Autowired
	private ResourceLoader resourceLoader;

	@PostConstruct
	public void read() throws Exception {
		logger.info("using schema location {} ", schemaLocation);
		logger.info("resourceloader {}", resourceLoader);
		if (!resourceLoader.getResource(schemaLocation).exists()) {
			throw new Exception("schema " + schemaLocation + " does not exist");
		}
		JsonNode root = Constants.objectMapper.readTree(resourceLoader.getResource(schemaLocation).getInputStream());
		if (root.isArray()) {
			Iterator<JsonNode> iterator = root.elements();
			Schema schema = null;
			while (iterator.hasNext()) {
				schema = Constants.objectMapper.treeToValue(iterator.next(), Schema.class);
				logger.info("loading schema with id [" + schema.getId() + "]");
				schemaMap.put(schema.getId(), schema);
				schemaMapper.put(schema.getName(), schema.getId());
			}
		} else {
			logger.info("it's no array");
			throw new Exception("no array found in the schema");
		}
	}

	public List getSchemas() throws Exception {
		return Constants.objectMapper.readValue(resourceLoader.getResource(schemaLocation).getInputStream(),
				List.class);
	}

	/**
	 * Gets the schema from the cache
	 *
	 * @param id the id of the schema
	 * @return the schema with the given id
	 */
	public Schema getSchema(String id) {
		return schemaMap.get(id);
	}

	public Schema getSchemaByResourceType(String resourceType) {
		return getSchema(schemaMapper.get(resourceType));
	}

	/**
	 * Validates
	 *
	 * @param schema        the schema to validate
	 * @param map           the map to be validated
	 * @param checkRequired boolean indicating if the required attributes have to be
	 *                      checked
	 * @return the same map
	 * @throws SchemaException exception containing the errors
	 */
	public Map<String, Object> validate(Schema schema, Map<String, Object> map, boolean checkRequired, String operation) throws SchemaException {
		for (SchemaAttribute attribute : schema.getAttributes()) {
			validateAttribute(map.get(attribute.getName()), attribute, checkRequired, operation);
		}
		return map;
	}
	
	

	private void validateAttribute(Object o, SchemaAttribute attribute, boolean checkRequired, String operation) throws SchemaException {
		try {
			if (o == null) {
				if (attribute.isRequired() && checkRequired) {
					throw new SchemaException("attribute with name [" + attribute.getName() + "] is required");
				}
			} 
			else {
				if ( operation.equals("PUT") || operation.equals("POST")) {
					if ( attribute.getMutability().equalsIgnoreCase("readOnly")) {
						throw new SchemaException("attribute is readonly");
					}
				}
				SchemaAttributeType type = SchemaAttributeType.fromString(attribute.getType());
				if (type.equals(SchemaAttributeType.STRING)) {
					if (attribute.isMultiValued()) {
						List<String> ll = (List<String>) o;
					} else {
						String s = (String) o;
						if (attribute.getCanonicalValues() != null && attribute.getCanonicalValues().length > 0) {
							boolean found = false;
							for (String value : attribute.getCanonicalValues()) {
								if (s.equals(value)) {
									found = true;
									break;
								}
							}
							if (!found) {
								throw new SchemaException("only one of " + Arrays.toString(attribute.getCanonicalValues()) + " is allowed");
							}
						}
					}
				} 
				else if (type.equals(SchemaAttributeType.COMPLEX)) {
					if (attribute.isMultiValued()) {
						for (Map<String, Object> mm : (List<Map<String, Object>>) o) {
							validateMap(attribute, mm, checkRequired, operation);
						}
					} else {
						validateMap(attribute, (Map<String, Object>) o, checkRequired, operation);
					}
				} else if (type.equals(SchemaAttributeType.BOOLEAN)) {
					Boolean.valueOf(o.toString());
				}
			}
		}
		catch (Exception e) {
			throw new SchemaException("schema validation for attribute [" + attribute.getName() + "] with value [" + o + "] " + e.getMessage());
		}
	}
	
	

	private void validateMap(SchemaAttribute attribute, Map<String, Object> mm, boolean checkRequired, String operation) throws SchemaException {
		for (String k : mm.keySet()) {
			boolean keyFoundInSchema = false;
			for (SchemaAttribute subAttribute : attribute.getSubAttributes()) {
				if (k.equals(subAttribute.getName())) {
					validateAttribute(mm.get(k), subAttribute, checkRequired, operation);
					keyFoundInSchema = true;
				}
			}
			if (!keyFoundInSchema) {
				throw new SchemaException("unsupported attribute name [" + k + "]");
			}
		}
	}
}
