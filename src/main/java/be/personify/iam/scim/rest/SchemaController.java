/*
*     Copyright 2019-2022 Wouter Van der Beken @ https://personify.be
*
* Generated software by personify.be

* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*          http://www.apache.org/licenses/LICENSE-2.0
*
 * Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package be.personify.iam.scim.rest;

import be.personify.iam.scim.schema.Schema;
import be.personify.iam.scim.schema.SchemaReader;
import be.personify.iam.scim.schema.SchemaResourceType;
import be.personify.iam.scim.util.Constants;
import be.personify.iam.scim.util.ScimErrorType;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

/**
 * Schema controller for the SCIM server implementation
 *
 * @author wouter
 */
@RestController
public class SchemaController extends Controller {

	@Autowired
	private SchemaReader schemaReader;
	
	private static final Logger logger = LogManager.getLogger(SchemaController.class);
	
	
	@Value("${scim.validationEnabled:true}")
	private boolean validationEnabled;


	/**
	 * POST of a entity
	 *
	 * @param resourceType       the resourcetype for which to POST
	 * @param entity             the entity to post
	 * @param attributes         the attributes to be included from the response
	 * @param excludedAttributes the attributes to be excluded from the response
	 * @param request            the HttpServletRequest
	 * @param response           the HttpServletResponse
	 * @return the map as a ResponseEntity
	 */
	@PostMapping(path = "/scim/v2/{endpoint}", produces = { "application/scim+json", "application/json" })
	public ResponseEntity<Map<String, Object>> post(@PathVariable String endpoint, @RequestBody Map<String, Object> entity,
			@RequestParam(required = false, name = "attributes") String attributes,
			@RequestParam(required = false, name = "excludedAttributes") String excludedAttributes,
			HttpServletRequest request, HttpServletResponse response) {
		try {
			SchemaResourceType resourceType = schemaReader.getSchemaResourceTypeByEndpoint(endpoint);
			Schema schema = resourceType.getSchemaObject();
			if (schema != null) { 
				List<String> schemas = extractSchemas(entity); 
				if (schemas != null && schemas.size() > 0) {
					if (schemas.contains(schema.getId())) {
						return post(entity, request, response, resourceType, attributes, excludedAttributes);
					}
					return missingRequiredSchemaForResource(resourceType, schema.getId());
				}
				else {
					return post(entity, request, response, resourceType, attributes, excludedAttributes);
				}
			}
			return showError(HttpStatus.NOT_FOUND.value(), "the resource of type " + resourceType + " is not found", ScimErrorType.noTarget);
		}
		catch (Exception e) {
			e.printStackTrace();
			return showError(HttpStatus.NOT_FOUND.value(), "for the endpoint " + endpoint + " : " + e.getMessage() , ScimErrorType.invalidPath);
		}
	}

	/**
	 * PUT of a entity
	 *
	 * @param resourceType       the resourcetype of the entity to PUT
	 * @param id                 the id of the entity of the given resourcetype
	 * @param entity             the entity itself as a map
	 * @param attributes         the attributes to be included from the response
	 * @param excludedAttributes the attributes to be excluded from the response
	 * @param request            the HttpServletRequest
	 * @param response           the HttpServletResponse
	 * @return the map as a ResponseEntity
	 */
	@PutMapping(path = "/scim/v2/{endpoint}/{id}", produces = { "application/scim+json", "application/json" })
	public ResponseEntity<Map<String, Object>> put(@PathVariable String endpoint, @PathVariable String id,
			@RequestBody Map<String, Object> entity,
			@RequestParam(required = false, name = "attributes") String attributes,
			@RequestParam(required = false, name = "excludedAttributes") String excludedAttributes,
			HttpServletRequest request, HttpServletResponse response) {
		try {
			SchemaResourceType resourceType = schemaReader.getSchemaResourceTypeByEndpoint(endpoint); 
			Schema schema = resourceType.getSchemaObject();
			if (schema != null) {
				List<String> schemas = extractSchemas(entity);
				if (schemas != null && schemas.size() > 0) {
					if (schemas.contains(schema.getId())) {
						return put(id, entity, request, response, resourceType, attributes, excludedAttributes);
					}
					return missingRequiredSchemaForResource(resourceType, schema.getId());
				} 
				else {
					return put(id, entity, request, response, resourceType, attributes, excludedAttributes);
				}
			}
			return showError(HttpStatus.NOT_FOUND.value(), "the resource of type " + resourceType + " is not found", ScimErrorType.noTarget);
		}
		catch (Exception e) {
			return showError(HttpStatus.NOT_FOUND.value(), e.getMessage() , ScimErrorType.invalidPath);
		}
		
	}

