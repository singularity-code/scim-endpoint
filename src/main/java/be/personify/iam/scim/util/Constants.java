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
package be.personify.iam.scim.util;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Constants {

	public static final String DATEFORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

	public static final String SCHEMA_SERVICEPROVIDERCONFIG = "urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig";
	public static final String SCHEMA_LISTRESPONSE = "urn:ietf:params:scim:api:messages:2.0:ListResponse";
	public static final String SCHEMA_BULKREQUEST = "urn:ietf:params:scim:api:messages:2.0:BulkRequest";
	public static final String SCHEMA_BULKRESPONSE = "urn:ietf:params:scim:api:messages:2.0:BulkResponse";
	public static final String SCHEMA_PATCHOP = "urn:ietf:params:scim:api:messages:2.0:PatchOp";

	public static final String[] SCHEMA_ERROR = new String[] { "urn:ietf:params:scim:api:messages:2.0:Error" };

	public static final String RESOURCE_TYPE_USER = "User";
	public static final String RESOURCE_TYPE_GROUP = "Group";
	public static final String RESOURCE_TYPE_SERVICEPROVIDERCONFIG = "ServiceProviderConfig";

	public static final String KEY_SCHEMAS = "schemas";
	public static final String KEY_RESOURCE_TYPE = "resourceType";
	public static final String KEY_CREATED = "created";
	public static final String KEY_LAST_MODIFIED = "lastModified";
	public static final String KEY_LOCATION = "location";
	public static final String KEY_VERSION = "version";
	public static final String KEY_META = "meta";
	public static final String KEY_STARTINDEX = "startIndex";
	public static final String KEY_ITEMSPERPAGE = "itemsPerPage";
	public static final String KEY_TOTALRESULTS = "totalResults";
	public static final String KEY_RESOURCES = "Resources";
	public static final String KEY_METHOD = "method";
	public static final String KEY_BULKID = "bulkId";
	public static final String KEY_OPERATIONS = "Operations";
	public static final String KEY_OP = "op";
	public static final String KEY_PATH = "path";
	public static final String KEY_DATA = "data";
	public static final String KEY_VALUE = "value";
	public static final String KEY_MEMBERS = "members";
	public static final String KEY_GROUPS = "groups";
	public static final String KEY_STATUS = "status";
	public static final String KEY_DETAIL = "detail";
	public static final String KEY_CODE = "code";
	public static final String KEY_SCIMTYPE = "scimType";

	public static final ObjectMapper objectMapper = new ObjectMapper();

	public static final String AND_WITH_SPACES = " and ";
	public static final String OR_WITH_SPACES = " or ";
	public static final String QUESTION_MARK__WITH_SPACES = " ? ";

	public static final String ID = "id";

	public static final String RETURNED_NEVER = "never";
	public static final String RETURNED_DEFAULT = "default";
	public static final String RETURNED_ALWAYS = "always";
	public static final String RETURNED_REQUEST = "request";

	public static final String HEADER_LOCATION = "Location";

	public static final String tempDir = System.getProperty("java.io.tmpdir");
	
	public static final String HTTP_METHOD_POST = "POST";
	public static final String HTTP_METHOD_GET = "GET";
	public static final String SEARCH = "SEARCH";

	public static final String BASIC = "Basic";
	public static final String BEARER = "Bearer";

	public static final String CLIENT_ID = "client_id";
	public static final String CLIENT_SECRET = "client_secret";

	public static final String WHERE = " where ";
	public static final String COUNT = "count";
	
	

}
