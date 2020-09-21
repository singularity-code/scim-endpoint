package be.personify.iam.scim.authentication;

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

import be.personify.iam.scim.util.Constants;
import be.personify.iam.scim.util.TokenUtils;


/**
 * filter to check security
 * 
 * @author wouter
 *
 */
public class PropertyFileAuthenticationFilter implements Filter {
	
	
	private static final String SERVER = "Server";
	private static final String ROLE_READ = "read";
	private static final String ROLE_WRITE = "write";

	private static final Logger logger = LogManager.getLogger(PropertyFileAuthenticationFilter.class);
	
	@Autowired
	private TokenUtils tokenUtils;
	
	@Autowired
	private AuthenticationUtils authenticationUtils;
	
	private final List<String> PUBLIC_ENDPOINTS = Arrays.asList(new String[] {"/scim/v2/token", "/scim/v2/Me"});
	private final String serverDescription = PropertyFileAuthenticationFilter.class.getPackage().getImplementationTitle() + Constants.SPACE + PropertyFileAuthenticationFilter.class.getPackage().getImplementationVersion();

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		
		HttpServletRequest req = (HttpServletRequest) request;
		
		((HttpServletResponse)response).addHeader(SERVER, serverDescription);

		String header = req.getHeader(HttpHeaders.AUTHORIZATION);
		boolean filtered = false;
		
		
		if ( PUBLIC_ENDPOINTS.contains(req.getRequestURI())){
			chain.doFilter(request, response);
			filtered = true;
		}
		else {
			if ( header != null ) {
				String[] auth = header.split(Constants.SPACE);
				if ( auth.length == 2) {
					String method = req.getMethod();
					if ( auth[0].equalsIgnoreCase(Constants.BASIC)) {
						String credential = new String(Base64Utils.decode(auth[1].getBytes()));
						
						if ( authenticationUtils.getBasicAuthUsers() != null && authenticationUtils.getBasicAuthUsers().containsKey(credential)) {
							//check roles 
							filtered = checkRole(request, response, chain, filtered, credential, method, authenticationUtils.getBasicAuthUsers());
						}
					}
					else if (auth[0].equalsIgnoreCase(Constants.BEARER)) {
						String token = auth[1];
						logger.debug("token {}", token);
						if ( tokenUtils.isValid(token)) {
							filtered = checkRole(request, response, chain, filtered, token, method, authenticationUtils.getBearerAuthUsers());
						}
					}
				}
			}
		}
		
		if ( filtered == false ) {
			HttpServletResponse resp = (HttpServletResponse) response;
		    resp.reset();
		    resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		    resp.flushBuffer();
		}
		
	}

	
	
	
	private boolean checkRole(ServletRequest request, ServletResponse response, FilterChain chain, boolean filtered, String credential, String method, Map<String,List<String>> users) throws IOException, ServletException {
		if ( method.equals(HttpMethod.GET.name()) ) {
			if ( users.get(credential).contains(ROLE_READ)) {
				chain.doFilter(request, response);
				filtered = true;
			}
		}
		else {
			if ( users.get(credential).contains(ROLE_WRITE)) {
				chain.doFilter(request, response);
				filtered = true;
			}
		}
		return filtered;
	}

	

	

}
