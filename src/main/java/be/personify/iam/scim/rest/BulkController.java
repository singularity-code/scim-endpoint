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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import be.personify.iam.scim.authentication.CurrentConsumer;
import be.personify.iam.scim.schema.Schema;
import be.personify.iam.scim.schema.SchemaException;
import be.personify.iam.scim.schema.SchemaReader;
import be.personify.iam.scim.schema.SchemaResourceType;
import be.personify.iam.scim.storage.ConfigurationException;
import be.personify.iam.scim.storage.ConstraintViolationException;
import be.personify.iam.scim.storage.DataException;
import be.personify.iam.scim.storage.StorageImplementationFactory;
import be.personify.iam.scim.util.Constants;
import be.personify.iam.scim.util.PropertiesUtil;
import be.personify.iam.scim.util.ScimErrorType;
import be.personify.util.StringUtils;

/**
 * Controller managing bulk operations with circular reference processing,
 * maxPayloadSize and maxOperations
 *
 * <p>
 * https://tools.ietf.org/html/rfc7644#section-3.7
 */
@RestController
public class BulkController extends Controller {

	private static final Logger logger = LogManager.getLogger(BulkController.class);

	private static final String SCHEMA = Constants.SCHEMA_BULKREQUEST;

	@Autowired
	private StorageImplementationFactory storageImplementationFactory;

	@Value("${scim.bulk.maxPayloadSize:1048576}")
	private int maxPayloadSize;

	@Value("${scim.bulk.maxOperations:1000}")
	private int maxOperations;
	
	@Value("${scim.bulk.failOnErrors.default:10}")
	private int failOnErrorsDefault;


	@PostMapping(path = "/scim/v2/Bulk", produces = { "application/scim+json", "application/json" })
	public ResponseEntity<Map<String, Object>> post(@RequestBody Map<String, Object> objects, HttpServletRequest request, HttpServletResponse response) {

		// check the maximum payload
		long contentLenth = request.getContentLength();
		if (contentLenth > maxPayloadSize) {
			return showError(HttpStatus.PAYLOAD_TOO_LARGE.value(), "The size of the bulk operation (" + contentLenth + ") exceeds the maxPayloadSize (" + maxPayloadSize + ")", null);
		}

		List<String> schemas = schemaReader.extractSchemas(objects);
		if (schemas.contains(SCHEMA)) {
			return postBulk(objects, request, response);
		}
		return showError(HttpStatus.NOT_FOUND.value(), "the schemas is not containing schema " + SCHEMA, null);
	}
	
	

	protected ResponseEntity<Map<String, Object>> postBulk(Map<String, Object> bulk, HttpServletRequest request, HttpServletResponse response) {

		long start = System.currentTimeMillis();
		
		Integer failOnErrors = failOnErrorsDefault;
		if ( bulk.get(Constants.FAIL_ON_ERRORS) != null ) {
			failOnErrors = (Integer) bulk.get(Constants.FAIL_ON_ERRORS);
		}
		Integer currentErrors = 0;
		
		
		List<Map<String, Object>> operations = (List<Map<String, Object>>) bulk.get(Constants.KEY_OPERATIONS);

		if (operations.size() > maxOperations) {
			return showError(HttpStatus.PAYLOAD_TOO_LARGE.value(), "The number of bulk operations (" + operations.size() + ") exceeds the maxOperations (" + maxOperations + ")", null);
		}

		List<Map<String, Object>> resultOperations = new ArrayList<Map<String, Object>>();

		String method = null;
		String bulkId = null;
		String path = null;

		Map<String, Object> entity = null;

		for (Map<String, Object> operation : operations) {
			
			if ( currentErrors >= failOnErrors ) {
				break;
			}
			
			entity = (Map<String, Object>) operation.get(Constants.KEY_DATA);
			List<String> schemas = schemaReader.extractSchemas(entity);
			Schema schema = schemaReader.getSchema(schemas.get(0));
			
			method = (String) operation.get(Constants.KEY_METHOD);
			bulkId = (String) operation.get(Constants.KEY_BULKID);
			path = (String) operation.get(Constants.KEY_PATH);
			logger.info("operation {} {} {}", method, path, bulkId);

			Map<String, Object> operationResult = new HashMap<String, Object>();

			if (method.equalsIgnoreCase(Constants.HTTP_METHOD_POST)) {
				try {
					SchemaResourceType resourceType = schemaReader.getResourceTypeByName(schema.getName());
					schemaReader.validate(resourceType, entity, true, request.getMethod());
					String id = createId(entity);
					entity.put(Constants.ID, id);
					String fullResourceTypeEndpoint = getFullResourceTypeEndpoint(resourceType); 
					String location = fullResourceTypeEndpoint + StringUtils.SLASH + id;
					location = StringSubstitutor.replace(location, PropertiesUtil.getPropertiesFromEnv(env));
					Date now = new Date();
					createMeta(now, id, entity, schema.getName(), location);
					storageImplementationFactory.getStorageImplementation(schema).create(id, entity, CurrentConsumer.getCurrent());
					operationResult = composeResultMap(method, bulkId, HttpStatus.CREATED.value(), null);
					operationResult.put(Constants.KEY_LOCATION, location);
					operationResult.put(Constants.KEY_VERSION, createVersion(now));
				}
				catch (DataException de) {
					//logger.error("error ", de);
					currentErrors++;
					int errorStatus = HttpStatus.INTERNAL_SERVER_ERROR.value();
					Map<String,Object> error = composeError(errorStatus, de.getMessage(), ScimErrorType.invalidValue);
					operationResult = composeResultMap(method, bulkId,errorStatus, error );
				} 
				catch (SchemaException se) {
					//logger.error("error validating", se);
					currentErrors++;
					int errorStatus = HttpStatus.BAD_REQUEST.value();
					Map<String,Object> error = composeError(errorStatus, se.getMessage(), ScimErrorType.invalidValue);
					operationResult = composeResultMap(method, bulkId, errorStatus, error);
				} 
				catch (ConstraintViolationException e) {
					//logger.error("constraint error", e);
					currentErrors++;
					int errorStatus = HttpStatus.BAD_REQUEST.value();
					Map<String,Object> error = composeError(errorStatus, e.getMessage(), ScimErrorType.invalidValue);
					operationResult = composeResultMap(method, bulkId, errorStatus, error );
				}
				catch (ConfigurationException e) {
					logger.error("configuration error", e);
					return showError(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), null);
				}
			}

			resultOperations.add(operationResult);
		}

		Map<String, Object> result = new HashMap<String, Object>();
		result.put(Constants.KEY_SCHEMAS, new String[] { Constants.SCHEMA_BULKRESPONSE });
		result.put(Constants.KEY_OPERATIONS, resultOperations);

		logger.info("operation {} {} {} completed in [{}ms]", method, path, bulkId, System.currentTimeMillis() - start);

		return new ResponseEntity<Map<String, Object>>(result, HttpStatus.OK);
	}
	
	
	

	
	

	private String getFullResourceTypeEndpoint(SchemaResourceType resourceType) {
		return env.getProperty("exposed.protocol") + "://" + env.getProperty("exposed.host") + resourceType.getEndpoint();
	}



	private Map<String, Object> composeResultMap(String method, String bulkId, int status, Map<String,Object> errorResponse) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put(Constants.KEY_METHOD, method);
		result.put(Constants.KEY_BULKID, bulkId);
		result.put(Constants.KEY_STATUS, status);
		if ( errorResponse != null) {
			result.put(Constants.KEY_RESPONSE, errorResponse);
		}
		return result;
	}
	
	
	
	
	
}


