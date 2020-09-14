package be.personify.iam.scim.storage.impl;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import be.mogo.provisioning.ProvisionResult;
import be.mogo.provisioning.ProvisionStatus;
import be.mogo.provisioning.ProvisionTask;
import be.mogo.provisioning.connectors.ConnectorConnection;
import be.mogo.provisioning.connectors.ConnectorPool;
import be.personify.iam.model.provisioning.TargetSystem;
import be.personify.iam.scim.schema.Schema;
import be.personify.iam.scim.schema.SchemaAttribute;
import be.personify.iam.scim.schema.SchemaReader;
import be.personify.iam.scim.storage.ConfigurationException;
import be.personify.iam.scim.storage.ConstraintViolationException;
import be.personify.iam.scim.storage.DataException;
import be.personify.iam.scim.storage.SearchCriteria;
import be.personify.iam.scim.util.Constants;
import be.personify.util.State;
import be.personify.util.StringUtils;

/**
 * Sample storage implementation that stores data into a volatile memory store
 * @author vanderw
 *
 */
public class MogoConnectorStorage extends MogoStorage {
	
	private static final String OBJECT_CLASS = "objectClass";
	private static final String CN = "cn=";
	


	private static final Logger logger = LogManager.getLogger(MogoConnectorStorage.class);

	
	private String basedn = null;
	
	private static TargetSystem targetSystem = null;
	
	private static Map<String,String> mapping;
	private static Map<String,String> depthMapping;
	
	private List<String> objectClasses = null;
	
	private Schema schema = null;
	private List<String> schemaList = null;

	
	
	
	@Override
	public void create(String id, Map<String,Object> scimObject) throws ConstraintViolationException, DataException {
		
		try {
			scimObject = processMapping( id, scimObject);
			ProvisionResult result = new ProvisionTask().provision(State.PRESENT, scimObject, mapping, targetSystem);
			if ( !result.getStatus().equals(ProvisionStatus.SUCCESS)) {
				throw new DataException(result.getErrorCode() + Constants.SPACE + result.getErrorDetail());
			}
		}
		catch (Exception e) {
			throw new DataException(e.getMessage());
		}
		
	}
	
	
	@Override
	public Map<String,Object> get(String id) {
		
		ConnectorConnection connection = null;
		try {
			connection = ConnectorPool.getInstance().getConnectorForTargetSystem(targetSystem);
			Map<String,Object> nativeMap = connection.getConnector().find(composeDn(id));
			if ( nativeMap != null ) {
				Map<String,Object> scimMap = convertNativeMap(nativeMap);
				scimMap.put(Constants.KEY_SCHEMAS, schemaList);
				scimMap.put(Constants.ID, id);
				return scimMap;
			}
			return null;
		}
		catch (Exception e) {
			throw new DataException(e.getMessage());
		}
		finally {
			if ( connection != null ) {
				connection.close();
			}
		}
	}

	

	
	
	@Override
	public void update(String id, Map<String,Object> scimObject) throws ConstraintViolationException {
		
		try {
			scimObject = processMapping( id, scimObject);
			ProvisionResult result = new ProvisionTask().provision(State.PRESENT, scimObject, mapping, targetSystem);
			if ( !result.getStatus().equals(ProvisionStatus.SUCCESS)) {
				throw new DataException(result.getErrorCode() + Constants.SPACE + result.getErrorDetail());
			}
		}
		catch (Exception e) {
			throw new DataException(e.getMessage());
		}
		
	}
	
	

	
	@Override
	public boolean delete(String id) {
	
		ConnectorConnection connection = null;
		try {
			connection = ConnectorPool.getInstance().getConnectorForTargetSystem(targetSystem);
			return connection.getConnector().delete(composeDn(id));
		}
		catch (Exception e) {
			throw new DataException(e.getMessage());
		}
		finally {
			if ( connection != null ) {
				connection.close();
			}
		}
	}
	
	
	
	
	@Override
	public List<Map<String,Object>> search(SearchCriteria searchCriteria, int start, int count, String sortBy, String sortOrderString) {
		return null;
	}
	
	
	@Override
	public long count(SearchCriteria searchCriteria) {
		return Long.valueOf(filterOnSearchCriteria(searchCriteria).size());
	}

	
	
