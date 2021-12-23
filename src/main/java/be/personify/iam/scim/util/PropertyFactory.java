package be.personify.iam.scim.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public class PropertyFactory {

  @Autowired private Environment env;

  
  public String getProperty(String key) {
    return env.getProperty(key);
  }

  public String resolvePlaceHolder(String text) {
    return env.resolvePlaceholders(text);
  }
}
