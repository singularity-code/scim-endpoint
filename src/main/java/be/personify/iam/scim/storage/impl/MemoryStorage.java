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
import be.personify.iam.scim.storage.Storage;
import be.personify.iam.scim.storage.util.MemoryStorageUtil;
import be.personify.iam.scim.util.Constants;
import be.personify.iam.scim.util.PropertyFactory;
import be.personify.util.SearchCriteria;
import be.personify.util.SearchCriterium;
import be.personify.util.SortOrder;
import be.personify.util.StringUtils;

/**
 * Sample storage implementation that stores data into a volatile memory store
 *
 * @author vanderw
 */
public class MemoryStorage implements Storage {

	@Autowired
	private PropertyFactory propertyFactory;

	private static final Logger logger = LogManager.getLogger(MemoryStorage.class);

	private Map<String, Map<String, Object>> storage = null;

	private List<String> uniqueConstraintsList = new ArrayList<String>();
	private Map<String, Map<Object, Object>> uniqueConstraints = null;

	private String type;

	@Override
	public Map<String, Object> get(String id) {
		return storage.get(id);
	}

	@Override
	public Map<String, Object> get(String id, String version) {
		throw new RuntimeException("versioning not implemented");
	}

	@Override
	public List<String> getVersions(String id) {
		throw new RuntimeException("versioning not implemented");
	}

	@Override
	public void create(String id, final Map<String, Object> object) throws ConstraintViolationException {
		checkConstraints(id, object);
		synchronized (storage) {
			storage.put(id, object);
		}
		updateConstraints(id, object);
	}

