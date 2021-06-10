package be.personify.iam.scim;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import be.personify.iam.scim.init.Application;
import be.personify.iam.scim.rest.SchemaController;
import be.personify.iam.scim.schema.Schema;
import be.personify.iam.scim.schema.SchemaReader;
import be.personify.iam.scim.storage.StorageImplementationFactory;
import be.personify.iam.scim.util.Constants;
import be.personify.util.StringUtils;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
public class ProvisionUpdateTest {

  public static String getJson(String name) throws IOException {
    return IOUtils.toString(
        ProvisionUpdateTest.class.getResourceAsStream(name), Charset.defaultCharset());
  }

  @Autowired private MockMvc restMock;

  @Autowired private SchemaController ctrl;

  @Autowired private StorageImplementationFactory storageImplementationFactory;

  @Autowired private SchemaReader schemaReader;

  @Autowired private ObjectMapper mapper;

  private void clearStorage() {
    Schema schema = schemaReader.getSchema("urn:ietf:params:scim:schemas:core:2.0:Group");
    storageImplementationFactory.getStorageImplementation(schema).deleteAll();

    schema = schemaReader.getSchema("urn:ietf:params:scim:schemas:core:2.0:User");
    storageImplementationFactory.getStorageImplementation(schema).deleteAll();
  }

  @Test
  void addUser() throws IOException, Exception {

    clearStorage();

    String u1 = createUser("/test-reqs/user_create.json");
    String u2 = createUser("/test-reqs/user_create2.json");

    printUsers();

    String g1 = createGroup("/test-reqs/group_create.json");

    printGroups();

    patchEntity("Group", g1, "/test-reqs/group_patch.json");
  }

  @Test
  public void createUsers() throws UnsupportedEncodingException, IOException, Exception {
    clearStorage();

    String u2 = createUser("/test-reqs/user_create2.json");

    patchEntity("user", u2, "/test-reqs/user_patch1.json");
    printPayload(getEntity("user", u2));

    patchEntity("user", u2, "/test-reqs/user_patch2.json");
    printPayload(getEntity("user", u2));
    patchEntity("user", u2, "/test-reqs/user_patch3.json");
    printPayload(getEntity("user", u2));
  }

  @Test
  public void updateUser() throws UnsupportedEncodingException, IOException, Exception {
    clearStorage();

    String u2 = createUser("/test-reqs/user_create3.json");

    putEntity("user", u2, "/test-reqs/user_modify3.json");
    printPayload(getEntity("user", u2));
  }

  private void putEntity(String entityType, String id, String jsonFile)
      throws IOException, Exception {

    Map<String, Object> ent = readEntity(getJson(jsonFile));
    ent.put(Constants.ID, id);

    restMock
        .perform(
            put("/scim/v2/" + StringUtils.capitalize(entityType) + "s/" + id)
                .with(httpBasic("scim-user", "changeit"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(ent))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().is2xxSuccessful())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

  private Map<String, Object> readEntity(String payload)
      throws JsonMappingException, JsonProcessingException {
    TypeReference<HashMap<String, Object>> typeRef =
        new TypeReference<HashMap<String, Object>>() {};
    return mapper.readValue(payload, typeRef);
  }

  private void printPayload(String payload) throws JsonMappingException, JsonProcessingException {
    Map<String, Object> ent = readEntity(payload);
    System.err.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(ent));
  }

  private void patchEntity(String entityType, String id, String jsonFile)
      throws IOException, Exception {
    restMock
        .perform(
            patch("/scim/v2/" + StringUtils.capitalize(entityType) + "s/" + id)
                .with(httpBasic("scim-user", "changeit"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getJson(jsonFile))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().is2xxSuccessful())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

  private void printGroups() throws Exception {
    restMock
        .perform(
            get("/scim/v2/Groups")
                .with(httpBasic("scim-user", "changeit"))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andDo(
            r -> {
              printPayload(r.getResponse().getContentAsString());
            })
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

  private String createGroup(String jsonFile)
      throws UnsupportedEncodingException, Exception, IOException {
    String groupRes =
        restMock
            .perform(
                post("/scim/v2/Groups")
                    .with(httpBasic("scim-user", "changeit"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(getJson(jsonFile))
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String g1 = JsonPath.read(groupRes, "$.id");
    return g1;
  }

  private void printUsers() throws Exception {
    restMock
        .perform(
            get("/scim/v2/Users")
                .with(httpBasic("scim-user", "changeit"))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andDo(
            r -> {
              printPayload(r.getResponse().getContentAsString());
            })
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

  private String createUser(String jsonFile)
      throws UnsupportedEncodingException, Exception, IOException {
    String res =
        restMock
            .perform(
                post("/scim/v2/Users")
                    .with(httpBasic("scim-user", "changeit"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(getJson(jsonFile))
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String u1 = JsonPath.read(res, "$.id");
    return u1;
  }

  private String getEntity(String type, String id)
      throws UnsupportedEncodingException, Exception, IOException {
    String res =
        restMock
            .perform(
                get("/scim/v2/" + StringUtils.capitalize(type) + "s/" + id)
                    .with(httpBasic("scim-user", "changeit"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return res;
  }
}
