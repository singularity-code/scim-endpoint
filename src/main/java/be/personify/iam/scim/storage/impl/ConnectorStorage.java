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
package be.personify.iam.scim.storage.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import be.personify.iam.provisioning.connectors.ConnectorConnection;
import be.personify.iam.provisioning.connectors.ConnectorPool;
import be.personify.iam.scim.schema.Schema;
import be.personify.iam.scim.schema.SchemaAttribute;
import be.personify.iam.scim.storage.ConfigurationException;
import be.personify.iam.scim.storage.Storage;
import be.personify.iam.scim.util.Constants;
import be.personify.iam.scim.util.PropertyFactory;
import be.personify.util.StringUtils;
import be.personify.util.io.IOUtils;
import be.personify.util.provisioning.TargetSystem;

public abstract class ConnectorStorage implements Storage {

	private static final Logger logger = LogManager.getLogger(ConnectorStorage.class);

	protected static final String ESCAPED_DOT = "\\.";

	@Autowired
	private PropertyFactory propertyFactory;
	
	

	protected void testConnection(TargetSystem targetSystem) {
		ConnectorConnection connection = null;
		try {
			connection = ConnectorPool.getInstance().getConnectorForTargetSystem(targetSystem);
			if (connection != null) {
				connection.getConnector().ping();
				connection.close();
				logger.info("successfully tested connection");
			}
		} 
		catch (Exception e) {
			logger.error("can not test connection", e);
			throw new ConfigurationException("can not lease connection " + e.getMessage());
		}
		if (connection == null) {
			throw new ConfigurationException("can not lease connection");
		}
	}

	protected Map<String, String> createDepthMapping(Map<String, String> m) {
		Map<String, String> mm = new HashMap<String, String>();
		for (String key : m.keySet()) {
			String value = m.get(key);
			if (key.contains(StringUtils.DOT)) {
				String[] parts = key.split(ESCAPED_DOT);
				if (parts.length == 2) {
					mm.put(value, key);
				}
				else {
					throw new ConfigurationException("expression is limited to depth of 2 for mapping [" + key + "->" + value + "]");
				}
			}
		}
		return mm;
	}

	protected Map<String, Object> convertNativeMap(Map<String, Object> nativeMap, Map<String, String> mapping, Map<String, String> depthMapping, List<String> excludes, Schema schema) {
		
		Map<String, Object> scimMap = new HashMap<String, Object>();
		
		
		logger.info("nativemap {}", nativeMap);
		
		logger.info("mapping {}", mapping);
		
		for ( Entry<String,String> entry : mapping.entrySet()) {
			String value = entry.getValue();
			if (nativeMap.containsKey(value)) {
				scimMap.put(entry.getKey(), nativeMap.get(value));
			}
		}
		
//		for (String key : mapping.keySet()) {
//			if (nativeMap.containsKey(key)) {
//				scimMap.put(mapping.get(key), nativeMap.get(key));
//			}
//		}
		for (String exclude : excludes) {
			scimMap.remove(exclude);
		}
		if (depthMapping.size() > 0) {
			for (String mappingValue : depthMapping.values()) {
				String parts[] = mappingValue.split(ESCAPED_DOT);
				if (scimMap.get(parts[0]) != null) {
					((Map) scimMap.get(parts[0])).put(parts[1], scimMap.get(mappingValue));
				} else {
					Object value = scimMap.get(mappingValue); // contains dot
					if (value != null) {
						logger.debug("mappingValue {} value {} parts [0] {}", mappingValue, value, parts[0]);
						SchemaAttribute sa = schema.getAttribute(parts[0]);
						if (sa != null && sa.isMultiValued()) {
							logger.debug("its multivalued {} {} {}", mappingValue, value, value.getClass());
							List<Map> newList = new ArrayList<>();
							if (value instanceof List) {
								logger.debug("its a list {} ", value);
								for (Object o : (List) value) {
									logger.debug("object {} ", o);
									Map<String, Object> newMap = new HashMap<>();
									newMap.put(parts[1], o);
									newList.add(newMap);
								}

							} else if (value instanceof String) {
								logger.debug("its a string {} ", value);
								Map<String, Object> newMap = new HashMap<>();
								newMap.put(parts[1], value);
								newList.add(newMap);
							}
							scimMap.put(parts[0], newList);
						} else {
							Map<String, Object> mm = new HashMap<>();
							mm.put(parts[1], value);
							scimMap.put(parts[0], mm);
						}
					}
				}
				scimMap.remove(mappingValue);
			}
		}
		return scimMap;
	}

