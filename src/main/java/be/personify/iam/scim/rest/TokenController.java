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

import java.util.HashMap;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import be.personify.iam.scim.authentication.AuthenticationUtils;
import be.personify.iam.scim.util.Constants;
import be.personify.iam.scim.util.TokenUtils;
import be.personify.util.StringUtils;

/**
 * Token controller for the SCIM server
 *
 * @author wouter
 */
@RestController
public class TokenController extends Controller {

	private static final Logger logger = LogManager.getLogger(TokenController.class);

	@Autowired
	private AuthenticationUtils authenticationUtils;

	@Autowired
	private TokenUtils tokenUtils;

	@Value("${scim.authentication.propertyfile.method.bearer.lifeTimeInSeconds:60}")
	private long lifeTimeInSeconds;

	@RequestMapping(path = "/scim/v2/token", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public ResponseEntity<?> tokenInfo(@RequestBody(required = false) MultiValueMap<String, Object> entity, HttpServletRequest request) {

		long start = System.currentTimeMillis();
		
		logger.debug("entity {}", entity);

		if (entity != null && entity.containsKey("grant_type")) {
			logger.debug("grant_type found");
			if (entity.get("grant_type").get(0).equals("client_credentials")) {

				String header = request.getHeader(HttpHeaders.AUTHORIZATION);
				String credentials = extractCredentials(entity, header);
	
				if (!StringUtils.isEmpty(credentials) && credentials.contains(StringUtils.COLON)) {
					
					String[] cc = credentials.split(StringUtils.COLON);
	
					if (authenticationUtils.getBearerAuthConsumers().containsKey(cc[0])) {
	
						Map<String, Object> response = new HashMap<>();
						response.put("access_token", tokenUtils.construct(cc[0], lifeTimeInSeconds));
						response.put("token_type", Constants.BEARER);
						response.put("expires_in", lifeTimeInSeconds);
	
						logger.info("acquired token in [{}]ms", System.currentTimeMillis() - start);
	
						return new ResponseEntity<Map<String, Object>>(response, HttpStatus.OK);
					} 
					else {
						logger.info("invalid client_id/client_secret");
						return showError(HttpStatus.FORBIDDEN.value(), "invalid client_id/client_secret");
					}
	
				} else {
					String message = "client_id/client_secret [" + credentials + "] not found or incorrect, make sure it is part of the payload or present in the Authorization header";
					logger.info(message);
					return showError(HttpStatus.FORBIDDEN.value(), message);
				}
			}
			else {
				return showError(HttpStatus.FORBIDDEN.value(), "grant type is not of type client_credentials");
			}
			

		}
		else {
			logger.info("grant_type NOT found in body, please add an entry with key 'grant_type' and value 'client_credentials'") ;
			return showError(HttpStatus.FORBIDDEN.value(), "grant_type is not found in body");
		}
	}
	
	

	private String extractCredentials(MultiValueMap<String, Object> entity, String authorizationHeader) {
		String credential = null;
		if (entity.containsKey(Constants.CLIENT_ID) && entity.containsKey(Constants.CLIENT_SECRET)) {
			// first fetch it from the payload
			credential = entity.get(Constants.CLIENT_ID).get(0).toString() + StringUtils.COLON	+ entity.get(Constants.CLIENT_SECRET).get(0).toString();
		}
		else {
			// else fetch it from the authorization header
			String[] auth = authorizationHeader.split(StringUtils.SPACE);
			if (auth.length == 2) {
				if (auth[0].equalsIgnoreCase(Constants.BASIC)) {
					credential = new String(Base64Utils.decode(auth[1].getBytes()));
				}
			}
		}
		return credential;
	}
}
