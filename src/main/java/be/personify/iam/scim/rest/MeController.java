package be.personify.iam.scim.rest;

import be.personify.iam.scim.schema.Schema;
import be.personify.iam.scim.schema.SchemaReader;
import be.personify.iam.scim.util.Constants;
import be.personify.util.StringUtils;
import java.util.ArrayList;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.Base64Utils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Me controller for the SCIM server
 *
 * @author wouter
 */
@RestController
public class MeController extends Controller {

	@Autowired
	private SchemaReader schemaReader;

	private Schema getSchema() {
		return schemaReader.getSchemaByResourceType(Constants.RESOURCE_TYPE_USER);
	}

	@PutMapping(path = "/scim/v2/Me", produces = { "application/scim+json", "application/json" })
	public ResponseEntity<Map<String, Object>> putMe(@RequestBody Map<String, Object> entity,
			@RequestParam(required = false, name = "attributes") String attributes,
			@RequestParam(required = false, name = "excludedAttributes") String excludedAttributes,
			HttpServletRequest request, HttpServletResponse response) {
		
		Map<String, Object> result = getAndValidateUserName(request, getSchema());
		if (!StringUtils.isEmpty(result)) {
			// perform update
			if (result.get(Constants.ID).equals(entity.get(Constants.ID))) {
				return put(result.get(Constants.ID).toString(), entity, request, response, getSchema(), attributes,
						excludedAttributes);
			} 
			else {
				return showError(HttpStatus.UNAUTHORIZED.value(),"not authorized to update user with id " + entity.get(Constants.ID), null);
			}
		} 
		else {
			return showError(HttpStatus.UNAUTHORIZED.value(), "no valid authorization subject found", null);
		}
	}

	@GetMapping(path = "/scim/v2/Me", produces = { "application/scim+json", "application/json" })
	public ResponseEntity<Map<String, Object>> getMe(
			@RequestParam(required = false, name = "attributes") String attributes,
			@RequestParam(required = false, name = "excludedAttributes") String excludedAttributes,
			HttpServletRequest request, HttpServletResponse response) {
		
		Map<String, Object> result = getAndValidateUserName(request, getSchema());
		if (!StringUtils.isEmpty(result)) {
			ResponseEntity<Map<String, Object>> responseEntity = new ResponseEntity<Map<String, Object>>(
					filterAttributes(getSchema(), result, getListFromString(attributes), excludedAttributes),
					HttpStatus.OK);
			String requestUrl = UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request)).build()
					.toUriString();
			requestUrl = requestUrl.substring(0, requestUrl.lastIndexOf("/Me") + 1) + "Users/"
					+ result.get(Constants.ID);
			response.addHeader(Constants.HEADER_LOCATION, requestUrl);
			return responseEntity;
		} else {
			return showError(HttpStatus.UNAUTHORIZED.value(), "no valid authorization subject found", null);
		}
	}

	@DeleteMapping(path = "/scim/v2/Me")
	public ResponseEntity<?> delete(@PathVariable String resourceType, @PathVariable String id) {
		// think this through before you implements this
		return showError(HttpStatus.NOT_IMPLEMENTED.value(),
				"the delete against the /Me endpoint is not yet implemented", null);
	}

	@PostMapping(path = "/scim/v2/Me", produces = { "application/scim+json", "application/json" })
	public ResponseEntity<Map<String, Object>> post(@RequestBody Map<String, Object> entity,
			@RequestParam(required = false, name = "attributes") String attributes,
			@RequestParam(required = false, name = "excludedAttributes") String excludedAttributes,
			HttpServletRequest request, HttpServletResponse response) {
		// think this through before you implement this
		return showError(HttpStatus.NOT_IMPLEMENTED.value(), "the post to the /Me endpoint is not yet implemented",
				null);
	}

	@PatchMapping(path = "/scim/v2/Me", produces = { "application/scim+json", "application/json" })
	public ResponseEntity<Map<String, Object>> patchMe(@RequestBody Map<String, Object> entity,
			@RequestParam(required = false, name = "attributes") String attributes,
			@RequestParam(required = false, name = "excludedAttributes") String excludedAttributes,
			HttpServletRequest request, HttpServletResponse response) {
		return showError(HttpStatus.NOT_IMPLEMENTED.value(),
				"the patch against the /Me endpoint is not yet implemented", null);
	}

	private Map<String, Object> getAndValidateUserName(HttpServletRequest request, Schema schema) {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null) {
			String[] auth = header.split(be.personify.util.StringUtils.SPACE);
			if (auth.length == 2) {
				if (auth[0].equalsIgnoreCase(Constants.BASIC)) {
					String credential = new String(Base64Utils.decode(auth[1].getBytes()));
					String[] parts = credential.split(StringUtils.COLON);
					if (parts.length == 2) {
						String userName = parts[0];
						String password = parts[1];
						ResponseEntity<Map<String, Object>> result = search(1, 1, schema,
								"userName eq " + userName + " and password eq " + password);
						if ((Long) result.getBody().get(Constants.KEY_TOTALRESULTS) == 1) {
							Map<String, Object> searchResult = result.getBody();
							return (Map) ((ArrayList) searchResult.get(Constants.KEY_RESOURCES)).get(0);
						}
					}
				}
			}
		}
		return null;
	}
}
