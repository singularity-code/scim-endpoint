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

import be.personify.iam.scim.storage.DataException;
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
    try {
      return encryptor.decrypt(encryptedText);
    } catch (Exception e) {
      throw new DataException("can not decrypt token");
    }
  }
}
