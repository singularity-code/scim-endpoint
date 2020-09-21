package be.personify.iam.scim.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import be.personify.iam.scim.authentication.AuthenticationUtils;
import be.personify.iam.scim.util.Constants;
import be.personify.iam.scim.util.TokenUtils;

/**
 * Discovery controller for the SCIM server
 * @author wouter
 *
 */
@RestController
public class TokenController extends Controller {

	private static final Logger logger = LogManager.getLogger(TokenController.class);
	
	@Autowired
	private AuthenticationUtils authenticationUtils;
	
	@Autowired
	private TokenUtils tokenUtils;
	
	
	@Value("${scim.authentication.method.bearer.lifeTimeInSeconds:60}")
	private long lifeTimeInSeconds;


	@RequestMapping(path="/scim/v2/token", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE )
	public ResponseEntity<?> tokenInfo(@RequestBody MultiValueMap<String, Object> entity, HttpServletRequest request ) {
		
		long start = System.currentTimeMillis();
		
		logger.info("tokeninfo");
		
		if ( entity.containsKey("grant_type") && entity.get("grant_type").get(0).equals("client_credentials")) {
			
			String header = request.getHeader(HttpHeaders.AUTHORIZATION);
			String credentials = extractCredentials(entity, header);
			
			if ( !StringUtils.isEmpty(credentials) && credentials.contains(Constants.COLON)) {
				
				if ( authenticationUtils.getBearerAuthUsers().containsKey(credentials)) {
					
					String[] cc = credentials.split(Constants.COLON);
					
					Map<String,Object> response = new HashMap<>();
					response.put("access_token", tokenUtils.construct(cc[0], lifeTimeInSeconds));
					response.put("token_type", Constants.BEARER);
					response.put("expires_in", lifeTimeInSeconds);
					
					logger.info("acquired token {} in [{}]ms", response, System.currentTimeMillis() - start);
				
					return new ResponseEntity<Map<String,Object>>(response, HttpStatus.OK);
				}
				else {
					logger.info("invalid client_id/client_secret");
					return showError(HttpStatus.FORBIDDEN.value(), "invalid client_id/client_secret");
				}
				
			}
			else {
				String message = "client_id/client_secret [" + credentials + "] not found or incorrect, make sure it is part of the payload or present in the Authorization header";
				logger.info(message);
				return showError(HttpStatus.FORBIDDEN.value(), message);
			}
			
		}
		else {
			logger.info("grant type [{}] is not of type client_credentials", entity.get("grant_type"));
			return showError(HttpStatus.FORBIDDEN.value(), "grant type is not of type client_credentials");
		}
		
			
	}


	private String extractCredentials(MultiValueMap<String, Object> entity, String authorizationHeader ) {
		String credential = null;
		if ( entity.containsKey(Constants.CLIENT_ID) && entity.containsKey(Constants.CLIENT_SECRET)) {
			//first fetch it from the payload
			credential = entity.get(Constants.CLIENT_ID).get(0).toString() + Constants.COLON + entity.get(Constants.CLIENT_SECRET).get(0).toString();
		}
		else {
			//else fetch it from the authorization header
			String[] auth = authorizationHeader.split(Constants.SPACE);
			if ( auth.length == 2) {
				if ( auth[0].equalsIgnoreCase(Constants.BASIC)) {
					credential = new String(Base64Utils.decode(auth[1].getBytes()));
				}
			}
		}
		return credential;
	}
	
	
	
	
	
	
	
}