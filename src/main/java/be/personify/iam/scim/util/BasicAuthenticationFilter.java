package be.personify.iam.scim.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
import org.springframework.util.StringUtils;

//@Component
//@Order(1)
public class BasicAuthenticationFilter implements Filter {
	
	private static final String ROLE_READ = "read";
	private static final String ROLE_WRITE = "write";

	private static final Logger logger = LogManager.getLogger(BasicAuthenticationFilter.class);
	
	private static final String BASIC = "Basic";
	
	private Map<String,List<String>> basicAuthCredentials = null;
	
	private static Object lock = new Object();

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		
		HttpServletRequest req = (HttpServletRequest) request;

		String header = req.getHeader(HttpHeaders.AUTHORIZATION);
		boolean filtered = false;
		if ( header != null ) {
			String[] auth = header.split(Constants.SPACE);
			if ( auth.length == 2) {
				if ( auth[0].equalsIgnoreCase(BASIC)) {
					String credential = new String(Base64Utils.decode(auth[1].getBytes()));
					Map<String,List<String>> users =  getBasicAuthList();
					if ( users != null && users.containsKey(credential)) {
						//check roles
						String method = req.getMethod();
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
	
	
	
	/**
	 * Initializes the credential list
	 * @return
	 */
	private Map<String,List<String>> getBasicAuthList(){
		if ( basicAuthCredentials == null ) {
			logger.info("initializing basic auth users");
			try {
				synchronized (lock) {
					basicAuthCredentials = new HashMap<String,List<String>>();
					int count = 1;
					String user = null;
					List<String> roles = null;
					while ( (user = PropertyFactory.getInstance().getProperty("scim.authentication.method.basic.user." + count)) != null) {
						String rolesString = PropertyFactory.getInstance().getProperty("scim.authentication.method.basic.user." + count + ".roles");
						logger.info("adding basic auth user {} with roles {}", user.split(Constants.COLON)[0], rolesString);
						roles = new ArrayList<String>();
						if ( !StringUtils.isEmpty(rolesString)) {
							roles = Arrays.asList(rolesString.split(Constants.COMMA));
						}
						basicAuthCredentials.put(user,roles);
						count++;
					}
				}
				logger.info("initializing basic auth users done : found {} users", basicAuthCredentials.size());
			}
			catch( Exception e ) {
				logger.error("initializing basic auth users", e);
				basicAuthCredentials = null;
			}
		}
		return basicAuthCredentials;
	}
	
	

}
