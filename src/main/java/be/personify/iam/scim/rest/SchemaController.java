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
 * Schema controller
 * @author wouter
 *
 */
@RestController
public class SchemaController extends Controller {
	
	
	/**
	 * POST of a entity
	 * @param resourceType the resourcetype for which to POST
	 * @param entity the entity to post
	 * @param request the HttpServletRequest
	 * @param response the HttpServletResponse
	 * @return the map as a ResponseEntity
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
		return showError(HttpStatus.NOT_FOUND.value(), "the resource of type " + resourceType + " is not found", null);
	}


	/**
	 * PUT of a entity
	 * @param resourceType the resourcetype of the entity to PUT
	 * @param id the id of the entity of the given resourcetype
	 * @param entity the entity itself as a map
	 * @param request the HttpServletRequest
	 * @param response the HttpServletResponse
	 * @return the map as a ResponseEntity
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
		return showError(HttpStatus.NOT_FOUND.value(), "the resource of type " + resourceType + " is not found", null);
	}


	
	
	/**
	 * PATCH a entity
	 * @param resourceType the resourcetype of the entity to PATCH
	 * @param id the id of the entity
	 * @param entity the entity
	 * @param request the HttpServletRequest
	 * @param response the HttpServletResponse
	 * @return the map as a ResponseEntity
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
		return showError(HttpStatus.NOT_FOUND.value(), "the resource of type " + resourceType + " is not found", null);
	}


	

	/**
	 * GETs the entity
	 * @param resourceType the resourceType of the entity
	 * @param id the id of the entity
	 * @param request the HttpServletRequest
	 * @param response the HttpServletResponse
	 * @return the map containing the entity
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
		return showError(HttpStatus.NOT_FOUND.value(), "the resource of type " + resourceType + " is not found", null);
	}


	
	
	/**
	 * Searches teh entity
	 * @param resourceType the resourcetype of the entities to be found
	 * @param startIndex the starindex ( default 1 )
	 * @param count the count of the entities to be returned
	 * @param filter the filter to be used
	 * @param request the HttpServletRequest
	 * @param response the HttpSevletResponse
	 * @return the entities found
	 */
	@GetMapping(path="/scim/v2/{resourceType}s", produces = {"application/scim+json","application/json"})
	public ResponseEntity<Map<String,Object>> search( 
			@PathVariable String resourceType,
			@RequestParam(required = false, name = "startIndex", defaultValue = "1") Integer startIndex, 
			@RequestParam(required = false, name="count", defaultValue = "200") Integer count, 
			@RequestParam(required = false, name="filter") String filter,
			@RequestParam(required = false, name="sortBy") String sortBy,
			@RequestParam(required = false, name="sortOrder") String sortOrder,
			HttpServletRequest request, HttpServletResponse response ) {
		Schema schema = SchemaReader.getInstance().getSchemaByResourceType(resourceType);
		if (schema != null ) {
			return search(startIndex, count, schema, filter, sortBy, sortOrder);
		}
		return showError(HttpStatus.NOT_FOUND.value(), "the resource of type " + resourceType + " is not found", null);
	}




	
	/**
	 * DELETEs the entity
	 * @param resourceType the resourcetype of the entity to be deleted
	 * @param id the id of the entity to be deleted
	 * @return the entity deleted
	 */
	@DeleteMapping(path="/scim/v2/{resourceType}s/{id}")
	public ResponseEntity<?> delete(@PathVariable String resourceType, @PathVariable String id ) {
		Schema schema = SchemaReader.getInstance().getSchemaByResourceType(resourceType);
		if (schema != null ) {
			return delete(id, schema);
		}
		return showError(HttpStatus.NOT_FOUND.value(), "the resource of type " + resourceType + " is not found", null);
	}
	
	
	
	
	
}