	private List<Map<String, Object>> filterOnSearchCriteria(SearchCriteria searchCriteria) {
		return null;
	}
	
	
	

	private String composeDn(String id) {
		return CN + id + Constants.COMMA + basedn;
	}
	
	

	@Override
	public void initialize(String type) {
		try {
			Map<String,Object> config = Constants.objectMapper.readValue(new File("src/main/resources/mogo_connector_ldap.json"), Map.class);
			final String targetSystemJson = Constants.objectMapper.writeValueAsString(config.get("targetSystem"));
			targetSystem = Constants.objectMapper.readValue(targetSystemJson, TargetSystem.class);

			//add type to basedn
			basedn = targetSystem.getConnectorConfiguration().getConfiguration().get("baseDn");
			basedn = "ou=" + type.toLowerCase() + Constants.COMMA + basedn;
			targetSystem.getConnectorConfiguration().getConfiguration().put("baseDn", basedn);
			
			
			mapping = (Map)config.get("mapping");
			if ( mapping == null || targetSystem == null) {
				throw new ConfigurationException("can not find mapping or targetSystem in configuration");
			}
			else {
				objectClasses = Arrays.asList(targetSystem.getConnectorConfiguration().getConfiguration().get(type.toLowerCase() + "ObjectClasses").split(Constants.COMMA));
				schema = SchemaReader.getInstance().getSchemaByResourceType(type);
				schemaList = Arrays.asList(new String[] {schema.getId()});
				depthMapping = createDepthMapping(mapping);
				testConnection(targetSystem);
			}
		}
		catch(Exception e) {
			logger.error("can not read/validate configuration for type {}", type, e);
			throw new ConfigurationException(e.getMessage());
		}
	}

	

	


	
	
	
	
	private Map<String, Object> convertNativeMap(Map<String, Object> nativeMap) {
		Map<String,Object> scimMap = new HashMap<String,Object>();
		for ( String key : mapping.keySet()) {
			if ( nativeMap.containsKey(key)) {
				scimMap.put(mapping.get(key), nativeMap.get(key));
			}
		}
		scimMap.remove(OBJECT_CLASS);
		if ( depthMapping.size() > 0 ) {
			for ( String mappingValue : depthMapping.values()) {
				String parts[] = mappingValue.split(ESCAPED_DOT);
				if ( scimMap.get(parts[0]) != null ) {
					((Map)scimMap.get(parts[0])).put(parts[1], scimMap.get(mappingValue));
				}
				else {
					Map<String,Object> mm = new HashMap<>();
					mm.put(parts[1], scimMap.get(mappingValue));
					scimMap.put(parts[0], mm);
				}
				scimMap.remove(mappingValue);
			}
		}
		return scimMap;
	}
	
	
	
	
	private Map<String, Object> processMapping(String id, Map<String, Object> scimObject) {
		scimObject.put(Constants.ID, composeDn(id));
		scimObject.put(OBJECT_CLASS, objectClasses);
		
		if ( depthMapping.size() > 0 ) {
			
			Map<String, Object> newMap = new HashMap<String,Object>(scimObject);
			SchemaAttribute sa = null;
			Object value = null;
			for ( String key : scimObject.keySet() ) {
				for ( String mappingValue : depthMapping.values()) {
					if ( mappingValue.startsWith(key + StringUtils.DOT)) {
						sa = schema.getAttribute(key);
						if ( sa != null) {
							String parts[] = mappingValue.split(ESCAPED_DOT);
							if ( sa.isMultiValued()) {
								//TODO
							}
							else {
								value = ((Map)scimObject.get(key)).get(parts[1]);
								newMap.put(mappingValue, value);
								sa.getSubAttribute(parts[1]);
							}
						}
					}
				}
			}
			return newMap;
		}
		return scimObject;
	}

	

	@Override
	public synchronized void flush() {
	}
	
	

	@Override
	public boolean deleteAll() {
		return false;
	}
	
	
	@Override
	public Map<String,Object> get(String id, String version) {
		throw new RuntimeException("versioning not implemented");
	}
	
	@Override
	public List<String> getVersions(String id) {
		throw new RuntimeException("versioning not implemented");
	}
	
	
	
	
	
	
}
