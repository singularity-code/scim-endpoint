package be.personify.iam.scim.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

public class CryptUtils {
	
	
	@Value("${scim.encryption.password:changeit}")
	private String password;
	
	
	public String encrypt(String plainText, String salt) {
		TextEncryptor encryptor = Encryptors.text(password, salt);
		return encryptor.encrypt(plainText);
	}

	
	public String decrypt(String encryptedText, String salt) {
		TextEncryptor encryptor = Encryptors.text(password, salt);
		return encryptor.decrypt(encryptedText);
	}
	
	

}
