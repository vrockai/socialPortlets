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

<div class="socialUserInfoPortlet">
    <h3 class="socialUserInfoHeader">User</h3>
    <img class="socialUserInfoUserImage" src="${googleUserInfo.picture}?size=100" title="${googleUserInfo.name}" />
    <div class="socialUserInfo">
        <div class="socialUserInfoProperty"><span class="socialUserInfoPropertyKey">Given name:</span><span class="socialUserInfoPropertyValue">${googleUserInfo.givenName}</span></div>
        <div class="socialUserInfoProperty"><span class="socialUserInfoPropertyKey">Family name:</span><span class="socialUserInfoPropertyValue">${googleUserInfo.familyName}</span></div>
        <div class="socialUserInfoProperty"><span class="socialUserInfoPropertyKey">Email:</span><span class="socialUserInfoPropertyValue">${googleUserInfo.email}</span></div>
        <div class="socialUserInfoProperty"><span class="socialUserInfoPropertyKey">Birthday:</span><span class="socialUserInfoPropertyValue">${googleUserInfo.birthday}</span></div>
        <div class="socialUserInfoProperty"><span class="socialUserInfoPropertyKey">Gender:</span><span class="socialUserInfoPropertyValue">${googleUserInfo.gender}</span></div>
        <div class="socialUserInfoProperty"><span class="socialUserInfoPropertyKey">Locale:</span><span class="socialUserInfoPropertyValue">${googleUserInfo.locale}</span></div>
    </div>
</div>