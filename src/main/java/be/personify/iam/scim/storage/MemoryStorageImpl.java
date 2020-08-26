package be.personify.iam.scim.storage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.StringUtils;

import be.personify.iam.scim.util.Constants;
import be.personify.iam.scim.util.PropertyFactory;

/**
 * Sample storage implementation that stores data into a volatile memory store
 * @author vanderw
 *
 */
public class MemoryStorageImpl implements Storage {
	
	private static final Logger logger = LogManager.getLogger(MemoryStorageImpl.class);

	private Map<String,Map<String,Object>> storage = null;
	
	private List<String> uniqueConstraintsList = new ArrayList<String>();
	private Map<String,Map<Object,Object>> uniqueConstraints = null;
	
	private String type;
	
	
	
	@Override
	public Map<String,Object> get(String id) {
		return storage.get(id);
	}
	
	@Override
	public Map<String,Object> get(String id, String version) {
		throw new RuntimeException("versioning not implemented");
	}
	
	@Override
	public List<String> getVersions(String id) {
		throw new RuntimeException("versioning not implemented");
	}
	

	@Override
	public void put(String id, final Map<String,Object> object) throws ConstraintViolationException {
		checkConstraints(id, object);
		storage.put(id, object);
		updateConstraints(id,object);
	}
	
	

	

	@Override
	public List<Map<String,Object>> getAll() {
		return new ArrayList<Map<String,Object>>(storage.values());
	}
	
	@Override
	public boolean delete(String id) {
		if (storage.remove(id) == null ) {
			return false;
		}
		removeConstraints(id);
		return true;
	}
	
	@Override
	public List<Map<String,Object>> search(SearchCriteria searchCriteria, String sortBy, String sortOrder) {
		if ( searchCriteria == null || searchCriteria.getCriteria() == null || searchCriteria.getCriteria().size() == 0){
			return getAll();
		}
		logger.info("searchCriteria {}", searchCriteria);
		List<Map<String,Object>> result = new ArrayList<Map<String,Object>>();
		for( Map<String,Object> object : getAll() ){
			int count = 0;
			for ( SearchCriterium criterium : searchCriteria.getCriteria() ) {
				Object value = object.get( criterium.getKey());
				if ( criterium.getSearchOperation() == SearchOperation.EQUALS) {
					if ( value.toString().equals(criterium.getValue())) {
						count++;
					}
				}
				else if ( criterium.getSearchOperation() == SearchOperation.NOT_EQUALS) {
					if ( !value.toString().equals(criterium.getValue())) {
						count++;
					}
				}
				else if ( criterium.getSearchOperation() == SearchOperation.STARTS_WITH) {
					if ( value.toString().startsWith((String)criterium.getValue())) {
						count++;
					}
				}
				else if ( criterium.getSearchOperation() == SearchOperation.CONTAINS) {
					if ( value.toString().contains((String)criterium.getValue())) {
						count++;
					}
				}
				else if ( criterium.getSearchOperation() == SearchOperation.ENDS_WITH) {
					if ( value.toString().endsWith((String)criterium.getValue())) {
						count++;
					}
				}
			}
			if ( count == searchCriteria.getCriteria().size()) {
				result.add(object);
			}
		}
		return result;
	}
	
	
	
	
	@Override
	public void initialize(String type) {
		this.type = type;
		File f = getStorageFile();
		initializeUniqueConstraints(type);
		logger.info("checking for file {}", f.getAbsolutePath());
		if ( f.exists()) {
			logger.info("{} exists, trying to read", f.getAbsolutePath());
			try {
				long start = System.currentTimeMillis();
				storage = Constants.objectMapper.readValue(f, Map.class);
				buildConstraints(storage);
				logger.info("{} read in {} ms", f.getAbsolutePath(), ( System.currentTimeMillis() - start));
			}
			catch (IOException e) {
				logger.error("can not read file {}", f.getAbsolutePath(), e);
				storage = null;
			}
		}
		else {
			storage = new HashMap<String, Map<String,Object>>();
		}
	}

	

