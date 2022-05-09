package be.personify.iam.scim.storage.util;


import be.personify.iam.scim.storage.SortOrder;
import be.personify.util.StringUtils;

public class CouchBaseUtil {
	
	
	private static final String ASC = "asc";
	private static final String DESC = "desc";
	
	
	
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
