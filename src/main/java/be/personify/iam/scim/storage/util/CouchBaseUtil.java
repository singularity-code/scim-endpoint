package be.personify.iam.scim.storage.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import be.personify.iam.scim.storage.DataException;
import be.personify.util.SearchCriteria;
import be.personify.util.SearchCriterium;
import be.personify.util.SearchOperation;
import be.personify.util.SortOrder;
import be.personify.util.StringUtils;

/**
 * 
 * Utility class for couchbase
 * 
 *
 */
public class CouchBaseUtil {
	
	
	private static final Logger logger = LogManager.getLogger(CouchBaseUtil.class);
	
	
	private static final String ASC = "asc";
	private static final String DESC = "desc";
	
	private static final String COUCHBASE_OPERATOR_PRESENT = " is not null ";
	private static final String COUCHBASE_OPERATOR_NOT_EQUALS = " <> ";
	private static final String COUCHBASE_OPERATOR_EQUALS = " = ";
	private static final String COUCHBASE_OPERATOR_GT = " > ";
	private static final String COUCHBASE_OPERATOR_GTE = " >= ";
	private static final String COUCHBASE_OPERATOR_LT = " < ";
	private static final String COUCHBASE_OPERATOR_LTE = " <= ";
	private static final String COUCHBASE_OPERATOR_CONTAINS = " like ";
	
	
	
	public static String getSort( String sortBy, String sortOrder, SortOrder defaultSortOrder ) {
		String sort = StringUtils.EMPTY_STRING;
		if ( !StringUtils.isEmpty(sortBy)) {
			sort  = " ORDER BY";
			String[] sb = sortBy.split(StringUtils.COMMA);
			String[] so = new String[sb.length];
			if ( !StringUtils.isEmpty(sortOrder)) {
				String[] soo = sortOrder.split(StringUtils.COMMA);
				if ( soo.length != so.length) {
					for ( int i = 0; i < so.length; i++) {
						if ( i < soo.length) {
							so[i] = transformSortOrder(soo[i], defaultSortOrder);
						}
					}
					for ( int i = soo.length; i < so.length; i++) {
						so[i] = transformSortOrder(defaultSortOrder.name(), defaultSortOrder);
					}
				}
				else {
					for ( int i = 0; i < so.length; i++) {
						so[i] = transformSortOrder(soo[i], defaultSortOrder);
					}
				}
			}
			else {
				for ( int i = 0; i < so.length; i++) {
					so[i] = transformSortOrder(defaultSortOrder.name(), defaultSortOrder);
				}
			}
			for (int i = 0; i < sb.length; i++ ) {
				if ( i != 0) {
					sort = sort + StringUtils.COMMA;
				}
				sort =  sort + StringUtils.SPACE + "t." + sb[i] + StringUtils.SPACE + so[i];
			}
		}
		return sort;
		
	}
	
	
	
	public static String safeSubAttribute(String s) {
		s =  s.replace(StringUtils.DOT, StringUtils.EMPTY_STRING);
		s =  s.replace("$", StringUtils.EMPTY_STRING);
		return s;
	}
	
	
	
	
	
	
	public static StringBuilder constructQuery(SearchCriteria searchCriteria, StringBuilder sb, int count) {
		if (searchCriteria != null && searchCriteria.size() > 0) {
			SearchCriterium criterium = null;
			SearchCriteria sc = null;
			for (int i = 0; i < searchCriteria.getCriteria().size(); i++) {
				criterium = searchCriteria.getCriteria().get(i);
				int lastindexOfDOt = criterium.getKey().lastIndexOf(".");
				String modifiedKey = null;
				if ( lastindexOfDOt < 0 ) {
					modifiedKey = "t.`" + criterium.getKey() + "`";
				}
				else {
					String firstPart = criterium.getKey().substring(0,lastindexOfDOt +1);
					String secoString = criterium.getKey().substring(lastindexOfDOt + 1,criterium.getKey().length());
					logger.debug("firstp {} lastp {}", firstPart, secoString);
					modifiedKey = firstPart + "`" + secoString + "`";
				}
				sb.append(modifiedKey);
				sb.append(searchOperationToString(criterium.getSearchOperation()));
				if (criterium.getSearchOperation().getParts() == 3) {
					sb.append("$" + safeSubAttribute(criterium.getKey() + StringUtils.UNDERSCORE + count));
					count++;
				}
				if (i < (searchCriteria.size() - 1) || searchCriteria.getGroupedCriteria().size() > 1 ) {
					sb.append(StringUtils.SPACE + searchCriteria.getOperator() + StringUtils.SPACE);
				}
			}
			for (int i = 0; i < searchCriteria.getGroupedCriteria().size(); i++) {
				sc = searchCriteria.getGroupedCriteria().get(i);
				if ( i > 0 ) {
					sb.append(StringUtils.SPACE + searchCriteria.getOperator() + StringUtils.SPACE);
				}
				sb.append("( " + constructQuery(sc, new StringBuilder(), count) + " )");
				count = count + 100;
			}
		}
		return sb;
	}
	
	
	
	private static Object searchOperationToString(SearchOperation searchOperation) {
		if (searchOperation.equals(SearchOperation.EQUALS)) {
			return COUCHBASE_OPERATOR_EQUALS;
		} else if (searchOperation.equals(SearchOperation.NOT_EQUALS)) {
			return COUCHBASE_OPERATOR_NOT_EQUALS;
		} else if (searchOperation.equals(SearchOperation.PRESENT)) {
			return COUCHBASE_OPERATOR_PRESENT;
		} else if (searchOperation.equals(SearchOperation.GREATER_THEN)) {
			return COUCHBASE_OPERATOR_GT;
		} else if (searchOperation.equals(SearchOperation.GREATER_THEN_OR_EQUAL)) {
			return COUCHBASE_OPERATOR_GTE;
		} else if (searchOperation.equals(SearchOperation.LESS_THEN)) {
			return COUCHBASE_OPERATOR_LT;
		} else if (searchOperation.equals(SearchOperation.LESS_THEN_EQUAL)) {
			return COUCHBASE_OPERATOR_LTE;
		}
		else if (searchOperation.equals(SearchOperation.CONTAINS) || searchOperation.equals(SearchOperation.ENDS_WITH) || searchOperation.equals(SearchOperation.STARTS_WITH)) {
			return COUCHBASE_OPERATOR_CONTAINS;
		}
		throw new DataException("search operation " + searchOperation.name() + " not implemented");
	}
	
	
	
	
	
	private static String transformSortOrder( String scimSortOrder, SortOrder defaultSortOrder ) {
		if ( !StringUtils.isEmpty(scimSortOrder)) {
			if ( scimSortOrder.toLowerCase().equalsIgnoreCase(SortOrder.ascending.name())) {
				return ASC;
			}
			else if ( scimSortOrder.toLowerCase().equalsIgnoreCase(SortOrder.descending.name())) {
				return DESC;
			}
		}
		if ( defaultSortOrder != null ) {
			return transformSortOrder(defaultSortOrder.name(),defaultSortOrder);
		}
		else {
			return ASC;
		}
	}

}
