package be.personify.iam.scim.storage.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import be.personify.util.LogicalOperator;
import be.personify.util.SearchCriteria;
import be.personify.util.SearchCriterium;
import be.personify.util.SearchOperation;
import be.personify.util.StringUtils;

public class MemoryStorageUtil {
	
	private static final Logger logger = LogManager.getLogger(MemoryStorageUtil.class);
	
	public static boolean objectMatchesCriteria(SearchCriteria searchCriteria, Map<String, Object> object) {
		int criteriaCount = 0;
		boolean or = searchCriteria.getOperator() == LogicalOperator.OR ? true : false;
		for (SearchCriterium criterium : searchCriteria.getCriteria()) {
			Object value = getRecursiveObject(object, criterium.getKey());
			if (matchValue(value, criterium)) {
				if ( or ) {
					return true;
				}
				criteriaCount++;
			}
		}
		for( SearchCriteria groupedCriteria : searchCriteria.getGroupedCriteria() ) {
			if ( objectMatchesCriteria(groupedCriteria, object)) {
				if ( or ) {
					return true;
				}
				criteriaCount++;
			}
		}
		
		if (!or && criteriaCount == searchCriteria.size()) {
			return true;
		}
		return false;
	}
	
	
	
	
	public static Object getRecursiveObject(Map<String, Object> object, String key) {
		if (key.contains(StringUtils.DOT)) {
			Object o = null;
			int index = 0;
			while ((index = key.indexOf(StringUtils.DOT)) > 0) {
				String part = key.substring(0, index);
				if (o == null) {
					o = object.get(part);
				} else {
					// TODO
				}
				key = key.substring(index, key.length());
			}
			key = key.substring(1, key.length());
			if (o instanceof List) {
				List<Object> valueList = new ArrayList<>();
				List<Map> mapList = (List) o;
				for (Map m : mapList) {
					valueList.add(m.get(key));
				}
				return valueList;
			} else if (o instanceof Map) {
				Map map = (Map) o;
				return map.get(key);
			}
			return o;
		}
		return object.get(key);
	}
	
	
	
	private static boolean matchValue(Object value, SearchCriterium criterium) {
		Object criteriumValue = criterium.getValue();
		if (value instanceof List) {
			List<Object> l = (List) value;
			boolean found = false;
			for (Object o : l) {
				if ( o == null ) {
					return false;
				}
				else if (evaluate(o, criteriumValue, criterium.getSearchOperation())) {
					found = true;
				}
			}
			return found;
		} else {
			if (value == null && criterium.getSearchOperation() != SearchOperation.PRESENT) {
				return false;
			}
			return evaluate(value, criteriumValue, criterium.getSearchOperation());
		}
	}

	
	
	private static boolean evaluate(Object firstValue, Object secondValue, SearchOperation searchOperation) {
		logger.info("evaluating {} {} {} ", firstValue, secondValue, searchOperation);
		if (searchOperation == SearchOperation.EQUALS) {
			return firstValue.equals(secondValue);
		} else if (searchOperation == SearchOperation.NOT_EQUALS) {
			return !firstValue.equals(secondValue);
		} else if (searchOperation == SearchOperation.STARTS_WITH) {
			return firstValue.toString().startsWith(secondValue.toString());
		} else if (searchOperation == SearchOperation.CONTAINS) {
			return firstValue.toString().contains(secondValue.toString());
		} else if (searchOperation == SearchOperation.ENDS_WITH) {
			return firstValue.toString().endsWith(secondValue.toString());
		} else if (searchOperation == SearchOperation.PRESENT) {
			return firstValue != null;
		} else if (searchOperation == SearchOperation.GREATER_THEN) {
			return firstValue.toString().compareTo(secondValue.toString()) > 0 ? true : false;
		} else if (searchOperation == SearchOperation.GREATER_THEN_OR_EQUAL) {
			return firstValue.toString().compareTo(secondValue.toString()) >= 0 ? true : false;
		} else if (searchOperation == SearchOperation.LESS_THEN) {
			return firstValue.toString().compareTo(secondValue.toString()) < 0 ? true : false;
		} else if (searchOperation == SearchOperation.LESS_THEN_EQUAL) {
			return firstValue.toString().compareTo(secondValue.toString()) <= 0 ? true : false;
		}
		return false;
	}

}
