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
import be.personify.iam.scim.storage.DataException;
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
 * 
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
		synchronized(storage){
			storage.put(id, object);
		}
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
	public List<Map> search(SearchCriteria searchCriteria, int start, int count, String sortBy, String sortOrderString ) {
		return search(searchCriteria, start, count, sortBy, sortOrderString, null);
	}
	
	
	@Override
	public List<Map> search(SearchCriteria searchCriteria, int start, int count, String sortBy, String sortOrderString, List<String> includeAttributes ) {
		List<Map> result = null;
		if ( searchCriteria == null || searchCriteria.getCriteria() == null || searchCriteria.getCriteria().size() == 0){
			result = new ArrayList<Map>(storage.values());
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
		List<Map> sublist = result.subList(newStart , newStart + count);
		
		return sublist;
	}
	
	
	
	@Override
	public long count(SearchCriteria searchCriteria) {
		long totalCount = 0;
		
		//first check constraints
		int count = 0;
		List<String> criteriaFoundInConstraints = new ArrayList<String>();
		for ( SearchCriterium criterium : searchCriteria.getCriteria() ) {
			Map m = uniqueConstraints.get(criterium.getKey());
			if ( m != null ) {
				String id = (String)m.get(criterium.getValue());
				//logger.info("found id in unique {} for value {}", id, criterium.getValue());
				if ( id != null && !criteriaFoundInConstraints.contains(id)) {
					//logger.info("putting it in map {}", id);
					criteriaFoundInConstraints.add(id);
				}
				count++;
			}
		}
			
		if ( count < searchCriteria.size() ) {
			logger.info("not all criteria are found in contraints");
			for( Map<String,Object> object : storage.values()){
				int criteriaCount = 0;
				for ( SearchCriterium criterium : searchCriteria.getCriteria() ) {
					Object value = getRecursiveObject(object, criterium.getKey());
					if ( matchValue( value, criterium)) {
						criteriaCount++;
					}
				}
				if ( criteriaCount == searchCriteria.getCriteria().size()) {
					totalCount++;
				}
			}
		}
		else {
			totalCount = criteriaFoundInConstraints.size();
		}
				
		return totalCount;
	}

	
	
	private List<Map> filterOnSearchCriteria(SearchCriteria searchCriteria) {
		List<Map> result = new ArrayList<Map>();
		
		//first check constraints
		int count = 0;
		List<String> criteriaFoundInConstraints = new ArrayList<String>();
		for ( SearchCriterium criterium : searchCriteria.getCriteria() ) {
			Map m = uniqueConstraints.get(criterium.getKey());
			if ( m != null ) {
				String id = (String)m.get(criterium.getValue());
				//logger.info("found id in unique {} for value {}", id, criterium.getValue());
				if ( id != null && !criteriaFoundInConstraints.contains(id)) {
					//logger.info("putting it in map {}", id);
					criteriaFoundInConstraints.add(id);
				}
				count++;
			}
		}
		
		if ( count < searchCriteria.size() ) {
			logger.info("not all criteria are found in contraints");
			for( Map<String,Object> object : storage.values()){
				int criteriaCount = 0;
				for ( SearchCriterium criterium : searchCriteria.getCriteria() ) {
					Object value = getRecursiveObject(object, criterium.getKey());
					if ( matchValue( value, criterium)) {
						criteriaCount++;
					}
				}
				if ( criteriaCount == searchCriteria.getCriteria().size()) {
					result.add(object);
				}
			}
		}
		else {
			for ( String s : criteriaFoundInConstraints ) {
				Map<String,Object> mm = storage.get(s);
				result.add(mm);
			}
		}
		
		return result;
	}
	
	
	
	private boolean matchValue(Object value, SearchCriterium criterium) {
		Object criteriumValue = criterium.getValue();
		if ( value instanceof List ) {
			List<Object> l = (List)value;
			boolean found = false;
			for ( Object o : l  ) {
				if ( evaluate(o, criteriumValue,criterium.getSearchOperation())) {
					found = true;
				}
			}
			return found;
		}
		else {
			if ( value == null ) {
				throw new DataException("criterium with key " + criterium.getKey() + " not valid");
			}
			return evaluate(value, criteriumValue,criterium.getSearchOperation());
		}
	}
	
	
	
	private  boolean evaluate( Object firstValue, Object secondValue, SearchOperation searchOperation ) {
		if ( searchOperation == SearchOperation.EQUALS) {
			return firstValue.equals(secondValue);
		}
		else if ( searchOperation == SearchOperation.NOT_EQUALS) {
			return !firstValue.equals(secondValue);
		}
		else if ( searchOperation == SearchOperation.STARTS_WITH) {
			return firstValue.toString().startsWith(secondValue.toString());
		}
		else if ( searchOperation == SearchOperation.CONTAINS) {
			return firstValue.toString().contains(secondValue.toString());
		}
		else if ( searchOperation == SearchOperation.ENDS_WITH) {
			return firstValue.toString().endsWith(secondValue.toString());
		}
		else if ( searchOperation == SearchOperation.PRESENT) {
			return firstValue != null;
		}
		else if ( searchOperation == SearchOperation.GREATER_THEN) {
			return firstValue.toString().compareTo(secondValue.toString()) > 0 ? true : false ;
		}
		else if ( searchOperation == SearchOperation.GREATER_THEN_OR_EQUAL) {
			return firstValue.toString().compareTo(secondValue.toString()) >= 0 ? true : false ;
		}
		else if ( searchOperation == SearchOperation.LESS_THEN) {
			return firstValue.toString().compareTo(secondValue.toString()) < 0 ? true : false ;
		}
		else if ( searchOperation == SearchOperation.LESS_THEN_EQUAL) {
			return firstValue.toString().compareTo(secondValue.toString()) <= 0 ? true : false ;
		}
		return false;
	}
	
	


	
	public Object getRecursiveObject(Map<String, Object> object, String key) {
		if ( key.contains(StringUtils.DOT)) {
			Object o = null;
			int index = 0;
			while ( (index = key.indexOf(StringUtils.DOT)) > 0 ) {
				String part = key.substring(0, index);
				if ( o == null ) {
					o = object.get(part);
				}
				else {
					//TODO
				}
				key = key.substring(index, key.length());
			}
			key = key.substring(1, key.length());
			if ( o instanceof List ) {
				List<Object> valueList = new ArrayList<>();
				List<Map> mapList = (List)o;
				for ( Map m : mapList ) {
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
	
	
	
	
	

	private List<Map> sort(List<Map> result, String sortBy, SortOrder sortOrder) {
		
		if ( !StringUtils.isEmpty(sortBy)) {
			
			Collections.sort(result, new Comparator<Map>() {

				@Override
				public int compare(Map arg0, Map arg1) {
					
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
