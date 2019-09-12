package be.personify.iam.scim.rest;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import be.personify.iam.scim.util.Constants;

/**
 * User mappings
 */
@RestController
public class UserMapping extends Mapping {
	
	private static final String RESOURCE_TYPE = Constants.RESOURCE_TYPE_USER;
	private static final String SCHEMA = Constants.SCHEMA_USER;
	
	
	/**
	 * POST a user
	 * @param user
	 * @param request
	 * @param response
	 * @return
	 */
	@PostMapping(path="/scim/v2/Users", produces = "application/scim+json")
	public ResponseEntity<Map<String, Object>> post(@RequestBody Map<String,Object> user, HttpServletRequest request, HttpServletResponse response ) {
		List<String> schemas = extractSchemas(user);
		if ( schemas.contains(SCHEMA)) {
			return post(user, request, response, SCHEMA, RESOURCE_TYPE);
		}
		return invalidSchemaForResource(SCHEMA, RESOURCE_TYPE);
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
		List<String> schemas = extractSchemas(user);
		if ( schemas.contains(SCHEMA)) {
			return put(id, user, request, response, SCHEMA, RESOURCE_TYPE);
		}
		return invalidSchemaForResource(SCHEMA, RESOURCE_TYPE);
	}


	
	
	/**
	 * PATCH a user
	 * @param id
	 * @param user
	 * @param request
	 * @param response
	 * @return
	 */
	@PatchMapping(path="/scim/v2/Users/{id}", produces = "application/scim+json")
	public ResponseEntity<Map<String, Object>> patch(@PathVariable String id , @RequestBody Map<String,Object> user, HttpServletRequest request, HttpServletResponse response ) {
		List<String> schemas = extractSchemas(user);
		if ( schemas.contains(SCHEMA)) {
			return patch(id, user, request, response,SCHEMA, RESOURCE_TYPE );
		}
		return invalidSchemaForResource(SCHEMA, RESOURCE_TYPE);
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
		return get(id, request, response, RESOURCE_TYPE);
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
		return search(startIndex, count, RESOURCE_TYPE);
	}



	
	
	/**
	 * Deletes the user
	 * @param id
	 * @return
	 */
	@DeleteMapping(path="/scim/v2/Users/{id}")
	public ResponseEntity<?> delete(@PathVariable String id ) {
		return delete(id, RESOURCE_TYPE);
	}
	
	
	
	
	
}