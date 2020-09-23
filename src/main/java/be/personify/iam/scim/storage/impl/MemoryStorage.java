package be.personify.iam.scim.storage.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import be.personify.iam.scim.storage.ConstraintViolationException;
import be.personify.iam.scim.storage.SortOrder;
import be.personify.iam.scim.storage.Storage;
import be.personify.iam.scim.util.Constants;
import be.personify.iam.scim.util.PropertyFactory;
import be.personify.util.SearchCriteria;
import be.personify.util.SearchCriterium;
import be.personify.util.SearchOperation;
import be.personify.util.StringUtils;

/**
 * Sample storage implementation that stores data into a volatile memory store
 * @author vanderw
 *
 */
public class MemoryStorage implements Storage {
	
	@Autowired
	private PropertyFactory propertyFactory;
	
	private static final Logger logger = LogManager.getLogger(MemoryStorage.class);

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
	public void create(String id, final Map<String,Object> object) throws ConstraintViolationException {
		checkConstraints(id, object);
		storage.put(id, object);
		updateConstraints(id,object);
	}
	
	
	@Override
	public void update(String id, final Map<String,Object> object) throws ConstraintViolationException {
		checkConstraints(id, object);
		storage.put(id, object);
		updateConstraints(id,object);
	}
	
	

	
	@Override
	public boolean delete(String id) {
		boolean removed = false;
		synchronized (storage) {
			 removed = storage.remove(id) == null ? false : true;
		}
		removeConstraints(id);
		return removed;
	}
	
	
	
	@Override
	public List<Map<String,Object>> search(SearchCriteria searchCriteria, int start, int count, String sortBy, String sortOrderString) {
		List<Map<String,Object>> result = null;
		if ( searchCriteria == null || searchCriteria.getCriteria() == null || searchCriteria.getCriteria().size() == 0){
			result = new ArrayList<Map<String,Object>>(storage.values());
		}
		else {
			logger.debug("{}", searchCriteria);
			result = filterOnSearchCriteria(searchCriteria);
		}
		
		if ( StringUtils.isEmpty(sortOrderString)) {
			sortOrderString = SortOrder.ascending.name();
			logger.debug("defaulting to sortorder {}", sortOrderString);
		}
		SortOrder sortOrder = SortOrder.valueOf(sortOrderString);
		
		result = sort( result, sortBy, sortOrder);
		
		count = count > result.size() ? result.size() : count;
		logger.debug("count {} start {}", count, start);
		int newStart = (start -1) * count;
		List<Map<String,Object>> sublist = result.subList(newStart , newStart + count);
		
		return sublist;
	}
	
	
	@Override
	public long count(SearchCriteria searchCriteria) {
		return Long.valueOf(filterOnSearchCriteria(searchCriteria).size());
	}

	
	
	private List<Map<String, Object>> filterOnSearchCriteria(SearchCriteria searchCriteria) {
		List<Map<String, Object>> result;
		result = new ArrayList<Map<String,Object>>();
		for( Map<String,Object> object : storage.values()){
			int criteriaCount = 0;
			for ( SearchCriterium criterium : searchCriteria.getCriteria() ) {
				
				Object value = getRecursiveObject(object, criterium.getKey());
				if ( criterium.getSearchOperation() == SearchOperation.EQUALS) {
					
					boolean m = matchValue( value, criterium.getValue());
					if (m) {
						criteriaCount++;
					}
				}
				else if ( criterium.getSearchOperation() == SearchOperation.NOT_EQUALS) {
					if ( !value.toString().equals(criterium.getValue())) {
						criteriaCount++;
					}
				}
				else if ( criterium.getSearchOperation() == SearchOperation.STARTS_WITH) {
					if ( value.toString().startsWith((String)criterium.getValue())) {
						criteriaCount++;
					}
				}
				else if ( criterium.getSearchOperation() == SearchOperation.CONTAINS) {
					if ( value.toString().contains((String)criterium.getValue())) {
						criteriaCount++;
					}
				}
				else if ( criterium.getSearchOperation() == SearchOperation.ENDS_WITH) {
					if ( value.toString().endsWith((String)criterium.getValue())) {
						criteriaCount++;
					}
				}
				else if ( criterium.getSearchOperation() == SearchOperation.PRESENT) {
					if ( value != null) {
						criteriaCount++;
					}
				}
			}
			if ( criteriaCount == searchCriteria.getCriteria().size()) {
				result.add(object);
			}
		}
		return result;
	}
	
	
	
	
	
	
	
