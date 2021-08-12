package be.personify.iam.scim.storage.impl;

import be.personify.iam.model.provisioning.TargetSystem;
import be.personify.iam.provisioning.ProvisionResult;
import be.personify.iam.provisioning.ProvisionStatus;
import be.personify.iam.provisioning.ProvisionTask;
import be.personify.iam.provisioning.connectors.ConnectorConnection;
import be.personify.iam.provisioning.connectors.ConnectorPool;
import be.personify.iam.scim.schema.Schema;
import be.personify.iam.scim.schema.SchemaReader;
import be.personify.iam.scim.storage.ConfigurationException;
import be.personify.iam.scim.storage.ConstraintViolationException;
import be.personify.iam.scim.storage.DataException;
import be.personify.iam.scim.util.Constants;
import be.personify.util.MapUtils;
import be.personify.util.SearchCriteria;
import be.personify.util.SearchCriterium;
import be.personify.util.State;
import be.personify.util.StringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Storage implementation that stores data into a LDAP using the personify connector framework
 *
 * @author vanderw
 */
public class LDAPConnectorStorage extends ConnectorStorage {

  private static final String OBJECT_CLASS = "objectClass";
  private static final String CN = "cn=";

  private static final Logger logger = LogManager.getLogger(LDAPConnectorStorage.class);

  private String basedn = null;

  private static TargetSystem targetSystem = null;

  private static Map<String, String> mapping;
  private static Map<String, String> depthMapping;

  private List<String> objectClasses = null;

  private Schema schema = null;
  private List<String> schemaList = null;

  @Autowired private SchemaReader schemaReader;

  @Override
  public void create(String id, Map<String, Object> scimObject)
      throws ConstraintViolationException, DataException {

    try {
      Map<String, Object> extra = new HashMap<String, Object>();
      extra.put(Constants.ID, composeDn(id));
      extra.put(OBJECT_CLASS, objectClasses);
      scimObject = processMapping(id, scimObject, extra, depthMapping, schema);
      ProvisionResult result =
          new ProvisionTask().provision(State.PRESENT, scimObject, mapping, targetSystem);
      if (!result.getStatus().equals(ProvisionStatus.SUCCESS)) {
        throw new DataException(
            result.getErrorCode() + StringUtils.SPACE + result.getErrorDetail());
      }
    } catch (Exception e) {
      throw new DataException(e.getMessage());
    }
  }

