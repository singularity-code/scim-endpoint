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

import be.personify.iam.scim.util.AuthenticationUtils;
import be.personify.iam.scim.util.Constants;
import be.personify.iam.scim.util.TokenUtils;

/**
 * Discovery controller for the SCIM server
 * @author wouter
 *
 */
@RestController
public class MeController extends Controller {

	private static final Logger logger = LogManager.getLogger(MeController.class);
	
	@RequestMapping(path="/scim/v2/Me", method = RequestMethod.GET, produces = {"application/scim+json","application/json"} )
	public ResponseEntity<?> me(HttpServletRequest request ) {
		
		
		logger.info("/Me endpoint");
		
		return showError(HttpStatus.NOT_IMPLEMENTED.value(), "the /Me endpoint is not yet implemented", null);
			
	}


	
	
	
	
	
	
	
	
	
}