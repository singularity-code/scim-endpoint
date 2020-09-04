package be.personify.iam.scim.util;

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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Base64Utils;


public class AuthenticationFilter implements Filter {
	
	private static final String ROLE_READ = "read";
	private static final String ROLE_WRITE = "write";

	private static final Logger logger = LogManager.getLogger(AuthenticationFilter.class);
	
	private TokenUtils tokenUtils;
	
	private Map<String,List<String>> basicAuthUsers = AuthenticationUtils.getUserList(Constants.BASIC.toLowerCase());
	private Map<String,List<String>> bearerAuthUsers = AuthenticationUtils.getUserList(Constants.BEARER.toLowerCase());
	
	
	private static final List<String> PUBLIC_ENDPOINTS = Arrays.asList(new String[] {"/scim/v2/token"});

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		
		HttpServletRequest req = (HttpServletRequest) request;
		

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
					if ( auth[0].equalsIgnoreCase(Constants.BASIC)) {
						String credential = new String(Base64Utils.decode(auth[1].getBytes()));
						if ( basicAuthUsers != null && basicAuthUsers.containsKey(credential)) {
							//check roles 
							String method = req.getMethod();
							if ( method.equals(HttpMethod.GET.name()) ) {
								if ( basicAuthUsers.get(credential).contains(ROLE_READ)) {
									chain.doFilter(request, response);
									filtered = true;
								}
							}
							else {
								if ( basicAuthUsers.get(credential).contains(ROLE_WRITE)) {
									chain.doFilter(request, response);
									filtered = true;
								}
							}
						}
					}
					else if (auth[0].equalsIgnoreCase(Constants.BEARER)) {
						String token = auth[1];
						logger.debug("token {}", token);
						if ( tokenUtils.isValid(token)) {
							chain.doFilter(request, response);
							filtered = true;
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

	public void setTokenUtils(TokenUtils tokenUtils) {
		this.tokenUtils = tokenUtils;
	}
	
	
	
	

}
