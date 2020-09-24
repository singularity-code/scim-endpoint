package be.personify.iam.scim.rest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.util.UriComponentsBuilder;

import be.personify.iam.scim.schema.Schema;
import be.personify.iam.scim.schema.SchemaAttribute;
import be.personify.iam.scim.schema.SchemaException;
import be.personify.iam.scim.schema.SchemaReader;
import be.personify.iam.scim.storage.ConfigurationException;
import be.personify.iam.scim.storage.ConstraintViolationException;
import be.personify.iam.scim.storage.DataException;
import be.personify.iam.scim.storage.Storage;
import be.personify.iam.scim.storage.StorageImplementationFactory;
import be.personify.iam.scim.util.Constants;
import be.personify.iam.scim.util.ScimErrorType;
import be.personify.util.SearchCriteria;
import be.personify.util.SearchCriterium;
import be.personify.util.SearchOperation;
import be.personify.util.StringUtils;

/**
 * Main controller class for the SCIM operations
 * @author vanderw
 *
 */
public class Controller {
	
	private static final String SCHEMA_VALIDATION = "schema validation : ";

	private static final Logger logger = LogManager.getLogger(Controller.class);
	
	@Value("${scim.allowIdOnCreate:true}")
	private boolean allowIdOnCreate;
	
	@Autowired
	private StorageImplementationFactory storageImplementationFactory;
	
	
	protected ResponseEntity<Map<String, Object>> post(Map<String, Object> entity, HttpServletRequest request, HttpServletResponse response, Schema schema, String attributes, String excludedAttributes ) {
		long start = System.currentTimeMillis();
		try {
			//validate
			SchemaReader.getInstance().validate(schema, entity, true);

			//prepare
			String id = createId(entity);
			entity.put(Constants.ID, id);
			String location = UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request)).build().toUriString() + StringUtils.SLASH + id;
			createMeta( new Date(), id, entity, schema.getName(), location);
			response.addHeader(Constants.HEADER_LOCATION, location);
			
