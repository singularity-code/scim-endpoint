package be.personify.iam.scim.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.StringUtils;

public class AuthenticationUtils {
	
	
	private static final Logger logger = LogManager.getLogger(AuthenticationUtils.class);
	

	/**
	 * Initializes the credential list
	 * @return
	 */
	public static Map<String,List<String>> getUserList(String authenticationType){
		logger.info("initializing users of type {}", authenticationType);
		try {
			Map<String,List<String>> users = new HashMap<String,List<String>>();
			int count = 1;
			String user = null;
			List<String> roles = null;
			while ( (user = PropertyFactory.getInstance().getProperty("scim.authentication.method." + authenticationType + ".user." + count)) != null) {
				String rolesString = PropertyFactory.getInstance().getProperty("scim.authentication.method." + authenticationType + ".user." + count + ".roles");
				logger.info("adding basic auth user {} with roles {}", user.split(Constants.COLON)[0], rolesString);
				roles = new ArrayList<String>();
				if ( !StringUtils.isEmpty(rolesString)) {
					roles = Arrays.asList(rolesString.split(Constants.COMMA));
				}
				users.put(user,roles);
				count++;
			}
			logger.info("initializing auth users of type {} done : found {} users", authenticationType, users.size());
			return users;
		}
		catch( Exception e ) {
			logger.error("initializing auth users of type {} ", authenticationType, e);
		}
		return null;
	}

}
