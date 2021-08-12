package be.personify.iam.scim.storage.impl;

import be.personify.iam.model.provisioning.TargetSystem;
import be.personify.iam.provisioning.connectors.ConnectorConnection;
import be.personify.iam.provisioning.connectors.ConnectorPool;
import be.personify.iam.scim.schema.Schema;
import be.personify.iam.scim.schema.SchemaAttribute;
import be.personify.iam.scim.storage.ConfigurationException;
import be.personify.iam.scim.storage.Storage;
import be.personify.iam.scim.util.Constants;
import be.personify.iam.scim.util.PropertyFactory;
import be.personify.util.StringUtils;
import be.personify.util.io.IOUtils;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class ConnectorStorage implements Storage {

  private static final Logger logger = LogManager.getLogger(ConnectorStorage.class);

  protected static final String ESCAPED_DOT = "\\.";

  @Autowired private PropertyFactory propertyFactory;

  protected void testConnection(TargetSystem targetSystem) {
    try {
      ConnectorConnection connection =
          ConnectorPool.getInstance().getConnectorForTargetSystem(targetSystem);
      if (connection != null) {
        connection.getConnector().ping();
        connection.close();
        logger.info("successfully tested connection");
      } else {
        throw new ConfigurationException("can not lease connection");
      }
    } catch (Exception e) {
      logger.error("can not test connection", e);
      throw new ConfigurationException("can not lease connection " + e.getMessage());
    }
  }

  protected Map<String, String> createDepthMapping(Map<String, String> m) {
    Map<String, String> mm = new HashMap<String, String>();
    for (String key : m.keySet()) {
      String value = m.get(key);
      if (value.contains(StringUtils.DOT)) {
        String[] parts = value.split(ESCAPED_DOT);
        if (parts.length == 2) {
          mm.put(key, value);
        } else {
          throw new ConfigurationException(
              "expression is limited to depth of 2 for mapping [" + key + "->" + value + "]");
        }
      }
    }
    return mm;
  }

  protected Map<String, Object> convertNativeMap(
      Map<String, Object> nativeMap,
      Map<String, String> mapping,
      Map<String, String> depthMapping,
      List<String> excludes,
      Schema schema) {
    Map<String, Object> scimMap = new HashMap<String, Object>();
    for (String key : mapping.keySet()) {
      if (nativeMap.containsKey(key)) {
        scimMap.put(mapping.get(key), nativeMap.get(key));
      }
    }
    for (String exclude : excludes) {
      scimMap.remove(exclude);
    }
    if (depthMapping.size() > 0) {
      for (String mappingValue : depthMapping.values()) {
        String parts[] = mappingValue.split(ESCAPED_DOT);
        if (scimMap.get(parts[0]) != null) {
          ((Map) scimMap.get(parts[0])).put(parts[1], scimMap.get(mappingValue));
        } else {
          Object value = scimMap.get(mappingValue); // contains dot
          if (value != null) {
            logger.debug("mappingValue {} value {} parts [0] {}", mappingValue, value, parts[0]);
            SchemaAttribute sa = schema.getAttribute(parts[0]);
            if (sa != null && sa.isMultiValued()) {
              logger.debug("its multivalued {} {} {}", mappingValue, value, value.getClass());
              List<Map> newList = new ArrayList<>();
              if (value instanceof List) {
                logger.debug("its a list {} ", value);
                for (Object o : (List) value) {
                  logger.debug("object {} ", o);
                  Map<String, Object> newMap = new HashMap<>();
                  newMap.put(parts[1], o);
                  newList.add(newMap);
                }

              } else if (value instanceof String) {
                logger.debug("its a string {} ", value);
                Map<String, Object> newMap = new HashMap<>();
                newMap.put(parts[1], value);
                newList.add(newMap);
              }
              scimMap.put(parts[0], newList);
            } else {
              Map<String, Object> mm = new HashMap<>();
              mm.put(parts[1], value);
              scimMap.put(parts[0], mm);
            }
          }
        }
        scimMap.remove(mappingValue);
      }
    }
    return scimMap;
  }

  protected Map<String, Object> processMapping(
      String id,
      Map<String, Object> scimObject,
      Map<String, Object> extraAttributes,
      Map<String, String> depthMapping,
      Schema schema) {

    scimObject.putAll(extraAttributes);

    if (depthMapping.size() > 0) {

      Map<String, Object> newMap = new HashMap<String, Object>(scimObject);
      SchemaAttribute sa = null;
      Object value = null;
      for (String key : scimObject.keySet()) {
        for (String mappingValue : depthMapping.values()) {
          if (mappingValue.startsWith(key + StringUtils.DOT)) {
            sa = schema.getAttribute(key);
            if (sa != null) {
              String parts[] = mappingValue.split(ESCAPED_DOT);
              if (sa.isMultiValued()) {
                // TODO
                List<Map> list = (List) scimObject.get(key);
                List<Object> valueList = new ArrayList<Object>();
                for (Map m : list) {
                  valueList.add(m.get(parts[1]));
                }
                newMap.put(mappingValue, valueList);
              } else {
                value = ((Map) scimObject.get(key)).get(parts[1]);
                newMap.put(mappingValue, value);
              }
            }
          }
        }
      }
      return newMap;
    }
    return scimObject;
  }

  protected Map<String, Object> getConfigMap(String connectorType)
      throws JsonMappingException, JsonParseException, IOException {
    String configFile =
        propertyFactory.getProperty("scim.storage." + connectorType + ".configFile");
    String fileContent = null;
    if (!StringUtils.isEmpty(configFile)) {
      fileContent = new String(IOUtils.readFileAsBytes(new FileInputStream(new File(configFile))));
    } else {
      fileContent =
          new String(
              IOUtils.readFileAsBytes(
                  DatabaseConnectorStorage.class.getResourceAsStream(
                      "/connector_" + connectorType + ".json")));
    }

    fileContent = propertyFactory.resolvePlaceHolder(fileContent);

    logger.debug("{}", fileContent);
    return Constants.objectMapper.readValue(fileContent, Map.class);
  }
}
