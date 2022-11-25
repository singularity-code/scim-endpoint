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
package be.personify.iam.scim.schema;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic Schema class
 *
 * @author wouter
 */
public class Schema {

  private String id;
  private String name;
  private String description;
  private List<SchemaAttribute> attributes;
  private SchemaMeta meta;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<SchemaAttribute> getAttributes() {
    return attributes;
  }

  public void setAttributes(List<SchemaAttribute> attributes) {
    this.attributes = attributes;
  }

  public SchemaMeta getMeta() {
    return meta;
  }

  public void setMeta(SchemaMeta meta) {
    this.meta = meta;
  }

  public SchemaAttribute getAttribute(String name) {
    for (SchemaAttribute attr : getAttributes()) {
      if (attr.getName().equals(name)) {
        return attr;
      }
    }
    return null;
  }
  
  
  public List<String> getAttributeNames() {
	  List<String> attributeNames = new ArrayList<>();
	  for (SchemaAttribute attr : getAttributes()) {
		  attributeNames.add(attr.getName());
	  }
	  return attributeNames;
  }
  
  
  
}
