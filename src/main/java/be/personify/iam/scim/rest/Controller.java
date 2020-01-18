package be.personify.iam.scim.rest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.util.UriComponentsBuilder;

import be.personify.iam.scim.schema.Schema;
import be.personify.iam.scim.schema.SchemaAttribute;
import be.personify.iam.scim.schema.SchemaException;
import be.personify.iam.scim.schema.SchemaReader;
import be.personify.iam.scim.storage.ConstraintViolationException;
import be.personify.iam.scim.storage.StorageImplementationFactory;
import be.personify.iam.scim.util.Constants;
import be.personify.iam.scim.util.ScimErrorType;

/**
 * Mappings
 * @author vanderw
 *
 */
public class Controller {
	
	private static final String SCHEMA_VALIDATION = "schema validation : ";

	private static final Logger logger = LogManager.getLogger(Controller.class);
	
	@Autowired
	private StorageImplementationFactory storageImplementationFactory;
	
	
	protected ResponseEntity<Map<String, Object>> post(Map<String, Object> entity, HttpServletRequest request, HttpServletResponse response, Schema schema ) {
		long start = System.currentTimeMillis();
		try {
			//validate
			SchemaReader.getInstance().validate(schema, entity, true);

			//prepare
			String id = createId(entity);
			entity.put(Constants.ID, id);
			String location = UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request)).build().toUriString() + Constants.SLASH + id;
			createMeta( new Date(), id, entity, schema.getName(), location);
			response.addHeader(Constants.HEADER_LOCATION, location);
			