	/**
	 * PATCH a entity
	 *
	 * @param resourceType       the resourcetype of the entity to PATCH
	 * @param id                 the id of the entity
	 * @param entity             the entity
	 * @param attributes         the attributes to be included from the response
	 * @param excludedAttributes the attributes to be excluded from the response
	 * @param request            the HttpServletRequest
	 * @param response           the HttpServletResponse
	 * @return the map as a ResponseEntity
	 */
	@PatchMapping(path = "/scim/v2/{endpoint}/{id}", produces = { "application/scim+json", "application/json" })
	public ResponseEntity<Map<String, Object>> patch(@PathVariable String endpoint, @PathVariable String id,
			@RequestBody Map<String, Object> entity,
			@RequestParam(required = false, name = "attributes") String attributes,
			@RequestParam(required = false, name = "excludedAttributes") String excludedAttributes,
			HttpServletRequest request, HttpServletResponse response) {
		try {
			SchemaResourceType resourceType = schemaReader.getSchemaResourceTypeByEndpoint(endpoint); 
			Schema schema = resourceType.getSchemaObject();
			if (schema != null) {
				List<String> schemas = extractSchemas(entity);
				if (schemas.contains(Constants.SCHEMA_PATCHOP)) {
					return patch(id, entity, request, response, resourceType, attributes, excludedAttributes);
				}
				return missingRequiredSchemaForResource(resourceType, Constants.SCHEMA_PATCHOP);
			}
			return showError(HttpStatus.NOT_FOUND.value(), "the resource of type " + endpoint + " is not found", null);
		}
		catch (Exception e) {
			return showError(HttpStatus.NOT_FOUND.value(), e.getMessage() , ScimErrorType.invalidPath);
		}
	}

	/**
	 * GETs the entity
	 *
	 * @param resourceType       the resourceType of the entity
	 * @param id                 the id of the entity
	 * @param attributes         the attributes to be included from the response
	 * @param excludedAttributes the attributes to be excluded from the response
	 * @param request            the HttpServletRequest
	 * @param response           the HttpServletResponse
	 * @return the map containing the entity
	 */
	@GetMapping(path = "/scim/v2/{endpoint}/{id}", produces = { "application/scim+json", "application/json" })
	public ResponseEntity<Map<String, Object>> get(@PathVariable String endpoint, @PathVariable String id,
			@RequestParam(required = false, name = "attributes") String attributes,
			@RequestParam(required = false, name = "excludedAttributes") String excludedAttributes,
			HttpServletRequest request, HttpServletResponse response) {
		try {
			SchemaResourceType resourceType = schemaReader.getSchemaResourceTypeByEndpoint(endpoint); 
			Schema schema = resourceType.getSchemaObject();
			if (schema != null) {
				return get(id, request, response, schema, attributes, excludedAttributes);
			}
			return showError(HttpStatus.NOT_FOUND.value(), "the resource of type " + resourceType + " is not found", null);
		}
		catch( Exception e ) {
			return showError(HttpStatus.NOT_FOUND.value(), e.getMessage() , ScimErrorType.invalidPath);
		}
	}

	/**
	 * Searches the entity
	 *
	 * @param resourceType       the resource type
	 * @param startIndex         the index to start from
	 * @param count              the number of records to return
	 * @param filter             the filter to apply
	 * @param sortBy             the sortby attribute
	 * @param sortOrder          the sortorder
	 * @param attributes         the attributes to be included from the response
	 * @param excludedAttributes the attributes to be excluded from the response
	 * @param request            the HttpServletRequest
	 * @param response           the HttpServletResponse
	 * @return the response entity
	 */
	@GetMapping(path = "/scim/v2/{endpoint}", produces = { "application/scim+json", "application/json" })
	public ResponseEntity<Map<String, Object>> search(@PathVariable String endpoint,
			@RequestParam(required = false, name = "startIndex", defaultValue = "1") Integer startIndex,
			@RequestParam(required = false, name = "count", defaultValue = "200") Integer count,
			@RequestParam(required = false, name = "filter") String filter,
			@RequestParam(required = false, name = "sortBy") String sortBy,
			@RequestParam(required = false, name = "sortOrder") String sortOrder,
			@RequestParam(required = false, name = "attributes") String attributes,
			@RequestParam(required = false, name = "excludedAttributes") String excludedAttributes,
			HttpServletRequest request, HttpServletResponse response) {
		try {
			SchemaResourceType resourceType = schemaReader.getSchemaResourceTypeByEndpoint(endpoint); 
			Schema schema = resourceType.getSchemaObject();
			if (schema != null) {
				return search(startIndex, count, schema, filter, sortBy, sortOrder, attributes, excludedAttributes);
			}
			return showError(HttpStatus.NOT_FOUND.value(), "the resource of type " + resourceType + " is not found", null);
		}
		catch( Exception e ) {
			return showError(HttpStatus.NOT_FOUND.value(), e.getMessage() , ScimErrorType.invalidPath);
		}
	}

	/**
	 * DELETEs the entity
	 *
	 * @param resourceType the resourcetype of the entity to be deleted
	 * @param id           the id of the entity to be deleted
	 * @return the entity deleted
	 */
	@DeleteMapping(path = "/scim/v2/{endpoint}/{id}")
	public ResponseEntity<?> delete(@PathVariable String endpoint, @PathVariable String id) {
		try {
			SchemaResourceType resourceType = schemaReader.getSchemaResourceTypeByEndpoint(endpoint); 
			Schema schema = resourceType.getSchemaObject();
			if (schema != null) { 
				return delete(id, schema);
			}
			return showError(HttpStatus.NOT_FOUND.value(), "the resource of type " + resourceType + " is not found", null);
		}
		catch( Exception e ) {
			return showError(HttpStatus.NOT_FOUND.value(), e.getMessage() , ScimErrorType.invalidPath);
		}

			
	}
}
