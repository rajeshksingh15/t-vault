// =========================================================================
// Copyright 2019 T-Mobile, US
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// See the readme.txt file for additional language around disclaimer of warranties.
// =========================================================================

package com.tmobile.cso.vault.api.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.bidimap.DualHashBidiMap;

public class TVaultConstants {
    public static final String READ_POLICY="read";
    public static final String WRITE_POLICY="write";
    public static final String DENY_POLICY="deny";
    public static final String OWNER_POLICY="owner";
    public static final String SUDO_POLICY="sudo";
    public static final String CREATE_POLICY="create";
    public static final String ADD_USER = "addUser";
    public static final String FALSE = "false";
    public static final String REMOVE_USER = "removeUser";
    public static final String SAFE = "safe";
    public static final String FOLDER = "folder";
    public static final String APPS = "apps";
    public static final String SHARED = "shared";
    public static final String USERS = "users";
    public static final String GROUPS = "groups";
    public static final String AWS_ROLES = "aws-roles";
    public static final String EMPTY = "";
    public static final String USERPASS = "userpass";
    public static final String LDAP = "ldap";
    public static final String DELETE = "delete";
    public static final String IAM = "iam";
    public static final String CREATE ="create";
    public static final String UPDATE ="update";
    public static final String UNKNOWN = "unknown";
    public static final String EMPTY_JSON = "{}";
    public static final String APPROLE = "approle";
    public static final String APPROLE_METADATA_MOUNT_PATH = "metadata/approle";
    public static final String APPROLE_USERS_METADATA_MOUNT_PATH = "metadata/approle_users";
    public static final String AWSROLE_METADATA_MOUNT_PATH = "metadata/awsrole";
    public static final String SELF_SERVICE_APPROLE_NAME = "selfservicesupportrole";
    public static final String SECRET = "secret";
    public static final boolean HIDEMASTERAPPROLE = true;
    public static final String APPROLE_DELETE_OPERATION="DELETE";
    public static final String APPROLE_READ_OPERATION="READ";
    public static final String APPROLE_UPDATE_OPERATION="UPDATE";
    public static final String SVC_ACC_PATH_PREFIX="svcacct";
    protected static final Map<String, String> SVC_ACC_POLICIES;
    public static final DualHashBidiMap SVC_ACC_POLICIES_PREFIXES ;
    static {
    	SVC_ACC_POLICIES = Collections.synchronizedMap(new HashMap<String, String>());
    	SVC_ACC_POLICIES.put("r_", TVaultConstants.READ_POLICY);
    	SVC_ACC_POLICIES.put("w_", TVaultConstants.WRITE_POLICY);
    	SVC_ACC_POLICIES.put("o_", TVaultConstants.SUDO_POLICY);
    	SVC_ACC_POLICIES.put("d_", TVaultConstants.DENY_POLICY);
    	SVC_ACC_POLICIES_PREFIXES = new DualHashBidiMap(SVC_ACC_POLICIES);
    }
    public static final String SVC_ACC_CREDS_PATH="ad/creds/";
	/**
	 * @return the svcAccPolicies
	 */
	public static Map<String, String> getSvcAccPolicies() {
		return SVC_ACC_POLICIES;
	}

    public static final String ADD_GROUP = "addGroup";
    public static final String REMOVE_GROUP = "removeGroup";
    public static final String SVC_ACC_ROLES_METADATA_MOUNT_PATH = "metadata/ad/roles";
    public static final String SVC_ACC_ROLES_PATH = "ad/roles";
    public static final Long PASSWORD_AUTOROTATE_TTL_MAX_VALUE = 1590897977L;
}