	private void initializeUniqueConstraints(String type) {
		String uniqueConstraintsString = PropertyFactory.getInstance().getProperty("scim.storage.memory." + type.toLowerCase() + ".unique");
		uniqueConstraints = new HashMap<String, Map<Object,Object>>();
		if ( !StringUtils.isEmpty(uniqueConstraintsString)) {
			uniqueConstraintsList = Arrays.asList(uniqueConstraintsString.split(Constants.COMMA));
			for ( String u : uniqueConstraintsList ) {
				uniqueConstraints.put(u, new HashMap<Object, Object>());
			}
		}
	}
	
	
	private void buildConstraints(Map<String, Map<String, Object>> map) {
		for ( String key : map.keySet()) {
			Map<String,Object> entity = map.get(key);
			Object id = entity.get("id");
			for ( String constraint : uniqueConstraintsList) {
				Object o = entity.get(constraint);
				if ( o != null ) {
					uniqueConstraints.get(constraint).put( o, id);
				}
			}
		}
	}
	
	private void checkConstraints(String id, Map<String, Object> object) throws ConstraintViolationException {
		for ( String constraint : uniqueConstraintsList) {
			Object valueFromEntity = object.get(constraint);
			Object valueFromConstraintCache = uniqueConstraints.get(constraint).get(valueFromEntity);
			if ( valueFromConstraintCache != null ){
				if ( !valueFromConstraintCache.equals(id) ) {
					throw new ConstraintViolationException("the value " + valueFromEntity + " is already existing for the attribute " + constraint);
				}
			}
		}
	}
	
	
	private void updateConstraints(String id, Map<String, Object> object) {
		for ( String constraint : uniqueConstraintsList) {
			Object valueFromEntity = object.get(constraint);
			if ( valueFromEntity != null ) {
				uniqueConstraints.get(constraint).put(valueFromEntity, id);
			}
		}
	}
	
	private void removeConstraints(String id) {
		for ( String constraint : uniqueConstraintsList) {
			 Map<Object,Object> c = uniqueConstraints.get(constraint);
			 if ( c.containsValue(id)) {
				 Map<Object,Object> newMap = new HashMap<Object, Object>();
				 for (Object o : c.keySet()) {
					 if ( !c.get(o).equals(id)) {
						 newMap.put(o, c.get(o));
					 }
				 }
				 uniqueConstraints.put(constraint, newMap);
			 }
			
		}
	}


	@Override
	public void flush() {
		Boolean flush = Boolean.valueOf(PropertyFactory.getInstance().getProperty("scim.storage.memory.flushToFile"));
		if ( flush ) {
			logger.debug("flushing");
			
			File f = getStorageFile();
			logger.debug("saving to file {}", f.getAbsolutePath());
			try {
				long start = System.currentTimeMillis();
				synchronized (storage) {
					Constants.objectMapper.writeValue(f, storage);
				}
				logger.debug("{} saved in {} ms", f.getAbsolutePath(), ( System.currentTimeMillis() - start));
			} 
			catch (IOException e) {
				logger.error("can not flush", e);
			}
		}
		else {
			logger.info("flush not configured");
		}	
	}

	@Override
	public boolean deleteAll() {
		storage.clear();
		flush();
		return true;
	}
	
	
	
	private File getStorageFile() {
		String dir = PropertyFactory.getInstance().getProperty("scim.storage.memory.flushToFileDirectory");
		if ( dir != null) {
			File directory = new File(dir);
			if ( directory.exists() && directory.isDirectory()) {
				return new File(directory,"personify-scim-" + type.toLowerCase() + ".dump");
			}
		}
		return new File(Constants.tempDir,"personify-scim-" + type.toLowerCase() + ".dump");
	}

	
	
	
	
	
}