	private boolean matchValue(Object value, Object criteriumValue) {
		if ( value instanceof List ) {
			List l = (List)value;
			for ( Object o : l  ) {
				if ( o.equals(criteriumValue)) {
					return true;
				}
			}
		}
		else {
			return value.equals(criteriumValue);
		}
		return false;
	}

	private static Object getRecursiveObject(Map<String, Object> object, String key) {
		System.out.println(" object " + object + " " + key);
		if ( key.contains(StringUtils.DOT)) {
			Object o = null;
			int index = 0;
			while ( (index = key.indexOf(StringUtils.DOT)) > 0 ) {
				String part = key.substring(0, index);
				if ( o == null ) {
					o = object.get(part);
				}
				else {
					
				}
				key = key.substring(index, key.length());
			}
			key = key.substring(1, key.length());
			if ( o instanceof List ) {
				List<Object> valueList = new ArrayList<>();
				List<Map> mapList = (List)o;
				for ( Map m : mapList ) {
					System.out.println(" key " + key);
					valueList.add(m.get(key));
				}
				return valueList;
			}
			else if ( o instanceof Map ) {
				Map map = (Map)o;
				return map.get(key);
			}
			return o;
		}
		return object.get(key);
	}
	
	
	
	
//	public static void main(String[] args) {
//		Map<String,Object> m = new HashMap<>();
//		
//		
//		Map<String,Object> v1 = new HashMap<>();
//		v1.put("type", "home");
//		v1.put("mail", "mail1");
//		
//		List<Map> list = new ArrayList<>();
//		list.add(v1);
//		
//		m.put("emails", list);
//		
//		System.out.println(getRecursiveObject(m, "emails.mail"));
//	}
	
	
	public static void main(String[] args) {
		Map<String,Object> m = new HashMap<>();
		
		
		Map<String,Object> v1 = new HashMap<>();
		v1.put("familyName", "Simpson");
		
		
		
		m.put("name", v1);
		
		System.out.println(getRecursiveObject(m, "name.familyName"));
	}
	

	private List<Map<String, Object>> sort(List<Map<String, Object>> result, String sortBy, SortOrder sortOrder) {
		
		if ( !StringUtils.isEmpty(sortBy)) {
			
			Collections.sort(result, new Comparator<Map<String, Object>>() {

				@Override
				public int compare(Map<String, Object> arg0, Map<String, Object> arg1) {
					
					Object value0 = arg0.get(sortBy);
					Object value1 = arg1.get(sortBy);
					
					if ( value0 instanceof String && value1 instanceof String) {
						
						if ( sortOrder == SortOrder.ascending) {
							return ((String)value0).compareTo(((String)value1));
						}
						else {
							return ((String)value1).compareTo(((String)value0));
						}
					}
					
					return 0;
				}
			});
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
		String uniqueConstraintsString = propertyFactory.getProperty("scim.storage.memory." + type.toLowerCase() + ".unique");
		uniqueConstraints = new HashMap<String, Map<Object,Object>>();
		if ( !StringUtils.isEmpty(uniqueConstraintsString)) {
			uniqueConstraintsList = Arrays.asList(uniqueConstraintsString.split(StringUtils.COMMA));
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
		synchronized (uniqueConstraints) {
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
	}
	
	
	private void updateConstraints(String id, Map<String, Object> object) {
		synchronized (uniqueConstraints) {
			for ( String constraint : uniqueConstraintsList) {
				Object valueFromEntity = object.get(constraint);
				if ( valueFromEntity != null ) {
					uniqueConstraints.get(constraint).put(valueFromEntity, id);
				}
			}
		}
	}
	
	private void removeConstraints(String id) {
		for ( String constraint : uniqueConstraintsList) {
			if (uniqueConstraints.get(constraint).containsValue(id) ) {
				synchronized (uniqueConstraints) { 
					uniqueConstraints.get(constraint).values().remove(id);
				}
			}
		}
	}


	@Override
	public synchronized void flush() {
		Boolean flush = Boolean.valueOf(propertyFactory.getProperty("scim.storage.flush"));
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
		String dir = propertyFactory.getProperty("scim.storage.memory.flushToFileDirectory");
		if ( dir != null) {
			File directory = new File(dir);
			if ( directory.exists() && directory.isDirectory()) {
				return new File(directory,"personify-scim-" + type.toLowerCase() + ".dump");
			}
		}
		return new File(Constants.tempDir,"personify-scim-" + type.toLowerCase() + ".dump");
	}

	
	
	
	
	
}
