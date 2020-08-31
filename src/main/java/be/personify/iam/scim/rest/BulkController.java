package be.personify.iam.scim.rest;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import be.personify.iam.scim.schema.Schema;
import be.personify.iam.scim.schema.SchemaException;
import be.personify.iam.scim.schema.SchemaReader;
import be.personify.iam.scim.storage.ConstraintViolationException;
import be.personify.iam.scim.storage.StorageImplementationFactory;
import be.personify.iam.scim.util.Constants;

/**
 * Controller managing bulk operations
 */
@RestController
public class BulkController extends Controller {

	private static final Logger logger = LogManager.getLogger(BulkController.class);

	private static final String SCHEMA = Constants.SCHEMA_BULKREQUEST; 
	
	@Autowired
	private StorageImplementationFactory storageImplementationFactory;

	
	@PostMapping(path="/scim/v2/Bulk", produces = {"application/scim+json","application/json"})
	public ResponseEntity<Map<String, Object>> post(@RequestBody Map<String,Object> objects, HttpServletRequest request, HttpServletResponse response ) {
		List<String> schemas = extractSchemas(objects);
		if ( schemas.contains(SCHEMA)) {
			return postBulk(objects, request, response);
		}
		return invalidSchemaForResource(schemas, null);
	} 
	
	
	
	
	protected ResponseEntity<Map<String, Object>> postBulk(Map<String, Object> bulk, HttpServletRequest request, HttpServletResponse response) {
		
		long start = System.currentTimeMillis();
		List<Map<String,Object>> operations = (List<Map<String,Object>>)bulk.get(Constants.KEY_OPERATIONS);
		List<Map<String,Object>> resultOperations = new ArrayList<Map<String,Object>>();
		
		String method = null;
		String bulkId = null;
		String path = null;

		
		for ( Map<String,Object> operation : operations ) {
			Map<String,Object> entity = (Map<String,Object>)operation.get(Constants.KEY_DATA);
			List<String> schemas = extractSchemas(entity);
			Schema schema = SchemaReader.getInstance().getSchema(schemas.get(0));
			method = (String)operation.get(Constants.KEY_METHOD);
			bulkId = (String)operation.get(Constants.KEY_BULKID);
			path = (String)operation.get(Constants.KEY_PATH); 
			logger.info("operation {} {} {}", method, path, bulkId);
			
			Map<String,Object> operationResult = new HashMap<String, Object>();
			
			
			if ( method.equalsIgnoreCase(Constants.HTTP_METHOD_POST)) {
				try {
					SchemaReader.getInstance().validate(schema,entity, true);
					String id = createId(entity);
					entity.put(Constants.ID, id);
					String location = UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request)).build().toUriString() + Constants.SLASH + id;
					Date now = new Date();
					createMeta( now, id, entity, schema.getName(), location);
					storageImplementationFactory.getStorageImplementation(schema).create(id, entity);
					operationResult = composeResultMap(method, bulkId, HttpStatus.CREATED);
					operationResult.put(Constants.KEY_LOCATION, location);
					operationResult.put(Constants.KEY_VERSION, createVersion(now));
				}
				catch( SchemaException se ) {
					logger.error("error validating", se);
					operationResult = composeResultMap(method, bulkId, HttpStatus.BAD_REQUEST);
				} 
				catch (ConstraintViolationException e) {
					logger.error("constraint error", e);
					operationResult = composeResultMap(method, bulkId, HttpStatus.BAD_REQUEST);
				}
			}
			
			resultOperations.add(operationResult);
		}
		
		Map<String,Object> result = new HashMap<String, Object>();
		result.put(Constants.KEY_SCHEMAS, new String[] { Constants.SCHEMA_BULKRESPONSE });
		result.put(Constants.KEY_OPERATIONS, resultOperations);
		
		
		return new ResponseEntity<Map<String,Object>>(result, HttpStatus.OK);
	}
	
	
	
	private Map<String,Object> composeResultMap( String method, String bulkId, HttpStatus status ){
		Map<String,Object> result = new HashMap<String, Object>();
		result.put(Constants.KEY_METHOD, method);
		result.put(Constants.KEY_BULKID, bulkId);
		Map<String,String> statusMap = new HashMap<String, String>();
		statusMap.put(Constants.KEY_CODE, Constants.EMPTY + status.value());
		result.put(Constants.KEY_STATUS, statusMap);
		return result;
	}
	
	
	
}