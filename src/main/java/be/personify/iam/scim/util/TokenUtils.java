package be.personify.iam.scim.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.codec.Hex;

public class TokenUtils {
	
	private static final Logger logger = LogManager.getLogger(TokenUtils.class);
	
	public static final String SALT = new String(Hex.encode("tokenUtils".getBytes()));
	
	@Autowired
	private CryptUtils cryptUtils;
	
	public boolean isValid( String encryptedToken ) {
		String token = cryptUtils.decrypt(encryptedToken,SALT);
		String[] parts = token.split(Constants.COLON);
		logger.debug("checking is valid for user [{}]", parts[0]);
		long now = System.currentTimeMillis();
		long timeIssued = Long.parseLong(parts[1]);
		long expiryTimeInSeconds = Long.parseLong(parts[2]);
		if ( timeIssued + ( expiryTimeInSeconds * 1000) < now  ) {
			logger.debug("token is not valid for user [{}]", parts[0]);
			return false;
		}
		logger.debug("token is valid for user [{}]", parts[0]);
		return true;
	}
	
	
	public String construct( String client_id, long expiryTime ) {
		
		StringBuffer b = new StringBuffer(client_id);
		b.append(Constants.COLON).append(System.currentTimeMillis())
		.append(Constants.COLON).append(expiryTime);
		
		return cryptUtils.encrypt(b.toString(), SALT);
	}

}
