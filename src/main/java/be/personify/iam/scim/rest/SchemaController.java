package be.personify.iam.scim.rest;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
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

import be.personify.iam.scim.schema.Schema;
import be.personify.iam.scim.schema.SchemaReader;

/**
 * Scim mappings
 */
@RestController
public class SchemaController extends Controller {
	
	
	/**
	 * POST a entity
	 * @param entity
	 * @param request
	 * @param response
	 * @return
	 */
	@PostMapping(path="/scim/v2/{resourceType}s", produces = {"application/scim+json","application/json"})
	public ResponseEntity<Map<String, Object>> post(@PathVariable String resourceType, 
													@RequestBody Map<String,Object> entity, 
													HttpServletRequest request, 
													HttpServletResponse response ) {
		Schema schema = SchemaReader.getInstance().getSchemaByResourceType(resourceType);
		if ( schema != null ) {
			List<String> schemas = extractSchemas(entity);
			if ( schemas != null && schemas.size() > 0) {
				if ( schemas.contains(schema.getId())) {
					return post(entity, request, response, schema);
				}
				return invalidSchemaForResource(schemas, resourceType);
			}
			else {
				return post(entity, request, response, schema);
			}
		}
		return new ResponseEntity<Map<String,Object>>(HttpStatus.NOT_FOUND);
	}


	/**
	 * PUT a entity
	 * @param id
	 * @param entity
	 * @param request
	 * @param response
	 * @return
	 */
	@PutMapping(path="/scim/v2/{resourceType}s/{id}", produces = {"application/scim+json","application/json"})
	public ResponseEntity<Map<String, Object>> put(@PathVariable String resourceType,
													@PathVariable String id , 
													@RequestBody Map<String,Object> entity, 
													HttpServletRequest request, 
													HttpServletResponse response ) {
		Schema schema = SchemaReader.getInstance().getSchemaByResourceType(resourceType);
		if ( schema != null ) {
			List<String> schemas = extractSchemas(entity);
			if ( schemas != null && schemas.size() > 0) {
				if ( schemas.contains(schema.getId())) {
					return put(id, entity, request, response, schema);
				}
				return invalidSchemaForResource(schemas, resourceType);
			}
			else {
				return put(id, entity, request, response, schema);
			}
		}
		return new ResponseEntity<Map<String,Object>>(HttpStatus.NOT_FOUND);
	}


	
	
	/**
	 * PATCH a entity
	 * @param id
	 * @param enitty
	 * @param request
	 * @param response
	 * @return
	 */
	@PatchMapping(path="/scim/v2/{resourceType}s/{id}", produces = {"application/scim+json","application/json"})
	public ResponseEntity<Map<String, Object>> patch(@PathVariable String resourceType,
														@PathVariable String id , 
														@RequestBody Map<String,Object> entity, 
														HttpServletRequest request, 
														HttpServletResponse response ) {
		Schema schema = SchemaReader.getInstance().getSchemaByResourceType(resourceType);
		if ( schema != null ) {
			List<String> schemas = extractSchemas(entity);
			if ( schemas.contains(schema.getId())) {
				return patch(id, entity, request, response, schema);
			}
			return invalidSchemaForResource(schemas, resourceType);
		}
		return new ResponseEntity<Map<String,Object>>(HttpStatus.NOT_FOUND);
	}


	

	/**
	 * GET a entity
	 * @param id
	 * @param request
	 * @param response
	 * @return
	 */
	@GetMapping(path="/scim/v2/{resourceType}s/{id}", produces = {"application/scim+json","application/json"})
	public ResponseEntity<Map<String,Object>> get(@PathVariable String resourceType, 
													@PathVariable String id , 
													HttpServletRequest request, 
													HttpServletResponse response ) {
		Schema schema = SchemaReader.getInstance().getSchemaByResourceType(resourceType);
		if ( schema != null ) {
			return get(id, request, response, schema);
		}
		return new ResponseEntity<Map<String,Object>>(HttpStatus.NOT_FOUND);
	}


	
	
	/**
	 * SEARCH entities
	 * @param startIndex
	 * @param count
	 * @param request
	 * @param response
	 * @return
	 */
	@GetMapping(path="/scim/v2/{resourceType}s", produces = {"application/scim+json","application/json"})
	public ResponseEntity<Map<String,Object>> search( 
			@PathVariable String resourceType,
			@RequestParam(required = false, name = "startIndex", defaultValue = "1") Integer startIndex, 
			@RequestParam(required = false, name="count", defaultValue = "200") Integer count, 
			@RequestParam(required = false, name="filter") String filter,
			HttpServletRequest request, HttpServletResponse response ) {
		Schema schema = SchemaReader.getInstance().getSchemaByResourceType(resourceType);
		if (schema != null ) {
			return search(startIndex, count, schema);
		}
		return new ResponseEntity<Map<String,Object>>(HttpStatus.NOT_FOUND);
	}



	
	
	/**
	 * Deletes the entity
	 * @param id
	 * @return
	 */
	@DeleteMapping(path="/scim/v2/{resourceType}s/{id}")
	public ResponseEntity<?> delete(@PathVariable String resourceType, @PathVariable String id ) {
		Schema schema = SchemaReader.getInstance().getSchemaByResourceType(resourceType);
		if (schema != null ) {
			return delete(id, schema);
		}
		return new ResponseEntity<Map<String,Object>>(HttpStatus.NOT_FOUND);
	}
	
	
	
	
	
}