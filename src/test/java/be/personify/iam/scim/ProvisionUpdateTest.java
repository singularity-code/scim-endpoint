package be.personify.iam.scim;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import be.personify.iam.scim.init.Application;
import be.personify.iam.scim.rest.SchemaController;
import be.personify.iam.scim.schema.Schema;
import be.personify.iam.scim.schema.SchemaReader;
import be.personify.iam.scim.storage.StorageImplementationFactory;
import be.personify.util.StringUtils;
import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
    System.err.println(getEntity("user", u2));

    patchEntity("user", u2, "/test-reqs/user_patch2.json");
    System.err.println(getEntity("user", u2));
    patchEntity("user", u2, "/test-reqs/user_patch3.json");
    System.err.println(getEntity("user", u2));
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
              System.err.println("Groups:\n" + r.getResponse().getContentAsString());
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
              System.err.println("Users:\n" + r.getResponse().getContentAsString());
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
