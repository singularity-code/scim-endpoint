package be.personify.iam.scim.util;

import java.text.SimpleDateFormat;
import java.util.Locale;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Constants {
	
	public static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
	//format.setTimeZone(TimeZone.getTimeZone("UTC"));
	
	public static final String SCHEMA_USER = "urn:ietf:params:scim:schemas:core:2.0:User";
	public static final String SCHEMA_GROUP = "urn:ietf:params:scim:schemas:core:2.0:Group";
	public static final String SCHEMA_SERVICEPROVIDERCONFIG = "urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig";
	
	public static final String RESOURCE_TYPE_USER = "User";
	public static final String RESOURCE_TYPE_GROUP = "Group";
	public static final String RESOURCE_TYPE_SERVICEPROVIDERCONFIG = "ServiceProviderConfig";
	
	public static final String KEY_SCHEMAS = "schemas";
	public static final String KEY_RESOURCE_TYPE = "resourceType";
	public static final String KEY_CREATED = "created";
	public static final String KEY_LAST_MODIFIED = "lastModified";
	public static final String KEY_LOCATION = "location";
	public static final String KEY_VERSION = "version";
	public static final String KEY_META = "meta";
	
	
	public static final ObjectMapper objectMapper = new ObjectMapper();
	
	public static final String SLASH = "/";
	public static final String EMPTY = "";
	
	public static final String ID = "id";

	
	
	public static final String HEADER_LOCATION = "Location";
	
	

}
