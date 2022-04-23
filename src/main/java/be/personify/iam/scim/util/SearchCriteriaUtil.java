package be.personify.iam.scim.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import be.personify.iam.scim.rest.InvalidFilterException;
import be.personify.util.LogicalOperator;
import be.personify.util.SearchCriteria;
import be.personify.util.SearchCriterium;
import be.personify.util.SearchOperation;
import be.personify.util.StringUtils;

public class SearchCriteriaUtil {
	
	private static final Logger logger = LogManager.getLogger(SearchCriteriaUtil.class);
	
	
	
	public SearchCriteria composeSearchCriteria(String filter) throws InvalidFilterException {
		SearchCriteria searchCriteria = new SearchCriteria();
		if (!StringUtils.isEmpty(filter)) {
			logger.info("composing searchcriteria from filter {}", filter);
			filter = filter.trim();
			while( !StringUtils.isEmpty(filter)) {
				try {
					filter = addNextPart(filter,searchCriteria, null);
				}
				catch( Exception e) {
					e.printStackTrace();
					throw new InvalidFilterException(e.getMessage());
				}
			}
		}
		return searchCriteria;
	}
	
	
	

	private String addNextPart(String filter, SearchCriteria searchCriteria, String keyPrefix) {
		if ( StringUtils.isEmpty(filter)) {
			return filter;
		}
		filter = filter.trim();
		//logger.info("filter 1 {}", filter);
		if ( filter.startsWith("and")) {
			searchCriteria.setOperator(LogicalOperator.AND);
			filter = filter.substring(3, filter.length());
		}
		else if ( filter.startsWith("or")) {
			searchCriteria.setOperator(LogicalOperator.OR);
			filter = filter.substring(2, filter.length());
		}
		filter = filter.trim();
		//it's a group
		//logger.info("filter 2 {}", filter);
		if ( filter.startsWith("(")) {
			int closing = filter.indexOf(")");
			String subFilter = filter.substring(1, closing);
			SearchCriteria s = new SearchCriteria();
			while( !StringUtils.isEmpty(subFilter)) {
				subFilter = addNextPart(subFilter,s, null);
			}
			searchCriteria.getGroupedCriteria().add(s);
			filter = filter.substring(closing + 1, filter.length());
		}
		else {
			int spaceIndex = filter.indexOf(StringUtils.SPACE);
			//read key
			String key = filter.substring(0, spaceIndex );
			
			//complex filtering
			int complexStart = key.indexOf("[");
			if ( complexStart != -1 ) {
				String newKeyPrefix = key.substring(0,complexStart);
				int complexEnd = filter.indexOf("]");
				String complexFilter = filter.substring(complexStart +1, complexEnd);
				SearchCriteria s = new SearchCriteria();
				while( !StringUtils.isEmpty(complexFilter)) {
					complexFilter = addNextPart(complexFilter,s, newKeyPrefix);
				}
				searchCriteria.getGroupedCriteria().add(s);
				filter = filter.substring(complexEnd + 1).trim();
				return addNextPart(filter, searchCriteria, null);
			}
			else {
				filter = filter.substring(spaceIndex + 1).trim();
				//read operator
				String operator = null;
				spaceIndex = filter.indexOf(StringUtils.SPACE);
				if ( spaceIndex != -1) {
					operator = filter.substring(0, spaceIndex );
					filter = filter.substring(spaceIndex + 1).trim();
				}
				else {
					operator = filter.substring(0, filter.length() );
					filter = StringUtils.EMPTY_STRING;
				}
				SearchOperation operation = SearchOperation.operationFromString(operator.toLowerCase());
				
				//read value if necessary
				String value = null;
				if ( operation.getParts() == 3) {
					spaceIndex = filter.indexOf(StringUtils.SPACE);
					if ( spaceIndex != -1) {
						value = filter.substring(0, spaceIndex );
						filter = filter.substring(spaceIndex + 1).trim();
					}
					else {
						value = filter.substring(0, filter.length() );
						filter = StringUtils.EMPTY_STRING;
					}
					value = value.replaceAll("\"", StringUtils.EMPTY_STRING);
				}
				//add criteria
				if ( keyPrefix != null ) {
					key = keyPrefix + StringUtils.DOT + key;
				}
				searchCriteria.getCriteria().add(new SearchCriterium(key, value, operation));
			}
		}
		return filter;
	}




}