	@Override
	public void update(String id, final Map<String, Object> object) throws ConstraintViolationException {
		checkConstraints(id, object);
		storage.put(id, object);
		updateConstraints(id, object);
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
	public List<Map> search(SearchCriteria searchCriteria, int start, int count, String sortBy, String sortOrderString) {
		return search(searchCriteria, start, count, sortBy, sortOrderString, null);
	}
	

	
	@Override
	public List<Map> search(SearchCriteria searchCriteria, int start, int count, String sortBy, String sortOrderString, List<String> includeAttributes) {
		List<Map> result = null;
		if (searchCriteria == null || searchCriteria.getCriteria() == null || searchCriteria.size() == 0) {
			result = new ArrayList<Map>(storage.values());
		}
		else {
			logger.debug("{}", searchCriteria);
			result = filterOnSearchCriteria(searchCriteria);
		}

		if (StringUtils.isEmpty(sortOrderString)) {
			sortOrderString = SortOrder.ascending.name();
			logger.debug("defaulting to sortorder {}", sortOrderString);
		}
		SortOrder sortOrder = SortOrder.valueOf(sortOrderString);

		result = sort(result, sortBy, sortOrder);

		count = count > result.size() ? result.size() : count;
		logger.info("count {} start {}", count, start);
		int newStart = (start - 1);
		if ( newStart > result.size()) {
			newStart = result.size();
		}
		int newEnd = newStart + count;
		if ( newEnd > result.size()) {
			newEnd = result.size();
		}
		
		List<Map> sublist = result.subList(newStart, newEnd);

		return sublist;
	}

	
	
	
	
	@Override
	public long count(SearchCriteria searchCriteria) {
		long totalCount = 0;
		
		if ( searchCriteria == null || searchCriteria.getCriteria().size() == 0 ) {
			return storage.values().size();
		}

		// first check constraints
		int count = 0;
		List<String> criteriaFoundInConstraints = new ArrayList<String>();
		for (SearchCriterium criterium : searchCriteria.getCriteria()) {
			Map m = uniqueConstraints.get(criterium.getKey());
			if (m != null) {
				String id = (String) m.get(criterium.getValue());
				// logger.info("found id in unique {} for value {}", id, criterium.getValue());
				if (id != null && !criteriaFoundInConstraints.contains(id)) {
					// logger.info("putting it in map {}", id);
					criteriaFoundInConstraints.add(id);
				}
				count++;
			}
		}

		if (count < searchCriteria.size()) {
			logger.debug("not all criteria are found in constraints");
			for (Map<String, Object> object : storage.values()) {
				if ( MemoryStorageUtil.objectMatchesCriteria(searchCriteria, object)) {
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

		// first check constraints
		int count = 0;
		List<String> criteriaFoundInConstraints = new ArrayList<String>();
		for (SearchCriterium criterium : searchCriteria.getCriteria()) {
			Map m = uniqueConstraints.get(criterium.getKey());
			if (m != null) {
				String id = (String) m.get(criterium.getValue());
				// logger.info("found id in unique {} for value {}", id, criterium.getValue());
				if (id != null && !criteriaFoundInConstraints.contains(id)) {
					// logger.info("putting it in map {}", id);
					criteriaFoundInConstraints.add(id);
				}
				count++;
			}
		}

		if (count < searchCriteria.size()) {
			logger.debug("not all criteria are found in the constraints");
			for (Map<String, Object> object : storage.values()) {
				if ( MemoryStorageUtil.objectMatchesCriteria(searchCriteria, object)) {
					result.add(object);
				}
			}
		} else {
			for (String s : criteriaFoundInConstraints) {
				Map<String, Object> mm = storage.get(s);
				result.add(mm);
			}
		}

		return result;
	}

	
	
	

	

	private List<Map> sort(List<Map> result, String sortBy, SortOrder sortOrder) {

		if (!StringUtils.isEmpty(sortBy)) {

			Collections.sort(result, new Comparator<Map>() {

				@Override
				public int compare(Map arg0, Map arg1) {

					Object value0 = arg0.get(sortBy);
					Object value1 = arg1.get(sortBy);

					if (value0 instanceof String && value1 instanceof String) {

						if (sortOrder == SortOrder.ascending) {
							return ((String) value0).compareTo(((String) value1));
						} else {
							return ((String) value1).compareTo(((String) value0));
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
		if (f.exists()) {
			logger.info("{} exists, trying to read", f.getAbsolutePath());
			try {
				long start = System.currentTimeMillis();
				storage = Constants.objectMapper.readValue(f, Map.class);
				buildConstraints(storage);
				logger.info("{} read in {} ms", f.getAbsolutePath(), (System.currentTimeMillis() - start));
			} catch (IOException e) {
				logger.error("can not read file {}", f.getAbsolutePath(), e);
				storage = null;
			}
		} else {
			storage = new HashMap<String, Map<String, Object>>();
		}
	}

	private void initializeUniqueConstraints(String type) {
		uniqueConstraints = new HashMap<String, Map<Object, Object>>();
		if ( propertyFactory != null ) {
			String uniqueConstraintsString = propertyFactory.getProperty("scim.storage.memory." + type.toLowerCase() + ".unique");
			if (!StringUtils.isEmpty(uniqueConstraintsString)) {
				uniqueConstraintsList = Arrays.asList(uniqueConstraintsString.split(StringUtils.COMMA));
				for (String u : uniqueConstraintsList) {
					uniqueConstraints.put(u, new HashMap<Object, Object>());
				}
			}
		}
	}

	private void buildConstraints(Map<String, Map<String, Object>> map) {
		for (String key : map.keySet()) {
			Map<String, Object> entity = map.get(key);
			Object id = entity.get("id");
			for (String constraint : uniqueConstraintsList) {
				Object o = entity.get(constraint);
				if (o != null) {
					uniqueConstraints.get(constraint).put(o, id);
				}
			}
		}
	}

	private void checkConstraints(String id, Map<String, Object> object) throws ConstraintViolationException {
		synchronized (uniqueConstraints) {
			for (String constraint : uniqueConstraintsList) {
				Object valueFromEntity = object.get(constraint);
				Object valueFromConstraintCache = uniqueConstraints.get(constraint).get(valueFromEntity);
				if (valueFromConstraintCache != null) {
					if (!valueFromConstraintCache.equals(id)) {
						throw new ConstraintViolationException(
								"the value " + valueFromEntity + " already exists for the attribute " + constraint);
					}
				}
			}
		}
	}

	private void updateConstraints(String id, Map<String, Object> object) {
		synchronized (uniqueConstraints) {
			for (String constraint : uniqueConstraintsList) {
				Object valueFromEntity = object.get(constraint);
				if (valueFromEntity != null) {
					uniqueConstraints.get(constraint).put(valueFromEntity, id);
				}
			}
		}
	}

	private void removeConstraints(String id) {
		for (String constraint : uniqueConstraintsList) {
			if (uniqueConstraints.get(constraint).containsValue(id)) {
				synchronized (uniqueConstraints) {
					uniqueConstraints.get(constraint).values().remove(id);
				}
			}
		}
	}

	@Override
	public synchronized void flush() {
		Boolean flush = Boolean.valueOf(propertyFactory.getProperty("scim.storage.flush"));
		if (flush) {
			logger.debug("flushing");

			File f = getStorageFile();
			logger.debug("saving to file {}", f.getAbsolutePath());
			try {
				long start = System.currentTimeMillis();
				synchronized (storage) {
					Constants.objectMapper.writeValue(f, storage); 
				}
				logger.debug("{} saved in {} ms", f.getAbsolutePath(), (System.currentTimeMillis() - start));
			} catch (IOException e) {
				logger.error("can not flush", e);
			}
		} else {
			logger.info("flush not configured");
		}
	}

	@Override
	public boolean deleteAll() {
		storage.clear();
		for (String constraint : uniqueConstraintsList) {
			uniqueConstraints.get(constraint).clear();
		}
		flush();
		return true;
	}

	private File getStorageFile() {
		if ( propertyFactory != null ) {
			String dir = propertyFactory.getProperty("scim.storage.memory.flushToFileDirectory");
			if (dir != null) {
				File directory = new File(dir);
				if (directory.exists() && directory.isDirectory()) {
					return new File(directory, "personify-scim-" + type.toLowerCase() + ".dump");
				}
			}
		}
		return new File(Constants.tempDir, "personify-scim-" + type.toLowerCase() + ".dump");
	}
}
