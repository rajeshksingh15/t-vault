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
package com.tmobile.cso.vault.api.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.utils.PolicyUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ControllerUtil.class, JSONUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class ServiceAccountsServiceTest {

    @InjectMocks
    ServiceAccountsService serviceAccountsService;

    @Mock
    AccessService accessService;

    @Mock
    RequestProcessor reqProcessor;

    @Mock
    LdapTemplate ldapTemplate;

    @Mock
    PolicyUtils policyUtils;


    @Before
    public void setUp() {
        PowerMockito.mockStatic(ControllerUtil.class);
        PowerMockito.mockStatic(JSONUtil.class);

        Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));
        when(JSONUtil.getJSON(Mockito.any(ImmutableMap.class))).thenReturn("log");

        Map<String, String> currentMap = new HashMap<>();
        currentMap.put("apiurl", "http://localhost:8080/vault/v2/ad");
        ReflectionTestUtils.setField(serviceAccountsService, "vaultAuthMethod", "ldap");
        Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));
        when(JSONUtil.getJSON(any(ImmutableMap.class))).thenReturn("log");

        currentMap.put("user", "");
        ThreadLocalContext.setCurrentMap(currentMap);
    }

    Response getMockResponse(HttpStatus status, boolean success, String expectedBody) {
        Response response = new Response();
        response.setHttpstatus(status);
        response.setSuccess(success);
        if (expectedBody!="") {
            response.setResponse(expectedBody);
        }
        return response;
    }

    UserDetails getMockUser(boolean isAdmin) {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = new UserDetails();
        userDetails.setUsername("normaluser");
        userDetails.setAdmin(isAdmin);
        userDetails.setClientToken(token);
        userDetails.setSelfSupportToken(token);
        return userDetails;
    }

    private ADServiceAccount generateADServiceAccount(String  userid) {
        ADServiceAccount adServiceAccount = new ADServiceAccount();
        adServiceAccount.setDisplayName("testacc");
        adServiceAccount.setGivenName("testacc");
        adServiceAccount.setUserEmail("testacc@t-mobile.com");
        adServiceAccount.setUserId(userid);
        adServiceAccount.setUserName("testaccr");
        adServiceAccount.setPurpose("This is a test user account");
        adServiceAccount.setAccountExpires("292239827-01-08 11:35:09");
        adServiceAccount.setMaxPwdAge(90);
        adServiceAccount.setAccountStatus("active");
        adServiceAccount.setLockStatus("active");
        return adServiceAccount;
    }
    private List<ADServiceAccount> generateADSerivceAccounts() {
    	List<ADServiceAccount> allServiceAccounts = new ArrayList<ADServiceAccount>();
    	allServiceAccounts.add(generateADServiceAccount("testacc01"));
    	return allServiceAccounts;
    }


    private ADServiceAccountObjects generateADServiceAccountObjects(List<ADServiceAccount> allServiceAccounts) {
		ADServiceAccountObjects adServiceAccountObjects = new ADServiceAccountObjects();
		ADServiceAccountObjectsList adServiceAccountObjectsList = new ADServiceAccountObjectsList();
		if (!CollectionUtils.isEmpty(allServiceAccounts)) {
			adServiceAccountObjectsList.setValues(allServiceAccounts.toArray(new ADServiceAccount[allServiceAccounts.size()]));
		}
		adServiceAccountObjects.setData(adServiceAccountObjectsList);
		return adServiceAccountObjects;
    }
    @Test
    public void test_getADServiceAccounts_excluded_success() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	String userPrincipalName = "test";
    	boolean excludeOnboarded = true;
    	String encodedFilter = "(&(userPrincipalName=test*)(objectClass=user)(!(CN=null))(!(CN=testacc02)))";
        List<ADServiceAccount> allServiceAccounts = generateADSerivceAccounts();
        ResponseEntity<ADServiceAccountObjects> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(generateADServiceAccountObjects(allServiceAccounts));

        when(ldapTemplate.search(Mockito.anyString(), Mockito.eq(encodedFilter), Mockito.any(AttributesMapper.class))).thenReturn(allServiceAccounts);

        Response response = getMockResponse(HttpStatus.OK, true, "{\"keys\":[\"testacc02\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token)).thenReturn(response);

        ResponseEntity<ADServiceAccountObjects> responseEntity = serviceAccountsService.getADServiceAccounts(token, userDetails, userPrincipalName, excludeOnboarded);


        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(1, responseEntity.getBody().getData().getValues().length);

    }


    @Test
    public void test_getADServiceAccounts_excluded_success_NoResults() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	String userPrincipalName = "test";
    	boolean excludeOnboarded = true;
    	String encodedFilter = "(&(userPrincipalName=test*)(objectClass=user)(!(CN=null))(!(CN=testacc02)))";
        List<ADServiceAccount> allServiceAccounts = generateADSerivceAccounts();
        allServiceAccounts.add(generateADServiceAccount("testacc02"));
        ResponseEntity<ADServiceAccountObjects> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(generateADServiceAccountObjects(allServiceAccounts));

        when(ldapTemplate.search(Mockito.anyString(), Mockito.eq(encodedFilter), Mockito.any(AttributesMapper.class))).thenReturn(allServiceAccounts);

        Response response = getMockResponse(HttpStatus.OK, true, "{\"keys\":[]}");
        when(reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token)).thenReturn(response);
        ResponseEntity<ADServiceAccountObjects> responseEntity = serviceAccountsService.getADServiceAccounts(token, userDetails, userPrincipalName, excludeOnboarded);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(0, responseEntity.getBody().getData().getValues().length);

    }

    @Test
    public void test_getADServiceAccounts_excluded_success_BadRequest_Onboarded() {
		UserDetails userDetails = getMockUser(false);
    	String token = userDetails.getClientToken();
    	String userPrincipalName = "test";
    	boolean excludeOnboarded = true;
    	String encodedFilter = "(&(userPrincipalName=test*)(objectClass=user)(!(CN=null)))";
        List<ADServiceAccount> allServiceAccounts = generateADSerivceAccounts();
        allServiceAccounts.add(generateADServiceAccount("testacc02"));
        ResponseEntity<ADServiceAccountObjects> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(generateADServiceAccountObjects(allServiceAccounts));

        when(ldapTemplate.search(Mockito.anyString(), Mockito.eq(encodedFilter), Mockito.any(AttributesMapper.class))).thenReturn(allServiceAccounts);
        String expectedMsg = "{\"errors\":[\"TO BE IMPLEMENTED for non admin user\"]}";
        Response response = getMockResponse(HttpStatus.BAD_REQUEST, true, expectedMsg);
        when(reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token)).thenReturn(response);
        ResponseEntity<ADServiceAccountObjects> responseEntity = serviceAccountsService.getADServiceAccounts(token, userDetails, userPrincipalName, excludeOnboarded);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(2, responseEntity.getBody().getData().getValues().length);

    }

    @Test
    public void test_getADServiceAccounts_excluded_success_NotFound_Onboarded() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	String userPrincipalName = "test";
    	boolean excludeOnboarded = true;
    	String encodedFilter = "(&(userPrincipalName=test*)(objectClass=user)(!(CN=null)))";
        List<ADServiceAccount> allServiceAccounts = generateADSerivceAccounts();
        allServiceAccounts.add(generateADServiceAccount("testacc02"));
        allServiceAccounts.add(generateADServiceAccount("testacc03"));
        ResponseEntity<ADServiceAccountObjects> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(generateADServiceAccountObjects(allServiceAccounts));

        when(ldapTemplate.search(Mockito.anyString(), Mockito.eq(encodedFilter), Mockito.any(AttributesMapper.class))).thenReturn(allServiceAccounts);

        Response response = getMockResponse(HttpStatus.NOT_FOUND, true, "{\"keys\":[]}");
        when(reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token)).thenReturn(response);
        ResponseEntity<ADServiceAccountObjects> responseEntity = serviceAccountsService.getADServiceAccounts(token, userDetails, userPrincipalName, excludeOnboarded);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(3, responseEntity.getBody().getData().getValues().length);

    }

    @Test
    public void test_getADServiceAccounts_all_successfully_NoAccounts() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	String userPrincipalName = "test";
    	boolean excludeOnboarded = false;
        List<ADServiceAccount> allServiceAccounts = null;
        ResponseEntity<ADServiceAccountObjects> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(generateADServiceAccountObjects(allServiceAccounts));

        when(ldapTemplate.search(Mockito.anyString(), Mockito.anyString(), Mockito.any(AttributesMapper.class))).thenReturn(allServiceAccounts);
        ResponseEntity<ADServiceAccountObjects> responseEntity = serviceAccountsService.getADServiceAccounts(token, userDetails, userPrincipalName, excludeOnboarded);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(0, responseEntity.getBody().getData().getValues().length);

    }

    @Test
    public void test_getADServiceAccounts_all_success() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	String userPrincipalName = "test";
    	boolean excludeOnboarded = false;
        List<ADServiceAccount> allServiceAccounts = generateADSerivceAccounts();
        ResponseEntity<ADServiceAccountObjects> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(generateADServiceAccountObjects(allServiceAccounts));

        when(ldapTemplate.search(Mockito.anyString(), Mockito.anyString(), Mockito.any(AttributesMapper.class))).thenReturn(allServiceAccounts);
        ResponseEntity<ADServiceAccountObjects> responseEntity = serviceAccountsService.getADServiceAccounts(token, userDetails, userPrincipalName, excludeOnboarded);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected.getBody().getData().getValues()[0], responseEntity.getBody().getData().getValues()[0]);

    }

	public String getJSON(Object obj)  {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			return TVaultConstants.EMPTY_JSON;
		}
	}
    private ServiceAccount generateServiceAccount(String svcAccName, String owner) {
    	ServiceAccount serviceAccount = new ServiceAccount();
    	serviceAccount.setName(svcAccName);
    	serviceAccount.setAutoRotate(true);
    	serviceAccount.setTtl(1234L);
    	serviceAccount.setMax_ttl(12345L);
    	serviceAccount.setOwner(owner);
    	return serviceAccount;
    }
    @Test
    public void test_onboardServiceAccount_succss_autorotate_off_Bad_Request() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	ServiceAccount serviceAccount = generateServiceAccount("testacc02","testacc01");
    	serviceAccount.setAutoRotate(false);
    	String expectedResponse = "{\"errors\":[\"TO BE IMPLEMENTED: Auto-Rotate of password has been turned off and this is yet to be implemented\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedResponse);
        ResponseEntity<String> responseEntity = serviceAccountsService.onboardServiceAccount(token, serviceAccount, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
    @Test
    @Ignore
    public void test_onboardServiceAccount_succss_autorotate_on_ttl_biggerthan_maxttl() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	ServiceAccount serviceAccount = generateServiceAccount("testacc02","testacc01");
    	serviceAccount.setAutoRotate(true);
    	serviceAccount.setTtl(1112L);
    	serviceAccount.setMax_ttl(1111L);
    	String expectedResponse = "{\"errors\":[\"ttl can't be more than max_ttl\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedResponse);


        ResponseEntity<String> responseEntity = serviceAccountsService.onboardServiceAccount(token, serviceAccount, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
    @Test
    @Ignore
    public void test_onboardServiceAccount_success() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	ServiceAccount serviceAccount = generateServiceAccount("testacc02","testacc01");
    	serviceAccount.setAutoRotate(true);

        // CreateRole
		ServiceAccountTTL serviceAccountTTL = new ServiceAccountTTL();
		serviceAccountTTL.setRole_name(serviceAccount.getName());
		serviceAccountTTL.setService_account_name(serviceAccount.getName() + "@aaa.bbb.ccc.com") ;
		serviceAccountTTL.setTtl(serviceAccount.getTtl());
		String svc_account_payload = getJSON(serviceAccountTTL);
        when(JSONUtil.getJSON(Mockito.any(ServiceAccountTTL.class))).thenReturn(svc_account_payload);
		Response onboardResponse = getMockResponse(HttpStatus.OK, true, "{\"messages\":[\"Successfully created service account role.\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboard", svc_account_payload, token)).thenReturn(onboardResponse);
        //CreateServiceAccountPolicies
        ResponseEntity<String> createPolicyResponse = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Successfully created policies for service account\"]}");
        when(accessService.createPolicy(Mockito.anyString(), Mockito.any())).thenReturn(createPolicyResponse);

        // Add User to Service Account
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response ldapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testacc01\"}",token)).thenReturn(userResponse);

        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPUser(eq("testacc01"),any(),any(),eq(token))).thenReturn(ldapConfigureResponse);


        // Metadata...
        String path="metadata/ad/roles/"+serviceAccount.getName();
        Map<String,Object> rqstParams = new HashMap<>();
        rqstParams.put("path",path);
        when(ControllerUtil.convetToJson(rqstParams)).thenReturn(getJSON(rqstParams));
        when(ControllerUtil.createMetadata(Mockito.any(), Mockito.anyString())).thenReturn(true);
        when(ControllerUtil.updateMetadata(Mockito.any(), Mockito.anyString())).thenReturn(getMockResponse(HttpStatus.OK, true,"{}"));

        // System under test
    	String expectedResponse = "{\"messages\":[\"Successfully completed onboarding of AD service account into TVault for password rotation.\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedResponse);

        ResponseEntity<String> responseEntity = serviceAccountsService.onboardServiceAccount(token, serviceAccount, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
    @Test
    public void test_onboardServiceAccount_BadRequest_for_AccountRole() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	ServiceAccount serviceAccount = generateServiceAccount("testacc02","testacc01");
    	serviceAccount.setAutoRotate(true);

        // CreateRole
		ServiceAccountTTL serviceAccountTTL = new ServiceAccountTTL();
		serviceAccountTTL.setRole_name(serviceAccount.getName());
		serviceAccountTTL.setService_account_name(serviceAccount.getName() + "@aaa.bbb.ccc.com") ;
		serviceAccountTTL.setTtl(serviceAccount.getTtl());
		String svc_account_payload = getJSON(serviceAccountTTL);
		when(JSONUtil.getJSON(Mockito.any(ServiceAccountTTL.class))).thenReturn(svc_account_payload);
		Response onboardResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, "{\"errors\":[\"Failed to create service account role.\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboard", svc_account_payload, token)).thenReturn(onboardResponse);

        // System under test
    	String expectedResponse = "{\"errors\":[\"Failed to onboard AD service account into TVault for password rotation.\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedResponse);

        ResponseEntity<String> responseEntity = serviceAccountsService.onboardServiceAccount(token, serviceAccount, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
    @Test
    public void test_onboardServiceAccount_BadRequest_for_CreateServiceAccountPolicies() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	ServiceAccount serviceAccount = generateServiceAccount("testacc02","testacc01");
    	serviceAccount.setAutoRotate(true);

        // CreateRole
		ServiceAccountTTL serviceAccountTTL = new ServiceAccountTTL();
		serviceAccountTTL.setRole_name(serviceAccount.getName());
		serviceAccountTTL.setService_account_name(serviceAccount.getName() + "@aaa.bbb.ccc.com") ;
		serviceAccountTTL.setTtl(serviceAccount.getTtl());
		String svc_account_payload = getJSON(serviceAccountTTL);
		when(JSONUtil.getJSON(Mockito.any(ServiceAccountTTL.class))).thenReturn(svc_account_payload);
		Response onboardResponse = getMockResponse(HttpStatus.OK, true, "{\"messages\":[\"Successfully created service account role.\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboard", svc_account_payload, token)).thenReturn(onboardResponse);
        //CreateServiceAccountPolicies
        ResponseEntity<String> createPolicyResponse = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"messages\":[\"Unable to create Policy\"]}");
        when(accessService.createPolicy(Mockito.anyString(), Mockito.any())).thenReturn(createPolicyResponse);
        //Metadata
        String path="metadata/ad/roles/"+serviceAccount.getName();
        Map<String,Object> rqstParams = new HashMap<>();
        rqstParams.put("path",path);
        when(ControllerUtil.convetToJson(rqstParams)).thenReturn(getJSON(rqstParams));
        when(ControllerUtil.createMetadata(Mockito.any(), Mockito.anyString())).thenReturn(true);
        when(ControllerUtil.updateMetadata(Mockito.any(), Mockito.anyString())).thenReturn(getMockResponse(HttpStatus.OK, true,"{}"));
        when(reqProcessor.process("/ad/serviceaccount/offboard", svc_account_payload, token)).thenReturn(getMockResponse(HttpStatus.NO_CONTENT, true,"{}"));
        // System under test
    	String expectedResponse = "{\"errors\":[\"Failed to onboard AD service account into TVault for password rotation.\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(expectedResponse);

        ResponseEntity<String> responseEntity = serviceAccountsService.onboardServiceAccount(token, serviceAccount, userDetails);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_onboardServiceAccount_failed_owner_association() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	ServiceAccount serviceAccount = generateServiceAccount("testacc02","testacc01");
    	serviceAccount.setAutoRotate(true);

        // CreateRole
		ServiceAccountTTL serviceAccountTTL = new ServiceAccountTTL();
		serviceAccountTTL.setRole_name(serviceAccount.getName());
		serviceAccountTTL.setService_account_name(serviceAccount.getName() + "@aaa.bbb.ccc.com") ;
		serviceAccountTTL.setTtl(serviceAccount.getTtl());
		String svc_account_payload = getJSON(serviceAccountTTL);
		when(JSONUtil.getJSON(Mockito.any(ServiceAccountTTL.class))).thenReturn(svc_account_payload);
		Response onboardResponse = getMockResponse(HttpStatus.OK, true, "{\"messages\":[\"Successfully created service account role.\"]}");
        when(reqProcessor.process("/ad/serviceaccount/onboard", svc_account_payload, token)).thenReturn(onboardResponse);
        //CreateServiceAccountPolicies
        ResponseEntity<String> createPolicyResponse = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Successfully created policies for service account\"]}");
        when(accessService.createPolicy(Mockito.anyString(), Mockito.any())).thenReturn(createPolicyResponse);

        // Add User to Service Account
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testacc01\"}",token)).thenReturn(userResponse);

        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Response ldapConfigureResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, "{\"errors\":[\"Failed to add user to the Service Account\"]}");
        when(ControllerUtil.configureLDAPUser(eq("testacc01"),any(),any(),eq(token))).thenReturn(ldapConfigureResponse);
        // Metadata
        String path="metadata/ad/roles/"+serviceAccount.getName();
        Map<String,Object> rqstParams = new HashMap<>();
        rqstParams.put("path",path);
        when(ControllerUtil.convetToJson(rqstParams)).thenReturn(getJSON(rqstParams));
        when(ControllerUtil.createMetadata(Mockito.any(), Mockito.anyString())).thenReturn(true);
        when(ControllerUtil.updateMetadata(Mockito.any(), Mockito.anyString())).thenReturn(getMockResponse(HttpStatus.OK, true,"{}"));

        // System under test
        String expectedResponse = "{\"messages\":[\"Successfully created Service Account Role and policies. However the association of owner information failed.\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.MULTI_STATUS).body(expectedResponse);

        ResponseEntity<String> responseEntity = serviceAccountsService.onboardServiceAccount(token, serviceAccount, userDetails);
        assertEquals(HttpStatus.MULTI_STATUS, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    private OnboardedServiceAccount generateOnboardedServiceAccount(String svcAccName, String owner) {
    	OnboardedServiceAccount onboardedServiceAccount = new OnboardedServiceAccount();
    	onboardedServiceAccount.setName(svcAccName);
    	onboardedServiceAccount.setOwner(owner);
    	return onboardedServiceAccount;
    }
    @Test
    @Ignore
    public void test_offboardServiceAccount_succss() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	OnboardedServiceAccount onboardedServiceAccount = generateOnboardedServiceAccount("testacc02","testacc01");

    	// Remove user...
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\", \"o_svcacct_testacc02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response ldapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testacc01\"}",token)).thenReturn(userResponse);
        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("o_svcacct_testacc02");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPUser(eq("testacc01"),any(),any(),eq(token))).thenReturn(ldapConfigureResponse);

        // Delete policies...
        ResponseEntity<String> createPolicyResponse = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Access is deleted\"]}");
        when(accessService.deletePolicyInfo(Mockito.anyString(), Mockito.any())).thenReturn(createPolicyResponse);

        //Delete Account Role...
		ServiceAccountTTL serviceAccountTTL = new ServiceAccountTTL();
		serviceAccountTTL.setRole_name(onboardedServiceAccount.getName());
		serviceAccountTTL.setService_account_name(onboardedServiceAccount.getName() + "@aaa.bbb.ccc.com") ;
		String svc_account_payload = getJSON(serviceAccountTTL);
		String deleteRoleResponseMsg = "{\"messages\":[\"Successfully deleted service account role.\"]}";
		Response deleteRoleResponse = getMockResponse(HttpStatus.OK, true, deleteRoleResponseMsg);
		when(JSONUtil.getJSON(Mockito.any(ServiceAccountTTL.class))).thenReturn(svc_account_payload);
        when(reqProcessor.process("/ad/serviceaccount/offboard",svc_account_payload,token)).thenReturn(deleteRoleResponse);

        // System under test
    	String expectedResponse = "{\"messages\":[\"Successfully completed offboarding of AD service account from TVault for password rotation.\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedResponse);
        ResponseEntity<String> responseEntity = serviceAccountsService.offboardServiceAccount(token, onboardedServiceAccount, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    @Ignore
    public void test_offboardServiceAccount_failed_to_removeUser() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	OnboardedServiceAccount onboardedServiceAccount = generateOnboardedServiceAccount("testacc02","testacc01");

    	// Remove user...
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\", \"o_svcacct_testacc02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response ldapConfigureResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, "{\"errors\":[\"Failed to remvoe the user from the Service Account\"]}");
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testacc01\"}",token)).thenReturn(userResponse);
        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("o_svcacct_testacc02");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPUser(eq("testacc01"),any(),any(),eq(token))).thenReturn(ldapConfigureResponse);

        // Delete policies...
        ResponseEntity<String> createPolicyResponse = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Access is deleted\"]}");
        when(accessService.deletePolicyInfo(Mockito.anyString(), Mockito.any())).thenReturn(createPolicyResponse);

        //Delete Account Role...
		ServiceAccountTTL serviceAccountTTL = new ServiceAccountTTL();
		serviceAccountTTL.setRole_name(onboardedServiceAccount.getName());
		serviceAccountTTL.setService_account_name(onboardedServiceAccount.getName() + "@aaa.bbb.ccc.com") ;
		String svc_account_payload = getJSON(serviceAccountTTL);
		String deleteRoleResponseMsg = "{\"messages\":[\"Successfully deleted service account role.\"]}";
		Response deleteRoleResponse = getMockResponse(HttpStatus.OK, true, deleteRoleResponseMsg);
		when(JSONUtil.getJSON(Mockito.any(ServiceAccountTTL.class))).thenReturn(svc_account_payload);
        when(reqProcessor.process("/ad/serviceaccount/offboard",svc_account_payload,token)).thenReturn(deleteRoleResponse);

        // System under test
    	String expectedResponse = "{\"messages\":[\"Successfully completed offboarding of AD service account from TVault for password rotation.\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedResponse);
        ResponseEntity<String> responseEntity = serviceAccountsService.offboardServiceAccount(token, onboardedServiceAccount, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    @Ignore
    public void test_offboardServiceAccount_failed_to_deletePolicies() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	OnboardedServiceAccount onboardedServiceAccount = generateOnboardedServiceAccount("testacc02","testacc01");

    	// Remove user...
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\", \"o_svcacct_testacc02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response ldapConfigureResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, "{\"errors\":[\"Failed to remvoe the user from the Service Account\"]}");
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testacc01\"}",token)).thenReturn(userResponse);
        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("o_svcacct_testacc02");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPUser(eq("testacc01"),any(),any(),eq(token))).thenReturn(ldapConfigureResponse);

        // Delete policies...
        ResponseEntity<String> createPolicyResponse = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"messages\":[\"Deletion of Policy information failed\"]}");
        when(accessService.deletePolicyInfo(Mockito.anyString(), Mockito.any())).thenReturn(createPolicyResponse);

        //Delete Account Role...
		ServiceAccountTTL serviceAccountTTL = new ServiceAccountTTL();
		serviceAccountTTL.setRole_name(onboardedServiceAccount.getName());
		serviceAccountTTL.setService_account_name(onboardedServiceAccount.getName() + "@aaa.bbb.ccc.com") ;
		String svc_account_payload = getJSON(serviceAccountTTL);
		String deleteRoleResponseMsg = "{\"messages\":[\"Successfully deleted service account role.\"]}";
		Response deleteRoleResponse = getMockResponse(HttpStatus.OK, true, deleteRoleResponseMsg);
		when(JSONUtil.getJSON(Mockito.any(ServiceAccountTTL.class))).thenReturn(svc_account_payload);
        when(reqProcessor.process("/ad/serviceaccount/offboard",svc_account_payload,token)).thenReturn(deleteRoleResponse);

        // Metadata
        String path="metadata/ad/roles/"+onboardedServiceAccount.getName();
        Map<String,Object> rqstParams = new HashMap<>();
        rqstParams.put("path",path);
        when(ControllerUtil.convetToJson(rqstParams)).thenReturn(getJSON(rqstParams));
        when(ControllerUtil.createMetadata(Mockito.any(), Mockito.anyString())).thenReturn(true);
        when(ControllerUtil.updateMetadata(Mockito.any(), Mockito.anyString())).thenReturn(getMockResponse(HttpStatus.OK, true,"{}"));

        // System under test
    	String expectedResponse = "{\"messages\":[\"Successfully completed offboarding of AD service account from TVault for password rotation.\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedResponse);
        ResponseEntity<String> responseEntity = serviceAccountsService.offboardServiceAccount(token, onboardedServiceAccount, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    @Ignore
    public void test_offboardServiceAccount_failed_to_deleteAccountRole() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	OnboardedServiceAccount onboardedServiceAccount = generateOnboardedServiceAccount("testacc02","testacc01");

    	// Remove user...
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\", \"o_svcacct_testacc02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response ldapConfigureResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, "{\"errors\":[\"Failed to remvoe the user from the Service Account\"]}");
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testacc01\"}",token)).thenReturn(userResponse);
        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("o_svcacct_testacc02");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPUser(eq("testacc01"),any(),any(),eq(token))).thenReturn(ldapConfigureResponse);

        // Delete policies...
        ResponseEntity<String> createPolicyResponse = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"messages\":[\"Deletion of Policy information failed\"]}");
        when(accessService.deletePolicyInfo(Mockito.anyString(), Mockito.any())).thenReturn(createPolicyResponse);

        //Delete Account Role...
		ServiceAccountTTL serviceAccountTTL = new ServiceAccountTTL();
		serviceAccountTTL.setRole_name(onboardedServiceAccount.getName());
		serviceAccountTTL.setService_account_name(onboardedServiceAccount.getName() + "@aaa.bbb.ccc.com") ;
		String svc_account_payload = getJSON(serviceAccountTTL);
		String deleteRoleResponseMsg = "{\"errors\":[\"Failed to delete service account role.\"]}";
		Response deleteRoleResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, deleteRoleResponseMsg);
		when(JSONUtil.getJSON(Mockito.any(ServiceAccountTTL.class))).thenReturn(svc_account_payload);
        when(reqProcessor.process("/ad/serviceaccount/offboard",svc_account_payload,token)).thenReturn(deleteRoleResponse);

        // System under test
    	String expectedResponse = "{\"errors\":[\"Failed to offboard AD service account from TVault for password rotation.\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.MULTI_STATUS).body(expectedResponse);
        ResponseEntity<String> responseEntity = serviceAccountsService.offboardServiceAccount(token, onboardedServiceAccount, userDetails);
        assertEquals(HttpStatus.MULTI_STATUS, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    @Ignore
    public void test_addUserToServiceAccount_ldap_success() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	ServiceAccountUser serviceAccountUser = new ServiceAccountUser("testacc02", "testacc01", "read");
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response ldapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testacc01\"}",token)).thenReturn(userResponse);

        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPUser(eq("testacc01"),any(),any(),eq(token))).thenReturn(ldapConfigureResponse);
        // System under test
    	String expectedResponse = "{\"errors\":[\"Successfully added user to the Service Account\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedResponse);
        ResponseEntity<String> responseEntity = serviceAccountsService.addUserToServiceAccount(token, serviceAccountUser, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
    @Test
    @Ignore
    public void test_addUserToServiceAccount_userpass_success() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	ServiceAccountUser serviceAccountUser = new ServiceAccountUser("testacc02", "testacc01", "read");
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response ldapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testacc01\"}",token)).thenReturn(userResponse);
        ReflectionTestUtils.setField(serviceAccountsService,"vaultAuthMethod", "userpass");
        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureUserpassUser(eq("testacc01"),any(),eq(token))).thenReturn(ldapConfigureResponse);
        // System under test
    	String expectedResponse = "{\"errors\":[\"Successfully added user to the Service Account\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedResponse);
        ResponseEntity<String> responseEntity = serviceAccountsService.addUserToServiceAccount(token, serviceAccountUser, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    @Ignore
    public void test_addUserToServiceAccount_failure_notauthorized() {
		UserDetails userDetails = getMockUser(false);
    	String token = userDetails.getClientToken();
    	ServiceAccountUser serviceAccountUser = new ServiceAccountUser("testacc02", "testacc01", "read");
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response ldapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testacc01\"}",token)).thenReturn(userResponse);
        ReflectionTestUtils.setField(serviceAccountsService,"vaultAuthMethod", "userpass");
        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureUserpassUser(eq("testacc01"),any(),eq(token))).thenReturn(ldapConfigureResponse);
        // System under test
    	String expectedResponse = "{\"errors\":[\"Not authorized to perform\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedResponse);
        ResponseEntity<String> responseEntity = serviceAccountsService.addUserToServiceAccount(token, serviceAccountUser, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    @Ignore
    public void test_removeUserFromServiceAccount_ldap_success() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	ServiceAccountUser serviceAccountUser = new ServiceAccountUser("testacc02", "testacc01", "read");
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\", \"o_svcacct_testacc02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response ldapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testacc01\"}",token)).thenReturn(userResponse);
        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("o_svcacct_testacc02");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPUser(eq("testacc01"),any(),any(),eq(token))).thenReturn(ldapConfigureResponse);
        // System under test
    	String expectedResponse = "{\"message\":[\"Successfully removed user from the Service Account\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedResponse);
        ResponseEntity<String> responseEntity = serviceAccountsService.removeUserFromServiceAccount(token, serviceAccountUser, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    @Ignore
    public void test_removeUserFromServiceAccount_userpass_success() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	ServiceAccountUser serviceAccountUser = new ServiceAccountUser("testacc02", "testacc01", "read");
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\", \"o_svcacct_testacc02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response ldapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testacc01\"}",token)).thenReturn(userResponse);
        ReflectionTestUtils.setField(serviceAccountsService,"vaultAuthMethod", "userpass");
        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("o_svcacct_testacc02");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureUserpassUser(eq("testacc01"),any(),eq(token))).thenReturn(ldapConfigureResponse);
        // System under test
    	String expectedResponse = "{\"message\":[\"Successfully removed user from the Service Account\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedResponse);
        ResponseEntity<String> responseEntity = serviceAccountsService.removeUserFromServiceAccount(token, serviceAccountUser, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    @Ignore
    public void test_removeUserFromServiceAccount_failure_notauthorized() {
		UserDetails userDetails = getMockUser(false);
    	String token = userDetails.getClientToken();
    	ServiceAccountUser serviceAccountUser = new ServiceAccountUser("testacc02", "testacc01", "read");
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\", \"o_svcacct_testacc02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response ldapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testacc01\"}",token)).thenReturn(userResponse);
        ReflectionTestUtils.setField(serviceAccountsService,"vaultAuthMethod", "userpass");
        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("o_svcacct_testacc02");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureUserpassUser(eq("testacc01"),any(),eq(token))).thenReturn(ldapConfigureResponse);
        // System under test
    	String expectedResponse = "{\"errors\":[\"Not authorized to perform\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedResponse);
        ResponseEntity<String> responseEntity = serviceAccountsService.removeUserFromServiceAccount(token, serviceAccountUser, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_resetSvcAccPassword_success() {
		UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	String svcAccName = "testacc02";

        // System under test
    	String expectedOutput = "{\n" +
    			"  \"current_password\": \"?@09AZkLfqgzr0AS6r2DwTo4E5r/VNCqWWqAs2AlCGdnkinuq9OKXkeXW6D4oVGc\",\n" +
    			"  \"last_password\": null,\n" +
    			"  \"username\": \"testacc02\"\n" +
    			"}";
    	Response pwdResetResponse = getMockResponse(HttpStatus.OK, true, expectedOutput);
    	when(reqProcessor.process("/ad/serviceaccount/reset","{\"role_name\":\""+svcAccName+"\"}",token)).thenReturn(pwdResetResponse);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedOutput);
        ResponseEntity<String> responseEntity = serviceAccountsService.resetSvcAccPassword(token, svcAccName, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_resetSvcAccPassword_permission_denied() {
    	UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	String svcAccName = "testacc03";

    	// System under test
    	String expectedOutput = "{\n" +
    			"    \"errors\": [\n" +
    			"               \"1 error occurred:\\n\\t* permission denied\\n\\n\"\n" +
    			"             ]\n" +
    			"           }";
    	Response pwdResetResponse = getMockResponse(HttpStatus.FORBIDDEN, true, expectedOutput);
    	when(reqProcessor.process("/ad/serviceaccount/reset","{\"role_name\":\""+svcAccName+"\"}",token)).thenReturn(pwdResetResponse);
    	ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN).body(expectedOutput);
    	ResponseEntity<String> responseEntity = serviceAccountsService.resetSvcAccPassword(token, svcAccName, userDetails);
    	assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
    	assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_getOnboarderdServiceAccount_success() {
    	UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	String svcAccName = "testacc03";
    	OnboardedServiceAccountDetails onboardedServiceAccountDetails = new OnboardedServiceAccountDetails();
    	onboardedServiceAccountDetails.setLastVaultRotation("2018-05-24T17:14:38.677370855Z");
    	onboardedServiceAccountDetails.setName(svcAccName+"@aaa.bbb.ccc.com");
    	onboardedServiceAccountDetails.setPasswordLastSet("2018-05-24T17:14:38.6038495Z");
    	onboardedServiceAccountDetails.setTtl(100L);

        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put("service_account_name", svcAccName+"@aaa.bbb.ccc.com");
        requestMap.put("last_vault_rotation", "2018-05-24T17:14:38.677370855Z");
        requestMap.put("password_last_set", "2018-05-24T17:14:38.6038495Z");
        requestMap.put("ttl", new Integer("100"));

    	// getOnboarderdServiceAccountDetails

    	Response svcAccDtlsResponse = getMockResponse(HttpStatus.OK, true, getJSON(requestMap));
    	when(reqProcessor.process("/ad/serviceaccount/details","{\"role_name\":\""+svcAccName+"\"}",token)).thenReturn(svcAccDtlsResponse);

    	// System under test
    	String onboardedServiceAccountDetailsJSON  = getJSON(onboardedServiceAccountDetails);
    	when(JSONUtil.getJSON(Mockito.any(OnboardedServiceAccountDetails.class))).thenReturn(onboardedServiceAccountDetailsJSON);
    	ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(onboardedServiceAccountDetailsJSON);
    	ResponseEntity<String> responseEntity = serviceAccountsService.getOnboarderdServiceAccount(token, svcAccName, userDetails);
    	assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    	assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_getOnboarderdServiceAccount_notfound() {
    	UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	String svcAccName = "testacc03";

    	// getOnboarderdServiceAccountDetails

    	Response svcAccDtlsResponse = getMockResponse(HttpStatus.NOT_FOUND, true, getJSON(null));
    	when(reqProcessor.process("/ad/serviceaccount/details","{\"role_name\":\""+svcAccName+"\"}",token)).thenReturn(svcAccDtlsResponse);

    	// System under test
    	String expectedMsg = "{\"errors\":[\"Either Service Account is not onbaorderd or you don't have enough permission to read\"]}";
    	ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.NOT_FOUND).body(expectedMsg);
    	ResponseEntity<String> responseEntity = serviceAccountsService.getOnboarderdServiceAccount(token, svcAccName, userDetails);
    	assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    	assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_canAddOrRemoveUser_admin_canadd() {
    	UserDetails userDetails = getMockUser(true);
    	ServiceAccountUser serviceAccountUser = new ServiceAccountUser("testacc02", "testacc01", "read");
    	String action = "addUser";
    	boolean expected = true;
    	// System under test
    	boolean actual = serviceAccountsService.canAddOrRemoveUser(userDetails, serviceAccountUser, action);
    	assertEquals(expected, actual);
    }

    @Test
    public void test_canAddOrRemoveUser_nonadmin_canadd() {
    	UserDetails userDetails = getMockUser(false);
    	ServiceAccountUser serviceAccountUser = new ServiceAccountUser("testacc02", "testacc01", "read");
    	String action = "addUser";
    	boolean expected = false;
    	// System under test
    	boolean actual = serviceAccountsService.canAddOrRemoveUser(userDetails, serviceAccountUser, action);
    	assertEquals(expected, actual);
    }

    @Test
    public void test_getOnboardedServiceAccounts_admin_success() {
    	UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	// Bevavior setup
    	String expectedOutput = "{\n" +
    			"  \"keys\": [\n" +
    			"    \"testacc02\",\n" +
    			"    \"testacc03\",\n" +
    			"    \"testacc04\"\n" +
    			"  ]\n" +
    			"}";
    	Response onboardedSvsAccsResponse = getMockResponse(HttpStatus.OK, true, expectedOutput);
    	when(reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token)).thenReturn(onboardedSvsAccsResponse);

    	// System under test
    	ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedOutput);
    	ResponseEntity<String> responseEntity = serviceAccountsService.getOnboardedServiceAccounts(token, userDetails);
    	assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    	assertEquals(responseEntityExpected, responseEntity);

    }
    @Test
    public void test_getOnboardedServiceAccounts_admin_notfound() {
    	UserDetails userDetails = getMockUser(true);
    	String token = userDetails.getClientToken();
    	// Bevavior setup
    	String expectedOutput = "{\"keys\":[]}";
    	Response onboardedSvsAccsResponse = getMockResponse(HttpStatus.NOT_FOUND, true, expectedOutput);
    	when(reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token)).thenReturn(onboardedSvsAccsResponse);

    	// System under test
    	ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedOutput);
    	ResponseEntity<String> responseEntity = serviceAccountsService.getOnboardedServiceAccounts(token, userDetails);
    	assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    	assertEquals(responseEntityExpected, responseEntity);
    }
    @Test
    public void test_getOnboardedServiceAccounts_nonadmin_notfound() {
    	UserDetails userDetails = getMockUser(false);
    	String token = userDetails.getClientToken();
    	// Bevavior setup
    	String expectedOutput = "{\"errors\":[\"TO BE IMPLEMENTED for non admin user\"]}";
    	Response onboardedSvsAccsResponse = getMockResponse(HttpStatus.NOT_FOUND, true, expectedOutput);
    	when(reqProcessor.process("/ad/serviceaccount/onboardedlist","{}",token)).thenReturn(onboardedSvsAccsResponse);

    	// System under test
    	ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedOutput);
    	ResponseEntity<String> responseEntity = serviceAccountsService.getOnboardedServiceAccounts(token, userDetails);
    	assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    	assertEquals(responseEntityExpected, responseEntity);

    }

    @Test
    public void test_addGroupToServiceAccount_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        ServiceAccountGroup serviceAccountGroup = new ServiceAccountGroup("svc_vault_test7", "group1", "write");
        UserDetails userDetails = getMockUser(false);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully associated with Service Account\"]}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        when(ControllerUtil.areSvcaccGroupInputsValid(serviceAccountGroup)).thenReturn(true);
        String [] policies = {"o_svcacct_svc_vault_test7"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        Response groupResp = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"group1\"}",token)).thenReturn(groupResp);
        ObjectMapper objMapper = new ObjectMapper();
        String responseJson = groupResp.getResponse();
        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("w_shared_mysafe01");
            resList.add("w_shared_mysafe02");
            when(ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson)).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);

        ResponseEntity<String> responseEntity = serviceAccountsService.addGroupToServiceAccount(token, serviceAccountGroup, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addGroupToServiceAccount_metadata_failure() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        ServiceAccountGroup serviceAccountGroup = new ServiceAccountGroup("svc_vault_test7", "group1", "write");
        UserDetails userDetails = getMockUser(false);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Group configuration failed.Please try again\"]}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response response404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");

        when(ControllerUtil.areSvcaccGroupInputsValid(serviceAccountGroup)).thenReturn(true);
        String [] policies = {"o_svcacct_svc_vault_test7"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        Response groupResp = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"group1\"}",token)).thenReturn(groupResp);
        ObjectMapper objMapper = new ObjectMapper();
        String responseJson = groupResp.getResponse();
        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("w_shared_mysafe01");
            resList.add("w_shared_mysafe02");
            when(ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson)).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(response404);

        ResponseEntity<String> responseEntity = serviceAccountsService.addGroupToServiceAccount(token, serviceAccountGroup, userDetails);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addGroupToServiceAccount_failure() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        ServiceAccountGroup serviceAccountGroup = new ServiceAccountGroup("svc_vault_test7", "group1", "write");
        UserDetails userDetails = getMockUser(true);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to add group to the Service Account\"]}");
        Response response404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");

        when(ControllerUtil.areSvcaccGroupInputsValid(serviceAccountGroup)).thenReturn(true);
        Response groupResp = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"group1\"}",token)).thenReturn(groupResp);
        ObjectMapper objMapper = new ObjectMapper();
        String responseJson = groupResp.getResponse();
        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("w_shared_mysafe01");
            resList.add("w_shared_mysafe02");
            when(ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson)).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(response404);

        ResponseEntity<String> responseEntity = serviceAccountsService.addGroupToServiceAccount(token, serviceAccountGroup, userDetails);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addGroupToServiceAccount_failure_403() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        ServiceAccountGroup serviceAccountGroup = new ServiceAccountGroup("svc_vault_test7", "group1", "write");
        UserDetails userDetails = getMockUser(false);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add groups to this service account\"]}");
        Response response404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");

        when(ControllerUtil.areSvcaccGroupInputsValid(serviceAccountGroup)).thenReturn(true);
        String [] policies = {"w_svcacct_svc_vault_test7"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        Response groupResp = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"group1\"}",token)).thenReturn(groupResp);
        ObjectMapper objMapper = new ObjectMapper();
        String responseJson = groupResp.getResponse();
        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("w_shared_mysafe01");
            resList.add("w_shared_mysafe02");
            when(ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson)).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(response404);

        ResponseEntity<String> responseEntity = serviceAccountsService.addGroupToServiceAccount(token, serviceAccountGroup, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }


    @Test
    public void test_removeGroupFromServiceAccount_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        ServiceAccountGroup serviceAccountGroup = new ServiceAccountGroup("svc_vault_test7", "group1", "write");
        UserDetails userDetails = getMockUser(false);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully removed from Service Account\"]}");
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully removed from Service Account\"]}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        when(ControllerUtil.areSvcaccGroupInputsValid(serviceAccountGroup)).thenReturn(true);
        String [] policies = {"o_svcacct_svc_vault_test7"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        Response groupResp = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"group1\"}",token)).thenReturn(groupResp);
        ObjectMapper objMapper = new ObjectMapper();
        String responseJson = groupResp.getResponse();
        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("w_shared_mysafe01");
            resList.add("w_shared_mysafe02");
            when(ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson)).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);

        ResponseEntity<String> responseEntity = serviceAccountsService.removeGroupFromServiceAccount(token, serviceAccountGroup, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeGroupFromServiceAccount_metadata_failure() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        ServiceAccountGroup serviceAccountGroup = new ServiceAccountGroup("svc_vault_test7", "group1", "write");
        UserDetails userDetails = getMockUser(false);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Group configuration failed.Please try again\"]}");
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Group configuration failed.Please try again\"]}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response response404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");

        when(ControllerUtil.areSvcaccGroupInputsValid(serviceAccountGroup)).thenReturn(true);
        String [] policies = {"o_svcacct_svc_vault_test7"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        Response groupResp = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"group1\"}",token)).thenReturn(groupResp);
        ObjectMapper objMapper = new ObjectMapper();
        String responseJson = groupResp.getResponse();
        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("w_shared_mysafe01");
            resList.add("w_shared_mysafe02");
            when(ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson)).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(response404);

        ResponseEntity<String> responseEntity = serviceAccountsService.removeGroupFromServiceAccount(token, serviceAccountGroup, userDetails);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeGroupFromServiceAccount_failure() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        ServiceAccountGroup serviceAccountGroup = new ServiceAccountGroup("svc_vault_test7", "group1", "write");
        UserDetails userDetails = getMockUser(true);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to remove the group from the Service Account\"]}");
        Response response404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");

        when(ControllerUtil.areSvcaccGroupInputsValid(serviceAccountGroup)).thenReturn(true);
        Response groupResp = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"group1\"}",token)).thenReturn(groupResp);
        ObjectMapper objMapper = new ObjectMapper();
        String responseJson = groupResp.getResponse();
        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("w_shared_mysafe01");
            resList.add("w_shared_mysafe02");
            when(ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson)).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(response404);

        ResponseEntity<String> responseEntity = serviceAccountsService.removeGroupFromServiceAccount(token, serviceAccountGroup, userDetails);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeGroupFromServiceAccount_failure_403() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        ServiceAccountGroup serviceAccountGroup = new ServiceAccountGroup("svc_vault_test7", "group1", "write");
        UserDetails userDetails = getMockUser(false);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add groups to this service account\"]}");
        Response response404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");

        when(ControllerUtil.areSvcaccGroupInputsValid(serviceAccountGroup)).thenReturn(true);
        String [] policies = {"w_svcacct_svc_vault_test7"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        Response groupResp = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"group1\"}",token)).thenReturn(groupResp);
        ObjectMapper objMapper = new ObjectMapper();
        String responseJson = groupResp.getResponse();
        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("w_shared_mysafe01");
            resList.add("w_shared_mysafe02");
            when(ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson)).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(response404);

        ResponseEntity<String> responseEntity = serviceAccountsService.removeGroupFromServiceAccount(token, serviceAccountGroup, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_AssociateAppRole_succssfully() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle successfully associated with Service Account\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountApprole serviceAccountApprole = new ServiceAccountApprole("svc_vault_test7", "role1", "write");

        when(ControllerUtil.areSvcaccApproleInputsValid(serviceAccountApprole)).thenReturn(true);
        String [] policies = {"o_svcacct_svc_vault_test7"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"w_shared_mysafe01\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        when(ControllerUtil.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);


        ResponseEntity<String> responseEntityActual =  serviceAccountsService.associateApproletoSvcAcc(userDetails, token, serviceAccountApprole);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_AssociateAppRole_failure_400() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid value specified for access\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountApprole serviceAccountApprole = new ServiceAccountApprole("svc_vault_test7", "role1", "write");

        when(ControllerUtil.areSvcaccApproleInputsValid(serviceAccountApprole)).thenReturn(false);
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.associateApproletoSvcAcc(userDetails, token, serviceAccountApprole);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_AssociateAppRole_failure_400_masterApprole() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: no permission to associate this AppRole to any Service Account\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountApprole serviceAccountApprole = new ServiceAccountApprole("svc_vault_test7", "selfservicesupportrole", "write");

        when(ControllerUtil.areSvcaccApproleInputsValid(serviceAccountApprole)).thenReturn(true);
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.associateApproletoSvcAcc(userDetails, token, serviceAccountApprole);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_AssociateAppRole_failure() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to add Approle to the Service Account\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountApprole serviceAccountApprole = new ServiceAccountApprole("svc_vault_test7", "role1", "write");

        when(ControllerUtil.areSvcaccApproleInputsValid(serviceAccountApprole)).thenReturn(true);
        String [] policies = {"o_svcacct_svc_vault_test7"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"w_shared_mysafe01\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        when(ControllerUtil.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        ResponseEntity<String> responseEntityActual =  serviceAccountsService.associateApproletoSvcAcc(userDetails, token, serviceAccountApprole);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_AssociateAppRole_failure_403() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add Approle to this service account\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountApprole serviceAccountApprole = new ServiceAccountApprole("svc_vault_test7", "role1", "write");

        when(ControllerUtil.areSvcaccApproleInputsValid(serviceAccountApprole)).thenReturn(true);
        String [] policies = {"r_svcacct_svc_vault_test7"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);

        ResponseEntity<String> responseEntityActual =  serviceAccountsService.associateApproletoSvcAcc(userDetails, token, serviceAccountApprole);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_AssociateAppRole_metadata_failure() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Approle configuration failed.Please try again\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountApprole serviceAccountApprole = new ServiceAccountApprole("svc_vault_test7", "role1", "write");

        when(ControllerUtil.areSvcaccApproleInputsValid(serviceAccountApprole)).thenReturn(true);
        String [] policies = {"o_svcacct_svc_vault_test7"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"w_shared_mysafe01\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);


        ResponseEntity<String> responseEntityActual =  serviceAccountsService.associateApproletoSvcAcc(userDetails, token, serviceAccountApprole);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_AssociateAppRole_metadata_failure_revoke_failure() throws Exception {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Approle configuration failed.Contact Admin \"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        ServiceAccountApprole serviceAccountApprole = new ServiceAccountApprole("svc_vault_test7", "role1", "write");

        when(ControllerUtil.areSvcaccApproleInputsValid(serviceAccountApprole)).thenReturn(true);
        String [] policies = {"o_svcacct_svc_vault_test7"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername())).thenReturn(policies);
        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"w_shared_mysafe01\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        Response configureAppRoleResponse_404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        when(ControllerUtil.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);

        when(ControllerUtil.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                if (count++ == 1)
                    return configureAppRoleResponse_404;

                return configureAppRoleResponse;
            }
        });
        Response updateMetadataResponse = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);


        ResponseEntity<String> responseEntityActual =  serviceAccountsService.associateApproletoSvcAcc(userDetails, token, serviceAccountApprole);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }
}