			//store and return
			storageImplementationFactory.getStorageImplementation(schema).create(id, entity);
			logger.info("resource of type {} with id {} created in {} ms", schema.getName(), id, ( System.currentTimeMillis() -start));
			return new ResponseEntity<>(filterAttributes(schema, entity, attributes, excludedAttributes), HttpStatus.CREATED);
			
		} 
		catch (SchemaException e) { 
			logger.error("invalid schema in {} ms : {}", ( System.currentTimeMillis() -start), e.getMessage());
			return showError( 400, SCHEMA_VALIDATION + e.getMessage(), ScimErrorType.invalidSyntax );
		}
		catch ( ConstraintViolationException e) {
			logger.error("constraint violation in {} ms : {}", ( System.currentTimeMillis() -start), e.getMessage());
			return showError( 409, SCHEMA_VALIDATION + e.getMessage(), ScimErrorType.uniqueness );
		}
		catch( DataException | ConfigurationException e) {
			return showError(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
		}
	}
	
	
	
	
	
	protected ResponseEntity<Map<String, Object>> put(String id, Map<String, Object> entity, HttpServletRequest request, HttpServletResponse response, Schema schema, String attributes, String excludedAttributes ) {
		long start = System.currentTimeMillis();
		try {
			//validate
			SchemaReader.getInstance().validate(schema, entity, true);
			if ( !entity.get(Constants.ID).equals(id)){
				return showError( 400, "id [" + entity.get(Constants.ID) + "] given in the data does not match the one in the url [" + id + "]");
			};
			Map<String,Object> existingUser = storageImplementationFactory.getStorageImplementation(schema).get(id);
			if ( existingUser != null ) {
				entity.put(Constants.KEY_META, existingUser.get(Constants.KEY_META));
			}
			else {
				return showError( 404, "resource of type " + schema.getName() + " with id " + id + " can not be updated");
			}
			
			//prepare
			String location = UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request)).build().toUriString();
			createMeta( new Date(), id, entity, schema.getName(), location);
			response.addHeader(Constants.HEADER_LOCATION, location);
			
			//store
			storageImplementationFactory.getStorageImplementation(schema).update(id, entity);
			logger.info("resource of type {} with id {} updated in {} ms", schema.getName(), id, ( System.currentTimeMillis() -start));
			return new ResponseEntity<>(filterAttributes(schema, entity, attributes, excludedAttributes), HttpStatus.OK);
			
		} 
		catch (SchemaException e) {
			logger.error("invalid schema in {} ms : {}", ( System.currentTimeMillis() -start), e.getMessage());
			return showError( 400, SCHEMA_VALIDATION + e.getMessage(), ScimErrorType.invalidSyntax );
		}
		catch ( ConstraintViolationException e) {
			logger.error("constraint violation", e);
			return showError( 409, SCHEMA_VALIDATION + e.getMessage(), ScimErrorType.uniqueness );
		}
		catch( DataException | ConfigurationException e) {
			return showError(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
		}
	}
	
	
	
	
	
	protected ResponseEntity<Map<String, Object>> patch(String id, Map<String, Object> entity, HttpServletRequest request, HttpServletResponse response, Schema schema, String attributes, String excludedAttributes ) {
		long start = System.currentTimeMillis();
		try {
			//validate
			SchemaReader.getInstance().validate(schema, entity, false);
			if ( !entity.get(Constants.ID).equals(id)){
				return showError( 400, "invalid id given in the patch request");
			};
			String location = UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request)).build().toUriString();
			
			
			Map<String,Object> existingEntity = storageImplementationFactory.getStorageImplementation(schema).get(id);
			if ( existingEntity != null ) {
				entity.put(Constants.KEY_META, existingEntity.get(Constants.KEY_META));
			}
			else {
				return showError( 404, "resource of type " + schema.getName() + " with id " + id + " can not be updated");
			}
		
			response.addHeader(Constants.HEADER_LOCATION, location);
			
			for ( String key : entity.keySet()) {
				if ( !key.equals(Constants.ID)) {
					existingEntity.put(key, entity.get(key));
				}
			}
			
			createMeta( new Date(), id, existingEntity, schema.getName(), location);
			storageImplementationFactory.getStorageImplementation(schema).update(id, existingEntity);
			
			logger.info("resource of type {} with id {} patched in {} ms", schema.getName(), id, ( System.currentTimeMillis() -start));
			return new ResponseEntity<>(filterAttributes(schema, existingEntity, attributes, excludedAttributes), HttpStatus.OK);
			
		} 
		catch (SchemaException e) {
			logger.error("invalid schema in {} ms : {}", ( System.currentTimeMillis() -start), e.getMessage());
			return showError( 400, SCHEMA_VALIDATION + e.getMessage(), ScimErrorType.invalidSyntax );
		}
		catch ( ConstraintViolationException e) {
			logger.error("constraint violation", e);
			return showError( 409, SCHEMA_VALIDATION + e.getMessage(), ScimErrorType.uniqueness );
		}
		catch( DataException | ConfigurationException e ) {
			return showError(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
		}
	}
	
	
	
	
	protected ResponseEntity<Map<String, Object>> get(String id, HttpServletRequest request, HttpServletResponse response, Schema schema, String attributes, String excludedAttributes) {
		long start = System.currentTimeMillis();
		try {
			Map<String,Object> user = storageImplementationFactory.getStorageImplementation(schema).get(id);
		
			ResponseEntity<Map<String,Object>> result = null;
			if ( user != null ) {
				user = filterAttributes(schema, user, attributes, excludedAttributes);
				result = new ResponseEntity<>(user, HttpStatus.OK);
				response.addHeader(Constants.HEADER_LOCATION, UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request)).build().toUriString());
			}
			else {
				return showError(HttpStatus.NOT_FOUND.value(), "the resource with id " + id + " is not found");
			}
			logger.info("resource of type {} with id {} fetched in {} ms", schema.getName(), id, ( System.currentTimeMillis() -start));
			return result;
		}
		catch( DataException | ConfigurationException e ) {
			return showError(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
		}
	}
	
	
	
	
	
	protected ResponseEntity<Map<String, Object>> search(Integer startIndex, Integer count, Schema schema, String filter ) {
		return search(startIndex, count, schema, filter, null, null, null, null);
	}
	
	
	
	protected ResponseEntity<Map<String, Object>> search(Integer startIndex, Integer count, Schema schema, String filter, String sortBy, String sortOrder, String attributes, String excludedAttributes ) {
		long start = System.currentTimeMillis();
		
		try {
			SearchCriteria searchCriteria = composeSearchCriteria(filter);
			Storage storage = storageImplementationFactory.getStorageImplementation(schema);	
			
			List<Map<String,Object>> dataFetched = storage.search(searchCriteria, startIndex, count, sortBy,sortOrder);
			List<Map<String,Object>> data = new ArrayList<>();
			for ( Map<String,Object> entity : dataFetched) {
				data.add(filterAttributes(schema, entity, attributes, excludedAttributes));
			}
			
			ResponseEntity<Map<String,Object>> result = null;
			
			Map<String,Object> responseObject = new HashMap<>();
			responseObject.put(Constants.KEY_SCHEMAS, new String[] {Constants.SCHEMA_LISTRESPONSE});
			responseObject.put(Constants.KEY_STARTINDEX, startIndex);
			responseObject.put(Constants.KEY_ITEMSPERPAGE, count);
			
			responseObject.put(Constants.KEY_TOTALRESULTS,storage.count(searchCriteria) );
			responseObject.put(Constants.KEY_RESOURCES, data);
			
			result = new ResponseEntity<>(responseObject, HttpStatus.OK);
			
			logger.info("resources of type {} fetched in {} ms", schema.getName(), ( System.currentTimeMillis() -start));
			
			return result;
		}
		catch ( InvalidFilterException ife ) {
			return showError(HttpStatus.BAD_REQUEST.value(), "the filter [" + filter + "] is not correct : " + ife.getMessage(), ScimErrorType.invalidFilter);
		}
		catch( DataException | ConfigurationException e) {
			return showError(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage() );
		}
		
		
	}
	
	
	
	private SearchCriteria composeSearchCriteria(String filter) throws InvalidFilterException {
		SearchCriteria searchCriteria = new SearchCriteria();
		if ( !StringUtils.isEmpty(filter)) {
			if ( filter.contains(Constants.AND_WITH_SPACES)) {
				String[] filterComponent = filter.split(Constants.AND_WITH_SPACES);
				for ( String c : filterComponent) {
					searchCriteria.getCriteria().add(extractCriteriumFromString(c, searchCriteria));
				}
			}
			else {
				searchCriteria.getCriteria().add(extractCriteriumFromString(filter, searchCriteria));
			}
		}
		return searchCriteria;
	}





	private SearchCriterium extractCriteriumFromString(String filter, SearchCriteria searchCriteria) throws InvalidFilterException {
		
		String[] filterParts = filter.split(StringUtils.SPACE);
		SearchOperation operation = null;
		if ( filterParts.length != 3 ) {
			if ( filterParts.length == 2 ) {
				operation = SearchOperation.operationFromString(filterParts[1]);
				if ( operation != SearchOperation.PRESENT ) {
					throw new InvalidFilterException("when a filterpart consists out of 2 parts, the present operator (pr) has to be used");
				}
			}
			else {
				throw new InvalidFilterException("a filterpart should consist out of 3 parts separated by a space or 2 parts if present operator (pr) is used");
			}
		}
		else {
			operation = SearchOperation.operationFromString(filterParts[1]);
		}
		if ( operation == null ) {
			throw new InvalidFilterException("no valid operator found for [" + filterParts[1] + "]");
		}
		if ( filterParts.length == 3 ) {
			String value = filterParts[2].replaceAll("\"", StringUtils.EMPTY_STRING);
			return new SearchCriterium(filterParts[0], value, operation);
		}
		else {
			return new SearchCriterium(filterParts[0], null, operation);
		}
		
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
				return showError( 400, "could not delete resource of type " + schema.getName() + " with id " + id);
			}
		}
		else {
			return showError(HttpStatus.NOT_FOUND.value(), "the resource with id " + id + " is not found");
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
		return StringUtils.EMPTY_STRING + date.getTime();
	}


	protected ResponseEntity<Map<String, Object>> showError(int status, String detail ) {
		return showError(status, detail, null);
	}

	protected ResponseEntity<Map<String, Object>> showError(int status, String detail, ScimErrorType scimType) {
		Map<String,Object> error = new HashMap<>();
		error.put(Constants.KEY_SCHEMAS, Constants.SCHEMA_ERROR);
		if ( scimType != null) {
			error.put(Constants.KEY_SCIMTYPE, scimType);
		}
		error.put(Constants.KEY_DETAIL, detail);
		error.put(Constants.KEY_STATUS, StringUtils.EMPTY_STRING + status );
		return new ResponseEntity<Map<String, Object>>(error, HttpStatus.valueOf(status));
	}
	
	
	

	protected Map<String, Object> filterAttributes(Schema schema, Map<String, Object> entity, String attributes, String excludedAttributes) {
		Map<String,Object> copy = new HashMap<>();
		copy.putAll(entity);
		List<String> includeList = getListFromString(attributes);
		List<String> excludeList = getListFromString(excludedAttributes);
		for ( SchemaAttribute attribute : schema.getAttributes()) {
			if ( attribute.getReturned().equalsIgnoreCase(Constants.RETURNED_NEVER)) {
				copy.remove(attribute.getName());
			}
			if ( includeList != null && includeList.size() > 0) {
				if (!attribute.getReturned().equalsIgnoreCase(Constants.RETURNED_ALWAYS) && !includeList.contains(attribute.getName())) {
					copy.remove(attribute.getName());
				}
			}
			else if ( excludeList != null && excludeList.size() > 0){
				if (!attribute.getReturned().equalsIgnoreCase(Constants.RETURNED_ALWAYS) && excludeList.contains(attribute.getName())) {
					copy.remove(attribute.getName());
				}
			}
		}
		
		
		//meta is not in the schema?
		if ( includeList != null && includeList.size() > 0) {
			if (!includeList.contains(Constants.KEY_META)) {
				copy.remove(Constants.KEY_META);
			}
		}
		else if ( excludeList != null && excludeList.size() > 0){
			if ( excludeList.contains(Constants.KEY_META)) {
				copy.remove(Constants.KEY_META);
			}
		}
		
		
		return copy;
	}
	
	
	
	private List<String> getListFromString(String attributes) {
		if ( !StringUtils.isEmpty(attributes)) {
			return Arrays.asList(attributes.split(StringUtils.COMMA));
		}
		return null;
	}





	protected String createId(Map<String, Object> user) {
		Object id = user.get(Constants.ID);
		if ( allowIdOnCreate) {
			return id != null ? id.toString() : UUID.randomUUID().toString();
		}
		else {
			return UUID.randomUUID().toString();
		}
	}
	
	
	
	protected List<String> extractSchemas(Map<String, Object> user) {
		return (List<String>)user.get(Constants.KEY_SCHEMAS);
	}
	
	
	protected ResponseEntity<Map<String, Object>> invalidSchemaForResource(List<String> schemas, String resourceType) {
		return showError( 400, "schemas contains no " + resourceType +  " schema " + schemas.toString(), ScimErrorType.invalidSyntax );
	}

	
	



}
