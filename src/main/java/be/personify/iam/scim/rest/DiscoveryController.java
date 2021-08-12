package be.personify.iam.scim.rest;

import be.personify.iam.scim.schema.SchemaReader;
import be.personify.iam.scim.util.Constants;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Discovery controller for the SCIM server
 *
 * @author wouter
 */
@RestController
public class DiscoveryController extends Controller {

  private Map<String, Object> serviceProviderConfig = null;
  private List<Object> resourceTypes = null;
  private List<Object> schemas = null;

  @Autowired private SchemaReader schemaReader;

  @GetMapping(
      path = "/scim/v2/ServiceProviderConfig",
      produces = {"application/scim+json", "application/json"})
  public ResponseEntity<?> getServiceProviderConfig(
      HttpServletRequest request, HttpServletResponse response) {

    long start = System.currentTimeMillis();
    ResponseEntity<?> result = null;
    try {
      if (serviceProviderConfig == null) {
        serviceProviderConfig =
            Constants.objectMapper.readValue(
                DiscoveryController.class.getResourceAsStream("/disc_service_provider_config.json"),
                Map.class);
      }
      result = new ResponseEntity<Map<String, Object>>(serviceProviderConfig, HttpStatus.OK);
    } catch (IOException e) {
      logger.error("can not read service provider config", e);
      result = new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    logger.info("ServiceProviderConfig fetched in {} ms", (System.currentTimeMillis() - start));

    return result;
  }

  @GetMapping(
      path = "/scim/v2/ResourceTypes",
      produces = {"application/scim+json", "application/json"})
  public ResponseEntity<?> getResourceTypes(
      HttpServletRequest request, HttpServletResponse response) {

    long start = System.currentTimeMillis();
    ResponseEntity<?> result = null;
    try {
      if (resourceTypes == null) {
        resourceTypes =
            Constants.objectMapper.readValue(
                DiscoveryController.class.getResourceAsStream("/disc_resource_types.json"),
                List.class);
      }
      result = new ResponseEntity<List<Object>>(resourceTypes, HttpStatus.OK);
    } catch (IOException e) {
      logger.error("can not read resource types", e);
      result = new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    logger.info("resource types fetched in {} ms", (System.currentTimeMillis() - start));

    return result;
  }

  @GetMapping(
      path = "/scim/v2/Schemas",
      produces = {"application/scim+json", "application/json"})
  public ResponseEntity<?> getSchemas(HttpServletRequest request, HttpServletResponse response) {

    long start = System.currentTimeMillis();
    ResponseEntity<?> result = null;
    try {
      if (schemas == null) {
        schemas = schemaReader.getSchemas();
      }
      result = new ResponseEntity<List<Object>>(schemas, HttpStatus.OK);
    } catch (Exception e) {
      logger.error("can not read schemas", e);
      result = new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    logger.info("schemas fetched in {} ms", (System.currentTimeMillis() - start));

    return result;
  }
}
