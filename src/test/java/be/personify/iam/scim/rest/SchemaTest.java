package be.personify.iam.scim.rest;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.personify.iam.scim.init.Application;
import be.personify.iam.scim.schema.SchemaException;
import be.personify.iam.scim.schema.SchemaReader;
import be.personify.iam.scim.util.Constants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = Application.class)
public class SchemaTest {

  @Autowired private SchemaReader schemaReader;
  @Autowired private SchemaController schemaController;

  // private Schema userSchema = schemaReader.getSchemaByResourceType(Constants.RESOURCE_TYPE_USER);

  @Test
  public void testEmptyMap() {
    Map<String, Object> map = new HashMap<String, Object>();
    try {
      schemaReader.validate(
          schemaReader.getSchemaByResourceType(Constants.RESOURCE_TYPE_USER), map, true);
    } catch (SchemaException se) {
      return;
    }
    fail("No schema exception thrown");
  }

  @Test
  public void testMinimal() {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("userName", "username");
    map.put("externalId", "externalId");
    try {
      schemaReader.validate(
          schemaReader.getSchemaByResourceType(Constants.RESOURCE_TYPE_USER), map, true);
    } catch (SchemaException se) {
      fail("No schema exception thrown " + se.getMessage());
    }
  }

  @Test
  public void testMinimalWithInvalidOptionalComplexMultiOne() {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("userName", "username");
    map.put("externalId", "externalId");
    map.put("emails", "another email");
    try {
      schemaReader.validate(
          schemaReader.getSchemaByResourceType(Constants.RESOURCE_TYPE_USER), map, true);
    } catch (SchemaException se) {
      // se.printStackTrace();
      return;
    }
    fail("No schema exception thrown ");
  }

  @Test
  public void testMinimalWithInvalidOptionalComplexMultiTwo() {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("userName", "username");
    map.put("externalId", "externalId");
    String[] emails = new String[] {"another email"};
    map.put("emails", emails);
    try {
      schemaReader.validate(
          schemaReader.getSchemaByResourceType(Constants.RESOURCE_TYPE_USER), map, true);
    } catch (SchemaException se) {
      // se.printStackTrace();
      return;
    }
    fail("No schema exception thrown ");
  }

  @Test
  public void testMinimalWithInvalidOptionalComplexMultiThree() {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("userName", "username");
    map.put("externalId", "externalId");

    List<Map<String, Object>> mailList = new ArrayList<Map<String, Object>>();
    Map<String, Object> mailMap = new HashMap<String, Object>();
    mailMap.put("test", "test");
    mailList.add(mailMap);

    map.put("emails", mailList);
    try {
      schemaReader.validate(
          schemaReader.getSchemaByResourceType(Constants.RESOURCE_TYPE_USER), map, true);
    } catch (SchemaException se) {
      // se.printStackTrace();
      return;
    }
    fail("No schema exception thrown ");
  }

  @Test
  public void testMinimalWithValidOptionalComplexMultiOne() {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("userName", "username");
    map.put("externalId", "externalId");

    List<Map<String, Object>> mailList = new ArrayList<Map<String, Object>>();
    Map<String, Object> mailMap = new HashMap<String, Object>();
    mailMap.put("value", "wouter@test.be");
    mailList.add(mailMap);

    map.put("emails", mailList);
    try {
      schemaReader.validate(
          schemaReader.getSchemaByResourceType(Constants.RESOURCE_TYPE_USER), map, true);
    } catch (SchemaException se) {
      fail("schema exception thrown " + se.getMessage());
    }
  }

  @Test
  public void testPathAccess() {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("userName", "username");
    map.put("externalId", "externalId");
    List<String> groups = new ArrayList<>();
    groups.add("admin");
    map.put("groups", groups);
    Map<String, Object> two = new HashMap<String, Object>();
    two.put("enabled", true);
    List<String> extra = new ArrayList<>();
    extra.add("e");
    two.put("extra", extra);
    map.put("extended", two);

    Object o = schemaController.getPath(null, map);
    assertTrue(o instanceof Map);

    o = schemaController.getPath("groups", map);
    assertTrue(o instanceof List);
    assertTrue(((List) o).contains("admin"));

    o = schemaController.getPath("extended.extra", map);
    assertTrue(o instanceof List);
    assertTrue(((List) o).contains("e"));
  }
}
