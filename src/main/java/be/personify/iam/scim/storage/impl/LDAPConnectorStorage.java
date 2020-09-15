package be.personify.iam.scim.storage.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import be.mogo.provisioning.ProvisionResult;
import be.mogo.provisioning.ProvisionStatus;
import be.mogo.provisioning.ProvisionTask;
import be.mogo.provisioning.connectors.ConnectorConnection;
import be.mogo.provisioning.connectors.ConnectorPool;
import be.personify.iam.model.provisioning.TargetSystem;
import be.personify.iam.scim.schema.Schema;
import be.personify.iam.scim.schema.SchemaReader;
import be.personify.iam.scim.storage.ConfigurationException;
import be.personify.iam.scim.storage.ConstraintViolationException;
import be.personify.iam.scim.storage.DataException;
import be.personify.iam.scim.storage.SearchCriteria;
import be.personify.iam.scim.util.Constants;
import be.personify.iam.scim.util.PropertyFactory;
import be.personify.util.State;
import be.personify.util.StringUtils;
import be.personify.util.io.IOUtils;

/**
 * Storage implementation that stores data into a LDAP
 * @author vanderw
 *
 */
public class LDAPConnectorStorage extends ConnectorStorage {
	
	private static final String OBJECT_CLASS = "objectClass";
	private static final String CN = "cn=";
	


	private static final Logger logger = LogManager.getLogger(LDAPConnectorStorage.class);

	
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
			Map<String,Object> extra = new HashMap<String,Object>();
			extra.put(Constants.ID, composeDn(id));
			extra.put(OBJECT_CLASS, objectClasses);
			scimObject = processMapping( id, scimObject, extra, depthMapping, schema);
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
				Map<String,Object> scimMap = convertNativeMap(nativeMap, mapping, depthMapping, Arrays.asList(new String[] {OBJECT_CLASS}));
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
			Map<String,Object> extra = new HashMap<String,Object>();
			extra.put(Constants.ID, composeDn(id));
			extra.put(OBJECT_CLASS, objectClasses);
			scimObject = processMapping( id, scimObject, extra, depthMapping, schema);
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
		ConnectorConnection connection = null;
		try {
			connection = ConnectorPool.getInstance().getConnectorForTargetSystem(targetSystem);
			return null;//connection.getConnector().find(searchCriteria, start, count, null);
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
			Map<String,Object> config = getConfigMap();
			
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


	
	
	
	private Map<String,Object> getConfigMap() throws JsonMappingException, JsonParseException, IOException  {
		String configFile = PropertyFactory.getInstance().getProperty("scim.storage.ldap.configFile");
		String fileContent = null;
		if ( !StringUtils.isEmpty(configFile)) {
			fileContent = new String(IOUtils.readFileAsBytes(new FileInputStream(new File(configFile))));
		}
		else {
			fileContent = new String(IOUtils.readFileAsBytes(LDAPConnectorStorage.class.getResourceAsStream("/connector_ldap.json")));
		}
		
		List<String> properties = PropertyFactory.getInstance().getPropertyKeysStartingWith("scim.storage.ldap.");
		for ( String key : properties ) {
			logger.debug("key {}", key);
			String toReplace = "\\$\\{" + key + "\\}";
			if ( fileContent.contains(key)) {
				logger.debug("contains");
				fileContent = fileContent.replaceAll(toReplace, PropertyFactory.getInstance().getProperty(key));
			}
		}

		logger.debug("{}", fileContent);
		return Constants.objectMapper.readValue(fileContent, Map.class);
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
