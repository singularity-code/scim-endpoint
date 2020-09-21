package be.personify.iam.scim.storage.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import be.personify.iam.model.provisioning.TargetSystem;
import be.personify.iam.provisioning.ProvisionResult;
import be.personify.iam.provisioning.ProvisionStatus;
import be.personify.iam.provisioning.ProvisionTask;
import be.personify.iam.provisioning.connectors.ConnectorConnection;
import be.personify.iam.provisioning.connectors.ConnectorPool;
import be.personify.iam.scim.schema.Schema;
import be.personify.iam.scim.schema.SchemaReader;
import be.personify.iam.scim.storage.ConfigurationException;
import be.personify.iam.scim.storage.ConstraintViolationException;
import be.personify.iam.scim.storage.DataException;
import be.personify.iam.scim.util.Constants;
import be.personify.util.MapUtils;
import be.personify.util.SearchCriteria;
import be.personify.util.SearchCriterium;
import be.personify.util.State;

/** * Storage implementation that stores data into a LDAP
 * @author vanderw
 *
 */
public class DatabaseConnectorStorage extends ConnectorStorage {
	



	private static final Logger logger = LogManager.getLogger(DatabaseConnectorStorage.class);


	private static TargetSystem targetSystem = null;
	
	private static Map<String,String> mapping;
	private static Map<String,String> depthMapping;
	
		
	private Schema schema = null;
	private List<String> schemaList = null;

	
	
	
	@Override
	public void create(String id, Map<String,Object> scimObject) throws ConstraintViolationException, DataException {
		
		try {
			Map<String,Object> extra = new HashMap<String,Object>();
			extra.put(Constants.ID, id);
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
			Map<String,Object> nativeMap = connection.getConnector().find(id);
			if ( nativeMap != null ) {
				Map<String,Object> scimMap = convertNativeMap(nativeMap, mapping, depthMapping, Arrays.asList(new String[] {}), schema);
				scimMap.put(Constants.KEY_SCHEMAS, schemaList);
				scimMap.put(Constants.ID, id);
				return scimMap;
			}
			return null;
		}
		catch (Exception e) {
			e.printStackTrace();
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
			extra.put(Constants.ID, id);
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
			return connection.getConnector().delete(id);
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
			
			SearchCriteria nativeSearchCriteria = getNativeSearchCriteria(searchCriteria);
			
			connection = ConnectorPool.getInstance().getConnectorForTargetSystem(targetSystem);
			List<Map<String,Object>> nativeList = connection.getConnector().find(nativeSearchCriteria, start, count, null);
			List<Map<String,Object>> scimList = new ArrayList<>();
			for ( Map<String,Object> nativeMap : nativeList ) {
				Map<String,Object> scimMap = convertNativeMap(nativeMap, mapping, depthMapping, Arrays.asList(new String[] {}), schema);
				scimMap.put(Constants.KEY_SCHEMAS, schemaList);
				scimMap.put(Constants.ID, scimMap.get(Constants.ID));
				scimList.add(scimMap);
			}
			return scimList;
		}
		catch (Exception e) {
			e.printStackTrace();
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
		ConnectorConnection connection = null;
		try {
			
			SearchCriteria nativeSearchCriteria = getNativeSearchCriteria(searchCriteria);
			
			connection = ConnectorPool.getInstance().getConnectorForTargetSystem(targetSystem);
			List<String> nativeList = connection.getConnector().findIds(nativeSearchCriteria, 0, 0, null);
			return Long.valueOf(nativeList.size());
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new DataException(e.getMessage());
		}
		finally {
			if ( connection != null ) {
				connection.close();
			}
		}
		
		
		
	}


	private SearchCriteria getNativeSearchCriteria(SearchCriteria searchCriteria) {
		SearchCriteria nativeSearchCriteria = new SearchCriteria();
		for ( SearchCriterium criterium : searchCriteria.getCriteria()) {
			String nativeKey = (String)MapUtils.getKeyByValue(mapping, criterium.getKey());
			nativeSearchCriteria.getCriteria().add(new SearchCriterium(nativeKey, criterium.getValue(), criterium.getSearchOperation()));
		}
		return nativeSearchCriteria;
	}

	
	
	

	@Override
	public void initialize(String type) {
		try {
			Map<String,Object> config = getConfigMap("database");
			
			final String targetSystemJson = Constants.objectMapper.writeValueAsString(config.get("targetSystem"));
			targetSystem = Constants.objectMapper.readValue(targetSystemJson, TargetSystem.class);

			
			mapping = (Map)config.get("mapping");
			if ( mapping == null || targetSystem == null) {
				throw new ConfigurationException("can not find mapping or targetSystem in configuration");
			}
			else {
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
