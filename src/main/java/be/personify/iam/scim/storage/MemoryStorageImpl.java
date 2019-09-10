package be.personify.iam.scim.storage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

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
	
	
	
	@Override
	public Map<String,Object> get(String id) {
		return storage.get(id);
	}

	@Override
	public void put(String id, Map<String,Object> object) {
		storage.put(id, object);
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
		return true;
	}
	
	@Override
	public List<Map<String,Object>> search(SearchCriteria searchCriteria) {
		if ( searchCriteria == null || searchCriteria.getCriteria() == null || searchCriteria.getCriteria().size() == 0){
			return getAll();
		}
		List<Map<String,Object>> result = new ArrayList<Map<String,Object>>();
		for( Map<String,Object> object : getAll() ){
			for ( SearchCriterium criterium : searchCriteria.getCriteria() ) {
				BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(object);
				Object value = wrapper.getPropertyValue( criterium.getKey());
				if ( value.toString().equals(criterium.getValue())) {
					result.add(object);
				}
			}
		}
		return result;
	}
	
	
	
	
	@Override
	public void initialize() {
		File f = new File(Constants.tempDir,"scim.dump");
		logger.info("checking for file {}", f.getAbsolutePath());
		if ( f.exists()) {
			logger.info("{} exists, trying to read", f.getAbsolutePath());
			try {
				long start = System.currentTimeMillis();
				storage = Constants.objectMapper.readValue(f, Map.class);
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


	@Override
	public void flush() {
		Boolean flush = Boolean.valueOf(PropertyFactory.getInstance().getProperty("scim.storage.memory.flushToFile"));
		if ( flush ) {
			logger.debug("flushing");
			
			File f = new File(Constants.tempDir,"scim.dump");
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
	
	
	
	
}
