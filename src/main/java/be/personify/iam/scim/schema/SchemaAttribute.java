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

import java.util.List;

/**
 * Basic SchemaAttribute class
 *
 * @author wouter
 */
public class SchemaAttribute {

  private String name;
  private String type;
  private boolean multiValued;
  private String description;
  private boolean required;
  private boolean caseExact;
  private String mutability;
  private String returned;
  private String uniqueness;
  private String[] referenceTypes;
  private String[] canonicalValues;

  private List<SchemaAttribute> subAttributes;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public boolean isMultiValued() {
    return multiValued;
  }

  public void setMultiValued(boolean multiValued) {
    this.multiValued = multiValued;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isRequired() {
    return required;
  }

  public void setRequired(boolean required) {
    this.required = required;
  }

  public boolean isCaseExact() {
    return caseExact;
  }

  public void setCaseExact(boolean caseExact) {
    this.caseExact = caseExact;
  }

  public String getMutability() {
    return mutability;
  }

  public void setMutability(String mutability) {
    this.mutability = mutability;
  }

  public String getReturned() {
    return returned;
  }

  public void setReturned(String returned) {
    this.returned = returned;
  }

  public List<SchemaAttribute> getSubAttributes() {
    return subAttributes;
  }

  public void setSubAttributes(List<SchemaAttribute> subAttributes) {
    this.subAttributes = subAttributes;
  }

  public String getUniqueness() {
    return uniqueness;
  }

  public void setUniqueness(String uniqueness) {
    this.uniqueness = uniqueness;
  }

  public String[] getReferenceTypes() {
    return referenceTypes;
  }

  public void setReferenceTypes(String[] referenceTypes) {
    this.referenceTypes = referenceTypes;
  }

  public String[] getCanonicalValues() {
    return canonicalValues;
  }

  public void setCanonicalValues(String[] canonicalValues) {
    this.canonicalValues = canonicalValues;
  }

  public SchemaAttribute getSubAttribute(String name) {
    for (SchemaAttribute attr : getSubAttributes()) {
      if (attr.getName().equals(name)) {
        return attr;
      }
    }
    return null;
  }
}
