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

import javax.servlet.Filter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import be.personify.iam.scim.authentication.AuthenticationUtils;
import be.personify.iam.scim.rest.PatchUtils;
import be.personify.iam.scim.schema.SchemaReader;
import be.personify.iam.scim.util.CryptUtils;
import be.personify.iam.scim.util.PropertyFactory;
import be.personify.iam.scim.util.TokenUtils;

@Configuration
@EnableScheduling
@EnableAsync
public class SpringConfig {

	@Bean
	public FilterRegistrationBean<Filter> authenticationFilters() {
		FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
		Filter filter;
		try {
			filter = authenticationUtils().getFilterImplementation();
			registrationBean.setFilter(filter);
			return registrationBean;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Bean
	public CryptUtils cryptUtils() {
		return new CryptUtils();
	}

	@Bean
	public TokenUtils tokenUtils() {
		return new TokenUtils();
	}

	@Bean
	public AuthenticationUtils authenticationUtils() {
		return new AuthenticationUtils();
	}

	@Bean
	public PropertyFactory propertyFactory() {
		return new PropertyFactory();
	}

	@Bean
	public SchemaReader schemaReader() {
		return new SchemaReader();
	}
	
	@Bean
	public PatchUtils patchUtils() {
		return new PatchUtils();
	}


//  @Bean
//  public CommonsRequestLoggingFilter requestLoggingFilter() {
//    CommonsRequestLoggingFilter loggingFilter = new CommonsRequestLoggingFilter();
//    loggingFilter.setIncludeClientInfo(true);
//    loggingFilter.setIncludeQueryString(true);
//    loggingFilter.setIncludePayload(true);
//    loggingFilter.setMaxPayloadLength(64000);
//    loggingFilter.setIncludeHeaders(true);
//    return loggingFilter;
//  }
	
	
	@Bean
    public CommonsRequestLoggingFilter logFilter() {
        CommonsRequestLoggingFilter filter
          = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(10000);
        filter.setIncludeHeaders(false);
        filter.setAfterMessagePrefix("REQUEST DATA : ");
        return filter;
    }
	
}
