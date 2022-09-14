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
package be.personify.iam.scim.util;

import be.personify.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.codec.Hex;

public class TokenUtils {

	private static final Logger logger = LogManager.getLogger(TokenUtils.class);

	public static final String SALT = new String(Hex.encode("ScimServer".getBytes()));

	@Autowired
	private CryptUtils cryptUtils;

	public boolean isValid(String encryptedToken) {
		String token = cryptUtils.decrypt(encryptedToken, SALT);
		String[] parts = token.split(StringUtils.COLON);
		logger.debug("checking is valid for user [{}]", parts[0]);
		long now = System.currentTimeMillis();
		long timeIssued = Long.parseLong(parts[1]);
		long expiryTimeInSeconds = Long.parseLong(parts[2]);
		if (timeIssued + (expiryTimeInSeconds * 1000) < now) {
			logger.debug("token is not valid for user [{}]", parts[0]);
			return false;
		}
		logger.debug("token is valid for user [{}]", parts[0]);
		return true;
	}

	public String construct(String client_id, long expiryTime) {

		StringBuffer b = new StringBuffer(client_id);
		b.append(StringUtils.COLON).append(System.currentTimeMillis()).append(StringUtils.COLON).append(expiryTime);

		return cryptUtils.encrypt(b.toString(), SALT);
	}
}
