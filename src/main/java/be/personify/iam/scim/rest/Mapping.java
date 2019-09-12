package be.personify.iam.scim.rest;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import be.personify.iam.scim.schema.Schema;
import be.personify.iam.scim.schema.SchemaAttribute;
import be.personify.iam.scim.schema.SchemaReader;
import be.personify.iam.scim.util.Constants;
import be.personify.iam.scim.util.ScimErrorType;

/**
 * Mappings
 * @author vanderw
 *
 */
public class Mapping {
	
	
	protected void createMeta(Date date, String id, Map<String, Object> user, String resourceType, String location) {
		
		Map<String,String> map = new HashMap<String, String>();
		String formattedDate = Constants.format.format(date);
		if ( user.containsKey(Constants.KEY_META)) {
			map = (Map<String,String>)user.get(Constants.KEY_META);
		}
		else {
			map.put(Constants.KEY_CREATED, formattedDate);	
		}
		map.put(Constants.KEY_RESOURCE_TYPE, resourceType);
		map.put(Constants.KEY_LAST_MODIFIED, formattedDate);
		String version = Constants.EMPTY + date.getTime();
		map.put(Constants.KEY_VERSION, version );
		map.put(Constants.KEY_LOCATION, location);
		
		user.put(Constants.KEY_META,map);
		
	}



	protected ResponseEntity<Map<String, Object>> showError(int status, String detail, ScimErrorType scimType) {
		Map<String,Object> error = new HashMap<String, Object>();
		error.put(Constants.KEY_SCHEMAS, new String[] {"urn:ietf:params:scim:api:messages:2.0:Error"});
		if ( scimType != null) {
			error.put("scimType", scimType);
		}
		error.put("detail", detail);
		error.put("status", "" + status );
		return new ResponseEntity<Map<String, Object>>(error, HttpStatus.valueOf(status));
	}
	
	
	

	protected Map<String, Object> filterResponse(String resourceType, Map<String, Object> entity) {
		Schema schema = SchemaReader.getInstance().getSchemaByType(resourceType);
		Map<String,Object> copy = new HashMap<String, Object>();
		copy.putAll(entity);
		for ( SchemaAttribute attribute : schema.getAttributes()) {
			if ( attribute.getReturned().equalsIgnoreCase(Constants.NEVER)) {
				copy.remove(attribute.getName());
			}
		}
		return copy;
	}
	
	
	
	protected String createId(Map<String, Object> user) {
		Object id = user.get(Constants.ID);
		return id != null ? id.toString() : UUID.randomUUID().toString();
	}

	
	
	





}
