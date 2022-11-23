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
package be.personify.iam.scim.authentication;

import be.personify.iam.scim.util.Constants;
import be.personify.iam.scim.util.CryptUtils;
import be.personify.iam.scim.util.TokenUtils;
import be.personify.util.StringUtils;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Base64Utils;

/**
 * filter to check security
 *
 * @author wouter
 */
public class PropertyFileAuthenticationFilter implements Filter {

	private static final String SERVER = "Server";
	private static final String ROLE_READ = "read";
	private static final String ROLE_WRITE = "write";

	private static final Logger logger = LogManager.getLogger(PropertyFileAuthenticationFilter.class);

	@Autowired
	private TokenUtils tokenUtils;

	@Autowired
	private CryptUtils cryptUtils;

	@Autowired
	private AuthenticationUtils authenticationUtils;

	private static final List<String> UNAUTHENTICATED_ENDPOINTS = Arrays.asList(new String[] { "/scim/v2/token", "/scim/v2/Me"});
	
	private static final String serverDescription = PropertyFileAuthenticationFilter.class.getPackage().getImplementationTitle() + StringUtils.SPACE + PropertyFileAuthenticationFilter.class.getPackage().getImplementationVersion();

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		HttpServletRequest req = (HttpServletRequest) request;

		((HttpServletResponse) response).addHeader(SERVER, serverDescription);

		String header = req.getHeader(HttpHeaders.AUTHORIZATION);
		boolean filtered = false;
		
		Consumer consumer = new Consumer(null, null);
		
		boolean hasAccess = false;
		

		if (UNAUTHENTICATED_ENDPOINTS.contains(req.getRequestURI())) {
			logger.debug("{} is a public endpoint", req.getRequestURI());
			chain.doFilter(request, response);
			filtered = true;
		} 
		else {
			if (header != null) {
				String[] auth = header.split(StringUtils.SPACE);
				if (auth.length == 2) {
					String method = req.getMethod();
					logger.debug("the auth method {}", auth[0]);
					if (auth[0].equalsIgnoreCase(Constants.BASIC)) {
						String credential = new String(Base64Utils.decode(auth[1].getBytes()));
						String[] splitted = credential.split(StringUtils.COLON);
						logger.debug("splitted {}", splitted);
						Map<String,Consumer> bc = authenticationUtils.getBasicAuthConsumers(); 
						if ( bc!= null	&& bc.containsKey(splitted[0])) {
							consumer = bc.get(splitted[0]);
							logger.debug("consumer {}", consumer);
							//check password
							if ( consumer.getSecret().equals(splitted[1])) {
								// check roles
								logger.debug("passwd match");
								hasAccess = checkRole(request, response, chain, splitted[0], method, bc);
							}
						}
					} 
					else if (auth[0].equalsIgnoreCase(Constants.BEARER)) {
						String token = auth[1];
						logger.debug("token {}", token);
						if (tokenUtils.isValid(token)) {
							String decrypted = cryptUtils.decrypt(token, TokenUtils.SALT);
							String[] parts = decrypted.split(StringUtils.COLON);
							String clientId = parts[0];
							token = getClientIdWithCredential(clientId, authenticationUtils.getBearerAuthConsumers());
							consumer = authenticationUtils.getBearerAuthConsumers().get(clientId);
							hasAccess = checkRole(request, response, chain, token, method, authenticationUtils.getBearerAuthConsumers());
						}
					}
				}
			}
		}
		

		if (filtered == false) {
			if (hasAccess ) {
				CurrentConsumer.setCurrent(consumer);
				chain.doFilter(request, response);
			}
			else {
				HttpServletResponse resp = (HttpServletResponse) response;
				resp.reset();
				resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				resp.flushBuffer();
			}
		}
		
	}

	
	
	private boolean checkRole(ServletRequest request, ServletResponse response, FilterChain chain, String clientid, String method, Map<String, Consumer> users) throws IOException, ServletException {
		logger.debug("clientid {}", clientid);
		List<String> roles = users.get(clientid).getRoles();
		if (roles != null) {
			if (method.equals(HttpMethod.GET.name())) {
				if (roles.contains(ROLE_READ)) {
					return true;
				}
			} else {
				if (roles.contains(ROLE_WRITE)) {
					return true;
				}
			}
		}
		return false;
	}

	
	
	
	private String getClientIdWithCredential(String clientId, Map<String, Consumer> users) {
		for (String key : users.keySet()) {
			if (key.equals(clientId)) {
				return key;
			}
		}
		return null;
	}
}
