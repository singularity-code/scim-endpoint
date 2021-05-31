package be.personify.iam.scim;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import be.personify.iam.scim.init.Application;
import be.personify.iam.scim.rest.SchemaController;
import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
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

  @Test
  void addUser() throws IOException, Exception {

    String res =
        restMock
            .perform(
                post("/scim/v2/Users")
                    .with(httpBasic("scim-user", "changeit"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(getJson("/test-reqs/user_create.json"))
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn()
            .getResponse()
            .getContentAsString();

    System.err.println("Resp:" + res);
    String u1 = JsonPath.read(res, "$.id");

    restMock
        .perform(
            post("/scim/v2/Users")
                .with(httpBasic("scim-user", "changeit"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getJson("/test-reqs/user_create2.json"))
                .accept(MediaType.APPLICATION_JSON))
        //        .andDo(
        //            r -> {
        //              System.err.println("Resp:" + r.getResponse().getContentAsString());
        //            })
        .andExpect(status().is2xxSuccessful())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));

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

    String groupRes =
        restMock
            .perform(
                post("/scim/v2/Groups")
                    .with(httpBasic("scim-user", "changeit"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(getJson("/test-reqs/group_create.json"))
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn()
            .getResponse()
            .getContentAsString();
    System.err.println("Resp:" + res);
    String g1 = JsonPath.read(groupRes, "$.id");

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

    //    restMock
    //        .perform(
    //            patch("/scim/v2/Groups/" + g1)
    //                .with(httpBasic("scim-user", "changeit"))
    //                .contentType(MediaType.APPLICATION_JSON)
    //                .content(getJson("/test-reqs/group_patch.json"))
    //                .accept(MediaType.APPLICATION_JSON))
    //        .andDo(
    //            r -> {
    //              System.err.println("Resp:" + r.getResponse().getContentAsString());
    //            })
    //        .andExpect(status().is2xxSuccessful())
    //        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }
}