  @Override
  public Map<String, Object> get(String id) {

    ConnectorConnection connection = null;
    try {
      connection = ConnectorPool.getInstance().getConnectorForTargetSystem(targetSystem);
      Map<String, Object> nativeMap = connection.getConnector().find(composeDn(id));
      if (nativeMap != null) {
        Map<String, Object> scimMap =
            convertNativeMap(
                nativeMap,
                mapping,
                depthMapping,
                Arrays.asList(new String[] {OBJECT_CLASS}),
                schema);
        scimMap.put(Constants.KEY_SCHEMAS, schemaList);
        scimMap.put(Constants.ID, id);
        return scimMap;
      }
      return null;
    } catch (Exception e) {
      e.printStackTrace();
      throw new DataException(e.getMessage());
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
  }

  @Override
  public void update(String id, Map<String, Object> scimObject)
      throws ConstraintViolationException {

    try {
      Map<String, Object> extra = new HashMap<String, Object>();
      extra.put(Constants.ID, composeDn(id));
      extra.put(OBJECT_CLASS, objectClasses);
      scimObject = processMapping(id, scimObject, extra, depthMapping, schema);
      ProvisionResult result =
          new ProvisionTask().provision(State.PRESENT, scimObject, mapping, targetSystem);
      if (!result.getStatus().equals(ProvisionStatus.SUCCESS)) {
        throw new DataException(
            result.getErrorCode() + StringUtils.SPACE + result.getErrorDetail());
      }
    } catch (Exception e) {
      throw new DataException(e.getMessage());
    }
  }

  @Override
  public boolean delete(String id) {

    ConnectorConnection connection = null;
    try {
      connection = ConnectorPool.getInstance().getConnectorForTargetSystem(targetSystem);
      return connection.getConnector().delete(composeDn(id));
    } catch (Exception e) {
      throw new DataException(e.getMessage());
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
  }

  @Override
  public List<Map> search(
      SearchCriteria searchCriteria, int start, int count, String sortBy, String sortOrderString) {
    return search(searchCriteria, start, count, sortBy, sortOrderString, null);
  }

  @Override
  public List<Map> search(
      SearchCriteria searchCriteria,
      int start,
      int count,
      String sortBy,
      String sortOrderString,
      List<String> includeAttributes) {
    ConnectorConnection connection = null;
    try {

      SearchCriteria nativeSearchCriteria = getNativeSearchCriteria(searchCriteria);

      connection = ConnectorPool.getInstance().getConnectorForTargetSystem(targetSystem);
      List<Map<String, Object>> nativeList =
          connection.getConnector().find(nativeSearchCriteria, start, count, null);
      List<Map> scimList = new ArrayList<>();
      for (Map<String, Object> nativeMap : nativeList) {
        Map<String, Object> scimMap =
            convertNativeMap(
                nativeMap,
                mapping,
                depthMapping,
                Arrays.asList(new String[] {OBJECT_CLASS}),
                schema);
        scimMap.put(Constants.KEY_SCHEMAS, schemaList);
        scimMap.put(Constants.ID, decomposeDn(scimMap.get(Constants.ID)));
        scimList.add(scimMap);
      }
      return scimList;
    } catch (Exception e) {
      e.printStackTrace();
      throw new DataException(e.getMessage());
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
  }

  @Override
  public long count(SearchCriteria searchCriteria) {
    ConnectorConnection connection = null;
    try {

      SearchCriteria nativeSearchCriteria = getNativeSearchCriteria(searchCriteria);

      connection = ConnectorPool.getInstance().getConnectorForTargetSystem(targetSystem);
      List<String> nativeList = connection.getConnector().findIds(nativeSearchCriteria, 0, 0, null);
      return Long.valueOf(nativeList.size());
    } catch (Exception e) {
      e.printStackTrace();
      throw new DataException(e.getMessage());
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
  }

  private SearchCriteria getNativeSearchCriteria(SearchCriteria searchCriteria) {
    SearchCriteria nativeSearchCriteria = new SearchCriteria();
    for (SearchCriterium criterium : searchCriteria.getCriteria()) {
      String nativeKey = (String) MapUtils.getKeyByValue(mapping, criterium.getKey());
      nativeSearchCriteria
          .getCriteria()
          .add(
              new SearchCriterium(nativeKey, criterium.getValue(), criterium.getSearchOperation()));
    }
    return nativeSearchCriteria;
  }

  private String composeDn(String id) {
    return CN + id + StringUtils.COMMA + basedn;
  }

  private String decomposeDn(Object id) {
    if (id instanceof List) {
      String firstId = ((String) ((List) id).get(0));
      return firstId.substring(CN.length(), firstId.indexOf(StringUtils.COMMA));
    }
    return null;
  }

  @Override
  public void initialize(String type) {
    try {
      Map<String, Object> config = getConfigMap("ldap");

      final String targetSystemJson =
          Constants.objectMapper.writeValueAsString(config.get("targetSystem"));
      targetSystem = Constants.objectMapper.readValue(targetSystemJson, TargetSystem.class);

      // add type to basedn
      basedn = targetSystem.getConnectorConfiguration().getConfiguration().get("baseDn");
      basedn = "ou=" + type.toLowerCase() + StringUtils.COMMA + basedn;
      targetSystem.getConnectorConfiguration().getConfiguration().put("baseDn", basedn);

      mapping = (Map) config.get("mapping");
      if (mapping == null || targetSystem == null) {
        throw new ConfigurationException("can not find mapping or targetSystem in configuration");
      } else {
        objectClasses =
            Arrays.asList(
                targetSystem
                    .getConnectorConfiguration()
                    .getConfiguration()
                    .get(type.toLowerCase() + "ObjectClasses")
                    .split(StringUtils.COMMA));
        schema = schemaReader.getSchemaByResourceType(type);
        schemaList = Arrays.asList(new String[] {schema.getId()});
        depthMapping = createDepthMapping(mapping);
        testConnection(targetSystem);
      }
    } catch (Exception e) {
      logger.error("can not read/validate configuration for type {}", type, e);
      throw new ConfigurationException(e.getMessage());
    }
  }

  @Override
  public synchronized void flush() {
    throw new DataException("flush all not implemented");
  }

  @Override
  public boolean deleteAll() {
    throw new DataException("delete all not implemented");
  }

  @Override
  public Map<String, Object> get(String id, String version) {
    throw new DataException("versioning not implemented");
  }

  @Override
  public List<String> getVersions(String id) {
    throw new DataException("versioning not implemented");
  }
}
