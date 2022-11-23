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
package be.personify.iam.scim.init;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main spring-boot application for the JAVA SCIM server implementation
 *
 * @author wouter
 */
@SpringBootApplication
@ComponentScan(basePackages = {"be.personify.iam.scim"})
@EnableAutoConfiguration(exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
public class Application {

  public static void main(String[] args) {
	  //System.setProperty("javax.net.debug", "ssl:handshake");
	  //System.setProperty("javax.net.debug", "all");
	  
    SpringApplication.run(Application.class, args);
  }
}
