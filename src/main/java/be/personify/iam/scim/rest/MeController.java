package be.personify.iam.scim.rest;

import java.util.ArrayList;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;
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
import be.personify.iam.scim.util.Constants;

/**
 * Me controller for the SCIM server
 * @author wouter
 *
 */
@RestController
public class MeController extends Controller {

	private static final Logger logger = LogManager.getLogger(MeController.class);
	
	private static final Schema schema = SchemaReader.getInstance().getSchemaByResourceType("User");
	
	
	
	@PostMapping(path="/scim/v2/Me", produces = {"application/scim+json","application/json"})
	public ResponseEntity<Map<String, Object>> post(@RequestBody Map<String,Object> entity,
													@RequestParam(required = false, name="attributes") String attributes,
													@RequestParam(required = false, name="excludedAttributes") String excludedAttributes,
													HttpServletRequest request, 
													HttpServletResponse response ) {	
		return showError(HttpStatus.NOT_IMPLEMENTED.value(), "the post to the /Me endpoint is not yet implemented", null);
	}


	
	@PutMapping(path="/scim/v2/Me", produces = {"application/scim+json","application/json"})
	public ResponseEntity<Map<String, Object>> put(	@RequestBody Map<String,Object> entity, 
													@RequestParam(required = false, name="attributes") String attributes,
													@RequestParam(required = false, name="excludedAttributes") String excludedAttributes,
													HttpServletRequest request, 
													HttpServletResponse response ) {
		return showError(HttpStatus.NOT_IMPLEMENTED.value(), "the put against the /Me endpoint is not yet implemented", null);
	}


	
	@PatchMapping(path="/scim/v2/Me", produces = {"application/scim+json","application/json"})
	public ResponseEntity<Map<String, Object>> patch( @RequestBody Map<String,Object> entity,
														@RequestParam(required = false, name="attributes") String attributes,
														@RequestParam(required = false, name="excludedAttributes") String excludedAttributes,
														HttpServletRequest request, 
														HttpServletResponse response ) {
		return showError(HttpStatus.NOT_IMPLEMENTED.value(), "the patch against the /Me endpoint is not yet implemented", null);
	}


	
	@GetMapping(path="/scim/v2/Me", produces = {"application/scim+json","application/json"})
	public ResponseEntity<Map<String,Object>> get( @RequestParam(required = false, name="attributes") String attributes,
													@RequestParam(required = false, name="excludedAttributes") String excludedAttributes,
													HttpServletRequest request, 
													HttpServletResponse response ) {
		Map<String,Object> result = getAndValidateUserName(request,schema);
		if ( !StringUtils.isEmpty(result)) {
			ResponseEntity responseEntity = new ResponseEntity<>(result, HttpStatus.OK);
			//responseEntity.getHeaders().add("Location", "zizi");
			return responseEntity;
		}
		else {
			return showError(HttpStatus.UNAUTHORIZED.value(), "no valid authorization subject found", null);
		}
	}



	
	



	@DeleteMapping(path="/scim/v2/Me")
	public ResponseEntity<?> delete(@PathVariable String resourceType, @PathVariable String id ) {
		return showError(HttpStatus.NOT_IMPLEMENTED.value(), "the delete against the /Me endpoint is not yet implemented", null);
	}
	
	
	
	
	
	
	

	private Map<String,Object> getAndValidateUserName(HttpServletRequest request, Schema schema ) {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if ( header != null ) {
			String[] auth = header.split(Constants.SPACE);
			if ( auth.length == 2) {
				if ( auth[0].equalsIgnoreCase(Constants.BASIC)) {
					String credential = new String(Base64Utils.decode(auth[1].getBytes()));
					String[] parts = credential.split(Constants.COLON);
					if ( parts.length == 2) {
						String userName = parts[0];
						String password = parts[1];
						logger.info("userName {}", userName);
						ResponseEntity<Map<String, Object>> result = search(1, 10, schema, "userName eq " + userName + " and password eq " + password);
						if ( (Long)result.getBody().get("totalResults") == 1) {
							logger.info("ok {}", result.getBody());
							Map<String,Object> searchResult = result.getBody();
							return (Map)((ArrayList)searchResult.get("Resources")).get(0);
						}
						logger.info("size {}", result.getBody().size());
					}
				}
			}
		}
		return null;
	}
	
	


	
	
	
	
	
	
	
	
	
}