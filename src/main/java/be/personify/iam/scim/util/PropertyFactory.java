package be.personify.iam.scim.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public class PropertyFactory {

  @Autowired private Environment env;

  private static final Logger logger = LoggerFactory.getLogger(PropertyFactory.class);

  public String getProperty(String key) {
    return env.getProperty(key);
  }

  public String resolvePlaceHolder(String text) {
    return env.resolvePlaceholders(text);
  }
}
