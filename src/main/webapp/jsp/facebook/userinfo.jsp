<!--
    JBoss, Home of Professional Open Source
    Copyright 2012, Red Hat, Inc. and/or its affiliates, and individual
    contributors by the @authors tag. See the copyright.txt in the 
    distribution for a full listing of individual contributors.
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,  
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 -->
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ page import="java.util.Locale"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page trimDirectiveWhitespaces="true"%>
<portlet:defineObjects />

<div class="socialUserInfoPortlet socialPortlet">
    <h3 class="socialHeader facebookHeader">User</h3>
    <img class="socialUserInfoImage facebookUserImage" src="http://graph.facebook.com/${googleUserInfo.username}/picture" title="${googleUserInfo.name}" />
    <div class="socialUserInfo">
        <div class="socialUserProperty"><span class="socialUserPropertyKey">ID:</span><span class="socialUserPropertyValue">${googleUserInfo.id}</span></div>
        <div class="socialUserProperty"><span class="socialUserPropertyKey">Name:</span><span class="socialUserPropertyValue">${googleUserInfo.name}</span></div>
        <div class="socialUserProperty"><span class="socialUserPropertyKey">Username:</span><span class="socialUserPropertyValue">${googleUserInfo.username}</span></div>
        <div class="socialUserProperty"><span class="socialUserPropertyKey">First Name:</span><span class="socialUserPropertyValue">${googleUserInfo.firstName}</span></div>
        <div class="socialUserProperty"><span class="socialUserPropertyKey">Last Name:</span><span class="socialUserPropertyValue">${googleUserInfo.lastName}</span></div>
        <div class="socialUserProperty"><span class="socialUserPropertyKey">Gender:</span><span class="socialUserPropertyValue">${googleUserInfo.gender}</span></div>
        <div class="socialUserProperty"><span class="socialUserPropertyKey">Timezone:</span><span class="socialUserPropertyValue">${googleUserInfo.timezone}</span></div>
        <div class="socialUserProperty"><span class="socialUserPropertyKey">Locale:</span><span class="socialUserPropertyValue">${googleUserInfo.locale}</span></div>
        <div class="socialUserProperty"><span class="socialUserPropertyKey">E-mail:</span><span class="socialUserPropertyValue">${googleUserInfo.email}</span></div>
    </div>
</div>