	protected Map<String, Object> processMapping(String id, Map<String, Object> scimObject,
			Map<String, Object> extraAttributes, Map<String, String> depthMapping, Schema schema) {

		scimObject.putAll(extraAttributes);
		
		logger.info("depthMapping {}", depthMapping);

		if (depthMapping.size() > 0) {

			Map<String, Object> newMap = new HashMap<String, Object>(scimObject);
			SchemaAttribute sa = null;
			Object value = null;
			for (String key : scimObject.keySet()) {
				for (String mappingValue : depthMapping.values()) {
					logger.info("mappingvalue {} {}", mappingValue, key);
					if (mappingValue.startsWith(key + StringUtils.DOT)) {
						sa = schema.getAttribute(key);
						String parts[] = mappingValue.split(ESCAPED_DOT);
						if (sa != null) {
							logger.info("schema attribute found {}", sa);
							if (sa.isMultiValued()) {
								logger.info("multiv");
								// TODO
								List<Map> list = (List) scimObject.get(key);
								List<Object> valueList = new ArrayList<Object>();
								for (Map m : list) {
									valueList.add(m.get(parts[1]));
								}
								newMap.put(mappingValue, valueList);
							} else {
								logger.info("not multiv");
								value = ((Map) scimObject.get(key)).get(parts[1]);
								newMap.put(mappingValue, value);
							}
						}
						else if ( mappingValue.startsWith("meta.")) {
							value = ((Map) scimObject.get(key)).get(parts[1]);
							newMap.put(mappingValue, value);
						}
					}
				}
			}
			return newMap;
		}
		return scimObject;
	}
	

	protected Map<String, Object> getConfigMap(String type, String connectorType) throws JsonMappingException, JsonParseException, IOException {
		logger.info("getting config for type {} connectorType {}", type, connectorType);
		String configFile = propertyFactory.getProperty("scim.storage." + connectorType + "." + type.toLowerCase() + ".configFile");
		logger.info("config file is {}", configFile);
		String fileContent = null;
		if (!StringUtils.isEmpty(configFile) ) {
			if ( configFile.startsWith("classpath:")) {
				String fName = configFile.substring("classpath:".length(), configFile.length());
				logger.info("fname : {}", fName);
				fileContent = new String(IOUtils.readFileAsBytes(PersonifyConnectorStorage.class.getResourceAsStream(fName)));
				logger.info("fileContent {}", fileContent);
			}
			else { 
				fileContent = new String(IOUtils.readFileAsBytes(new FileInputStream(new File(configFile))));
			}
		}
		else {
			String file = "/connector_" + connectorType + ".json";
			InputStream is = null;
			try {
				is = PersonifyConnectorStorage.class.getResourceAsStream(file);
				fileContent = new String(IOUtils.readFileAsBytes(is));
			}
			catch( IOException e) {
				throw new IOException("the file " + file + " can not be found for type " + type);
			}
		}

		fileContent = propertyFactory.resolvePlaceHolder(fileContent);

		logger.debug("{}", fileContent);
		return Constants.objectMapper.readValue(fileContent, Map.class);
	}
	
	
	protected Map<String,String> invertMap( Map<String,String> map ){
		Map<String, String> invertedMap = new HashMap<>();
		for(Map.Entry<String, String> entry : map.entrySet()){
		    invertedMap.put(entry.getValue(), entry.getKey());
		}
		return invertedMap;
	}
	
	
	
	
	
	
}
