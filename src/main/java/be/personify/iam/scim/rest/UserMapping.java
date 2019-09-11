package be.personify.iam.scim.rest;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import be.personify.iam.scim.schema.SchemaException;
import be.personify.iam.scim.schema.SchemaReader;
import be.personify.iam.scim.storage.ConstraintViolationException;
import be.personify.iam.scim.storage.StorageImplementationFactory;
import be.personify.iam.scim.util.Constants;
import be.personify.iam.scim.util.ScimErrorType;

/**
 * User mappings
 */
@RestController
public class UserMapping extends Mapping {

	private static final Logger logger = LogManager.getLogger(UserMapping.class);
	
	
	@Autowired
	private StorageImplementationFactory storageImplementationFactory;
	
	
	/**
	 * POST a user
	 * @param user
	 * @param request
	 * @param response
	 * @return
	 */
	@PostMapping(path="/scim/v2/Users", produces = "application/scim+json")
	public ResponseEntity<Map<String, Object>> post(@RequestBody Map<String,Object> user, HttpServletRequest request, HttpServletResponse response ) {
		long start = System.currentTimeMillis();
		
		List<String> schemas = (List<String>)user.get(Constants.KEY_SCHEMAS);
		if ( schemas.contains(Constants.SCHEMA_USER)) {
			try {
				//validate
				SchemaReader.getInstance().validate(Constants.SCHEMA_USER, user);
				//id
				String id = UUID.randomUUID().toString();
				user.put(Constants.ID, id);
				String location = UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request)).build().toUriString() + Constants.SLASH + id;
				//create meta			
				createMeta( new Date(), id, user, Constants.RESOURCE_TYPE_USER, location);
			
				response.addHeader(Constants.HEADER_LOCATION, location);
				
				storageImplementationFactory.getStorageImplementation(Constants.RESOURCE_TYPE_USER).put(id, user);
				
				user = filterResponse(Constants.RESOURCE_TYPE_USER, user);
				
				ResponseEntity<Map<String, Object>> result = new ResponseEntity<Map<String, Object>>(user, HttpStatus.CREATED);
				logger.info("user with id {} created in {} ms", id, ( System.currentTimeMillis() -start));
				
				return result;
				
			} 
			catch (SchemaException e) { 
				logger.error("invalid schema in {} ms : {}", ( System.currentTimeMillis() -start), e.getMessage());
				return showError( 400, "schema validation : " + e.getMessage(), ScimErrorType.invalidSyntax );
			}
			catch ( ConstraintViolationException e) {
				logger.error("constraint violation in {} ms : {}", ( System.currentTimeMillis() -start), e.getMessage());
				return showError( 409, "schema validation : " + e.getMessage(), ScimErrorType.uniqueness );
			}
		}
		else {
			return showError( 400, "schemas contains no user schema " + Constants.SCHEMA_USER, ScimErrorType.invalidSyntax );
		}
		
	}
	
	
	


	/**
	 * PUT a user
	 * @param id
	 * @param user
	 * @param request
	 * @param response
	 * @return
	 */
	@PutMapping(path="/scim/v2/Users/{id}", produces = "application/scim+json")
	public ResponseEntity<Map<String, Object>> put(@PathVariable String id , @RequestBody Map<String,Object> user, HttpServletRequest request, HttpServletResponse response ) {
		long start = System.currentTimeMillis();
		
		List<String> schemas = (List<String>)user.get(Constants.KEY_SCHEMAS);
		if ( schemas.contains(Constants.SCHEMA_USER)) {
			try {
				//validate
				SchemaReader.getInstance().validate(Constants.SCHEMA_USER, user);
				//check id
				if ( !user.get(Constants.ID).equals(id)){
					return showError( 400, "invalid id given in the put", null );
				};
				String location = UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request)).build().toUriString();
				//create meta	
				
				Map<String,Object> existingUser = storageImplementationFactory.getStorageImplementation(Constants.RESOURCE_TYPE_USER).get(id);
				if ( existingUser != null ) {
					//TODO check etag version and throw exception if no match
					user.put(Constants.KEY_META, existingUser.get(Constants.KEY_META));
					//perform delta
				}
				else {
					return showError( 404, "user with id " + id + " can not be updated", null );
				}
				
				createMeta( new Date(), id, user, Constants.RESOURCE_TYPE_USER, location);
			
				response.addHeader(Constants.HEADER_LOCATION, location);
				
				storageImplementationFactory.getStorageImplementation(Constants.RESOURCE_TYPE_USER).put(id, user);
				
				ResponseEntity<Map<String, Object>> result = new ResponseEntity<Map<String, Object>>(user, HttpStatus.OK);
				logger.info("user updated in {} ms", ( System.currentTimeMillis() -start));
				
				return result;
				
			} 
			catch (SchemaException e) {
				logger.error("invalid schema", e);
				return showError( 400, "schema validation : " + e.getMessage(), null );
			}
			catch ( ConstraintViolationException e) {
				logger.error("constraint violation", e);
				return showError( 409, "schema validation : " + e.getMessage(), ScimErrorType.uniqueness );
			}
		}
		else {
			return showError( 400, "schemas contains no user schema " + Constants.SCHEMA_USER, null );
		}
		
	}
	

	

	/**
	 * GET a user
	 * @param id
	 * @param request
	 * @param response
	 * @return
	 */
	@GetMapping(path="/scim/v2/Users/{id}", produces = "application/scim+json")
	public ResponseEntity<Map<String,Object>> get(@PathVariable String id , HttpServletRequest request, HttpServletResponse response ) {
		
		long start = System.currentTimeMillis();
		
		Map<String,Object> m = storageImplementationFactory.getStorageImplementation(Constants.RESOURCE_TYPE_USER).get(id);
		
		ResponseEntity<Map<String,Object>> result = null;
		if ( m != null ) {
			result = new ResponseEntity<Map<String,Object>>(m, HttpStatus.OK);
			response.addHeader(Constants.HEADER_LOCATION, UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request)).build().toUriString());
		}
		else {
			result = new ResponseEntity<Map<String,Object>>(HttpStatus.NOT_FOUND);
		}
		logger.info("user with id {} fetched in {} ms", id, ( System.currentTimeMillis() -start));
		
		return result;
		
	}
	
	
	/**
	 * SEARCH users
	 * @param startIndex
	 * @param count
	 * @param request
	 * @param response
	 * @return
	 */
	@GetMapping(path="/scim/v2/Users", produces = "application/scim+json")
	public ResponseEntity<Map<String,Object>> search(
			@RequestParam(required = false, name = "startIndex", defaultValue = "1") Integer startIndex, 
			@RequestParam(required = false, name="count", defaultValue = "200") Integer count, 
			HttpServletRequest request, HttpServletResponse response ) {
		
		long start = System.currentTimeMillis();
				
		List<Map<String,Object>> data = storageImplementationFactory.getStorageImplementation(Constants.RESOURCE_TYPE_USER).getAll();
		
		ResponseEntity<Map<String,Object>> result = null;
		
		Map<String,Object> responseObject = new HashMap<String, Object>();
		responseObject.put(Constants.KEY_SCHEMAS, new String[] {Constants.SCHEMA_LISTRESPONSE});
		responseObject.put(Constants.KEY_STARTINDEX, startIndex);
		responseObject.put(Constants.KEY_ITEMSPERPAGE, count);
		
		List<Map<String,Object>> sublist = data.subList(startIndex -1, count);
		responseObject.put(Constants.KEY_TOTALRESULTS, data.size());
		responseObject.put(Constants.KEY_RESOURCES, sublist);
		
		result = new ResponseEntity<Map<String,Object>>(responseObject, HttpStatus.OK);
		
		logger.info("users fetched in {} ms", ( System.currentTimeMillis() -start));
		
		return result;
		
	}
	
	
	
	
	/**
	 * Deletes the user
	 * @param id
	 * @return
	 */
	@DeleteMapping(path="/scim/v2/Users/{id}")
	public ResponseEntity<?> delete(@PathVariable String id ) {
		long start = System.currentTimeMillis();
		
		Map<String,Object> m = storageImplementationFactory.getStorageImplementation(Constants.RESOURCE_TYPE_USER).get(id);
		
		ResponseEntity<?> result = null;
		if ( m != null ) {
			boolean deleted = storageImplementationFactory.getStorageImplementation(Constants.RESOURCE_TYPE_USER).delete(id);
			if ( deleted ) {
				result = new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			}
			else {
				return showError( 400, "could not delete user with id " + id, null );
			}
		}
		else {
			result = new ResponseEntity<Map<String,Object>>(HttpStatus.NOT_FOUND);
		}
		logger.info("user with id {} deleted in {} ms", id, ( System.currentTimeMillis() -start));
		
		return result;
	}
	
	
	
	
	
	
}