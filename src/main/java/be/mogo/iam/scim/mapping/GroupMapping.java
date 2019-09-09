package be.mogo.iam.scim.mapping;

import java.util.Date;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import be.mogo.iam.scim.schema.SchemaException;
import be.mogo.iam.scim.schema.SchemaReader;
import be.mogo.iam.scim.storage.StorageImplementationFactory;
import be.mogo.iam.scim.util.Constants;

/**
 * Group mappings
 */
@RestController
public class GroupMapping extends Mapping {

	private static final Logger logger = LogManager.getLogger(GroupMapping.class);
	
	
	@Autowired
	private StorageImplementationFactory storageImplementationFactory;
	
	
	
	@PostMapping(path="/scim/v2/Groups", produces = "application/scim+json")
	public ResponseEntity<Map<String, Object>> post(@RequestBody Map<String,Object> group, HttpServletRequest request, HttpServletResponse response ) {
		long start = System.currentTimeMillis();
		
		List<String> schemas = (List<String>)group.get(Constants.KEY_SCHEMAS);
		if ( schemas.contains(Constants.SCHEMA_GROUP)) {
			try {
				//validate
				SchemaReader.getInstance().validate(Constants.SCHEMA_GROUP, group);
				//id
				String id = UUID.randomUUID().toString();
				group.put(Constants.ID, id);
				String location = UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request)).build().toUriString() + Constants.SLASH + id;
				//create meta			
				createMeta( new Date(), id, group, Constants.RESOURCE_TYPE_GROUP, location);
			
				response.addHeader(Constants.HEADER_LOCATION, location);
				
				storageImplementationFactory.getStorageImplementation().put(id, group);
				
				ResponseEntity<Map<String, Object>> result = new ResponseEntity<Map<String, Object>>(group, HttpStatus.CREATED);
				logger.info("group created in {} ms", ( System.currentTimeMillis() -start));
				
				return result;
				
			} 
			catch (SchemaException e) {
				logger.error("invalid schema", e);
				return showError( 400, "schema validation : " + e.getMessage(), null );
			}
		}
		else {
			return showError( 400, "schemas contains no group schema " + Constants.SCHEMA_GROUP, null );
		}
		
	}
	

	


	@GetMapping(path="/scim/v2/Groups/{id}", produces = "application/scim+json")
	public ResponseEntity<Map<String,Object>> get(@PathVariable String id , HttpServletRequest request, HttpServletResponse response ) {
		
		long start = System.currentTimeMillis();
		
		Map<String,Object> m = storageImplementationFactory.getStorageImplementation().get(id);
		
		ResponseEntity<Map<String,Object>> result = null;
		if ( m != null ) {
			result = new ResponseEntity<Map<String,Object>>(m, HttpStatus.OK);
			response.addHeader(Constants.HEADER_LOCATION, UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request)).build().toUriString());
		}
		else {
			result = new ResponseEntity<Map<String,Object>>(HttpStatus.NOT_FOUND);
		}
		logger.info("group with id {} fetched in {} ms", id, ( System.currentTimeMillis() -start));
		
		return result;
		
	}
	
	
	
	@GetMapping(path="/scim/v2/Groups", produces = "application/scim+json")
	public ResponseEntity<Map<String,Object>> search(@PathVariable String id , HttpServletRequest request, HttpServletResponse response ) {
		
		long start = System.currentTimeMillis();
		
		Map<String,Object> m = storageImplementationFactory.getStorageImplementation().get(id);
		
		ResponseEntity<Map<String,Object>> result = null;
		if ( m != null ) {
			result = new ResponseEntity<Map<String,Object>>(m, HttpStatus.OK);
			response.addHeader(Constants.HEADER_LOCATION, UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request)).build().toUriString());
		}
		else {
			result = new ResponseEntity<Map<String,Object>>(HttpStatus.NOT_FOUND);
		}
		logger.info("group with id {} fetched in {} ms", id, ( System.currentTimeMillis() -start));
		
		return result;
		
	}
	
	
	
	
	
	
	/**
	 * Deletes the user
	 * @param id
	 * @return
	 */
	@DeleteMapping(path="/scim/v2/Groups/{id}")
	public ResponseEntity<?> delete(@PathVariable String id ) {
		long start = System.currentTimeMillis();
		
		Map<String,Object> m = storageImplementationFactory.getStorageImplementation().get(id);
		
		ResponseEntity<?> result = null;
		if ( m != null ) {
			boolean deleted = storageImplementationFactory.getStorageImplementation().delete(id);
			if ( deleted ) {
				result = new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
			}
			else {
				return showError( 400, "could not delete group with id " + id, null );
			}
		}
		else {
			result = new ResponseEntity<Map<String,Object>>(HttpStatus.NOT_FOUND);
		}
		logger.info("group with id {} deleted in {} ms", id, ( System.currentTimeMillis() -start));
		
		return result;
	}
	
	
	
	
	
	
}