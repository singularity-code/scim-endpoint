package be.personify.iam.scim.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;

@Component
@Order(1)
public class BasicAuthenticationFilter implements Filter {
	
	private static final Logger logger = LogManager.getLogger(BasicAuthenticationFilter.class);
	
	private static final String BASIC = "Basic";
	
	private List<String> basicAuthCredentials = null;
	
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
					if ( getBasicAuthList().contains(credential)) {
						chain.doFilter(request, response);
						filtered = true;
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
	private List<String> getBasicAuthList(){
		if ( basicAuthCredentials == null ) {
			logger.info("initializing basic auth users");
			try {
				synchronized (lock) {
					basicAuthCredentials = new ArrayList<String>();
					int count = 1;
					String user = null;
					while ( (user = PropertyFactory.getInstance().getProperty("scim.authentication.method.basic.user." + count)) != null) {
						basicAuthCredentials.add(user);
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