			//store and return
			storageImplementationFactory.getStorageImplementation(schema).put(id, entity);
			logger.info("resource of type {} with id {} created in {} ms", schema.getName(), id, ( System.currentTimeMillis() -start));
			return new ResponseEntity<>(filterResponse(schema, entity), HttpStatus.CREATED);
			
		} 
		catch (SchemaException e) { 
			logger.error("invalid schema in {} ms : {}", ( System.currentTimeMillis() -start), e.getMessage());
			return showError( 400, SCHEMA_VALIDATION + e.getMessage(), ScimErrorType.invalidSyntax );
		}
		catch ( ConstraintViolationException e) {
			logger.error("constraint violation in {} ms : {}", ( System.currentTimeMillis() -start), e.getMessage());
			return showError( 409, SCHEMA_VALIDATION + e.getMessage(), ScimErrorType.uniqueness );
		}
	}
	
	
	
	
	
	protected ResponseEntity<Map<String, Object>> put(String id, Map<String, Object> entity, HttpServletRequest request, HttpServletResponse response, Schema schema ) {
		long start = System.currentTimeMillis();
		try {
			//validate
			SchemaReader.getInstance().validate(schema, entity, true);
			if ( !entity.get(Constants.ID).equals(id)){
				return showError( 400, "id [" + entity.get(Constants.ID) + "] given in the data does not match the one in the url [" + id + "]", null );
			};
			Map<String,Object> existingUser = storageImplementationFactory.getStorageImplementation(schema).get(id);
			if ( existingUser != null ) {
				entity.put(Constants.KEY_META, existingUser.get(Constants.KEY_META));
			}
			else {
				return showError( 404, "resource of type " + schema.getName() + " with id " + id + " can not be updated", null );
			}
			
			//prepare
			String location = UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request)).build().toUriString();
			createMeta( new Date(), id, entity, schema.getName(), location);
			response.addHeader(Constants.HEADER_LOCATION, location);
			
			//store
			storageImplementationFactory.getStorageImplementation(schema).put(id, entity);
			logger.info("resource of type {} with id {} updated in {} ms", schema.getName(), id, ( System.currentTimeMillis() -start));
			return new ResponseEntity<>(filterResponse(schema, entity), HttpStatus.OK);
			
		} 
		catch (SchemaException e) {
			logger.error("invalid schema in {} ms : {}", ( System.currentTimeMillis() -start), e.getMessage());
			return showError( 400, SCHEMA_VALIDATION + e.getMessage(), ScimErrorType.invalidSyntax );
		}
		catch ( ConstraintViolationException e) {
			logger.error("constraint violation", e);
			return showError( 409, SCHEMA_VALIDATION + e.getMessage(), ScimErrorType.uniqueness );
		}
	}
	
	
	
	
	
	protected ResponseEntity<Map<String, Object>> patch(String id, Map<String, Object> entity, HttpServletRequest request, HttpServletResponse response, Schema schema ) {
		long start = System.currentTimeMillis();
		try {
			//validate
			SchemaReader.getInstance().validate(schema, entity, false);
			if ( !entity.get(Constants.ID).equals(id)){
				return showError( 400, "invalid id given in the patch request", null );
			};
			String location = UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request)).build().toUriString();
			
			
			Map<String,Object> existingEntity = storageImplementationFactory.getStorageImplementation(schema).get(id);
			if ( existingEntity != null ) {
				entity.put(Constants.KEY_META, existingEntity.get(Constants.KEY_META));
			}
			else {
				return showError( 404, "resource of type " + schema.getName() + " with id " + id + " can not be updated", null );
			}
		
			response.addHeader(Constants.HEADER_LOCATION, location);
			
			for ( String key : entity.keySet()) {
				if ( !key.equals(Constants.ID)) {
					existingEntity.put(key, entity.get(key));
				}
			}
			
			createMeta( new Date(), id, existingEntity, schema.getName(), location);
			storageImplementationFactory.getStorageImplementation(schema).put(id, existingEntity);
			
			logger.info("resource of type {} with id {} patched in {} ms", schema.getName(), id, ( System.currentTimeMillis() -start));
			return new ResponseEntity<>(filterResponse(schema, existingEntity), HttpStatus.OK);
			
		} 
		catch (SchemaException e) {
			logger.error("invalid schema in {} ms : {}", ( System.currentTimeMillis() -start), e.getMessage());
			return showError( 400, SCHEMA_VALIDATION + e.getMessage(), ScimErrorType.invalidSyntax );
		}
		catch ( ConstraintViolationException e) {
			logger.error("constraint violation", e);
			return showError( 409, SCHEMA_VALIDATION + e.getMessage(), ScimErrorType.uniqueness );
		}
	}
	
	
	
	
	protected ResponseEntity<Map<String, Object>> get(String id, HttpServletRequest request, HttpServletResponse response, Schema schema) {
		long start = System.currentTimeMillis();
		Map<String,Object> user = storageImplementationFactory.getStorageImplementation(schema).get(id);
		
		ResponseEntity<Map<String,Object>> result = null;
		if ( user != null ) {
			user = filterResponse(schema, user);
			result = new ResponseEntity<>(user, HttpStatus.OK);
			response.addHeader(Constants.HEADER_LOCATION, UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request)).build().toUriString());
		}
		else {
			result = new ResponseEntity<Map<String,Object>>(HttpStatus.NOT_FOUND);
		}
		logger.info("resource of type {} with id {} fetched in {} ms", schema.getName(), id, ( System.currentTimeMillis() -start));
		return result;
	}
	
	
	
	
	protected ResponseEntity<Map<String, Object>> search(Integer startIndex, Integer count, Schema schema ) {
		long start = System.currentTimeMillis();
				
		List<Map<String,Object>> dataFetched = storageImplementationFactory.getStorageImplementation(schema).getAll();
		List<Map<String,Object>> data = new ArrayList<>();
		for ( Map<String,Object> entity : dataFetched) {
			data.add(filterResponse(schema, entity));
		}
		
		ResponseEntity<Map<String,Object>> result = null;
		
		Map<String,Object> responseObject = new HashMap<>();
		responseObject.put(Constants.KEY_SCHEMAS, new String[] {Constants.SCHEMA_LISTRESPONSE});
		responseObject.put(Constants.KEY_STARTINDEX, startIndex);
		responseObject.put(Constants.KEY_ITEMSPERPAGE, count);
		
		count = count > data.size() ? data.size() : count;
		List<Map<String,Object>> sublist = data.subList(startIndex -1, count);
		responseObject.put(Constants.KEY_TOTALRESULTS, data.size());
		responseObject.put(Constants.KEY_RESOURCES, sublist);
		
		result = new ResponseEntity<>(responseObject, HttpStatus.OK);
		
		logger.info("resources of type {} fetched in {} ms", schema.getName(), ( System.currentTimeMillis() -start));
		
		return result;
	}
	
	
	
	protected ResponseEntity<?> delete(String id, Schema schema ) {
		long start = System.currentTimeMillis();
		
		Map<String,Object> m = storageImplementationFactory.getStorageImplementation(schema).get(id);
		
		ResponseEntity<?> result = null;
		if ( m != null ) {
			boolean deleted = storageImplementationFactory.getStorageImplementation(schema).delete(id);
			if ( deleted ) {
				result = new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			}
			else {
				return showError( 400, "could not delete resource of type " + schema.getName() + " with id " + id, null );
			}
		}
		else {
			result = new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		logger.info("resource of type {} with id {} deleted in {} ms", schema.getName(), id, ( System.currentTimeMillis() -start));
		
		return result;
	}
	
	

	
	
	protected void createMeta(Date date, String id, Map<String, Object> user, String resourceType, String location) {
		
		Map<String,String> map = new HashMap<String, String>();
		String formattedDate = new SimpleDateFormat(Constants.DATEFORMAT_STRING, Locale.US).format(date);
		if ( user.containsKey(Constants.KEY_META)) {
			map = (Map<String,String>)user.get(Constants.KEY_META);
		}
		else {
			map.put(Constants.KEY_CREATED, formattedDate);	
		}
		map.put(Constants.KEY_RESOURCE_TYPE, resourceType);
		map.put(Constants.KEY_LAST_MODIFIED, formattedDate);
		map.put(Constants.KEY_VERSION, createVersion(date) );
		map.put(Constants.KEY_LOCATION, location);
		
		user.put(Constants.KEY_META,map);
		
	}
	
	
	protected String createVersion( Date date ) {
		return Constants.EMPTY + date.getTime();
	}



	protected ResponseEntity<Map<String, Object>> showError(int status, String detail, ScimErrorType scimType) {
		Map<String,Object> error = new HashMap<>();
		error.put(Constants.KEY_SCHEMAS, Constants.SCHEMA_ERROR);
		if ( scimType != null) {
			error.put(Constants.KEY_SCIMTYPE, scimType);
		}
		error.put(Constants.KEY_DETAIL, detail);
		error.put(Constants.KEY_STATUS, Constants.EMPTY + status );
		return new ResponseEntity<Map<String, Object>>(error, HttpStatus.valueOf(status));
	}
	
	
	

	protected Map<String, Object> filterResponse(Schema schema, Map<String, Object> entity) {
		Map<String,Object> copy = new HashMap<>();
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
	
	
	
	protected List<String> extractSchemas(Map<String, Object> user) {
		return (List<String>)user.get(Constants.KEY_SCHEMAS);
	}
	
	
	protected ResponseEntity<Map<String, Object>> invalidSchemaForResource(List<String> schemas, String resourceType) {
		return showError( 400, "schemas contains no " + resourceType +  " schema " + schemas.toString(), ScimErrorType.invalidSyntax );
	}

	
	



}
