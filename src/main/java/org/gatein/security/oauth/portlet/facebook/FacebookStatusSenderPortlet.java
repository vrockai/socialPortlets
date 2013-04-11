/*
 * JBoss, a division of Red Hat
 * Copyright 2013, Red Hat Middleware, LLC, and individual
 * contributors as indicated by the @authors tag. See the
 * copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */


package org.gatein.security.oauth.portlet.facebook;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletSession;
import javax.portlet.PortletURL;
import javax.portlet.ProcessAction;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.exoplatform.container.ExoContainer;
import org.gatein.security.oauth.common.OAuthConstants;
import org.gatein.security.oauth.common.OAuthProviderType;
import org.gatein.security.oauth.facebook.FacebookAccessTokenContext;
import org.gatein.security.oauth.facebook.GateInFacebookProcessor;
import org.gatein.security.oauth.portlet.AbstractSocialPortlet;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FacebookStatusSenderPortlet extends AbstractSocialPortlet<FacebookAccessTokenContext> {

    private GateInFacebookProcessor gtnFacebookProcessor;
    private static final String ACTION_SEND_STATUS = "_sendStatus";

    @Override
    protected void afterInit(ExoContainer container) {
        this.gtnFacebookProcessor = (GateInFacebookProcessor)container.getComponentInstanceOfType(GateInFacebookProcessor.class);
    }

    @Override
    protected OAuthProviderType<FacebookAccessTokenContext> getOAuthProvider() {
        return getOauthProviderTypeRegistry().getOAuthProvider(OAuthConstants.OAUTH_PROVIDER_KEY_FACEBOOK);
    }


    @Override
    protected void handleRender(RenderRequest request, RenderResponse response, FacebookAccessTokenContext accessToken) throws IOException {
        PrintWriter out = response.getWriter();
        PortletURL url = response.createActionURL();
        url.setParameter(ActionRequest.ACTION_NAME, ACTION_SEND_STATUS);

        out.println("<h3>Publish some content to your facebook wall</h3>");
        out.println("<div style=\"font-size: 13px;\">Either message or link are required fields</div><br>");
        out.println("<form method=\"POST\" action=\"" + url + "\">");
        out.println("<table>");
        out.println(renderInput("message", true, request));
        out.println("<tr><td></td><td></td></tr>");
        out.println("<tr><td colspan=2><div style=\"font-size: 13px;\">Other parameters, which are important only if you want to publish some link</div></td></tr>");
        out.println(renderInput("link", true, request));
        out.println(renderInput("picture", false, request));
        out.println(renderInput("name", false, request));
        out.println(renderInput("caption", false, request));
        out.println(renderInput("description", false, request));
        out.println("</table>");
        out.println("<input type=\"submit\" value=\"submit\" />");
        out.println("</form>");
    }


    @ProcessAction(name = ACTION_SEND_STATUS)
    public void actionSendStatus(ActionRequest aReq, ActionResponse aResp) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace("Method actionSendStatus triggered");
        }

        String message = getParameterAndSaveItToSession("message", aReq, aResp);
        String link = getParameterAndSaveItToSession("link", aReq, aResp);
        String picture = getParameterAndSaveItToSession("picture", aReq, aResp);
        String name = getParameterAndSaveItToSession("name", aReq, aResp);
        String caption = getParameterAndSaveItToSession("caption", aReq, aResp);
        String description = getParameterAndSaveItToSession("description", aReq, aResp);

//        if (isEmpty(message) && isEmpty(link)) {
//            out.println("Either message or link needs to be specified!<br>");
//            out.println("<a href=\"" + req.getRequestURI() + "\">Back</a><br>");
//            return;
//        }

        if (log.isTraceEnabled()) {
            StringBuilder builder = new StringBuilder("message=" + message)
                    .append(", link=" + link)
                    .append(", picture=" + picture)
                    .append(", name=" + name)
                    .append(", caption=" + caption)
                    .append(", description=" + description);
            log.trace(builder.toString());
        }

//        FacebookClient facebookClient = new DefaultFacebookClient(MY_ACCESS_TOKEN1);
//        List<Parameter> params = new ArrayList<Parameter>();
//        appendParam(params, "message", message);
//        appendParam(params, "link", link);
//        appendParam(params, "picture", picture);
//        appendParam(params, "name", name);
//        appendParam(params, "caption", capture);
//        appendParam(params, "description", description);
//
//        try {
//            FacebookType publishMessageResponse = facebookClient.publish("me/feed", FacebookType.class, params.toArray(new Parameter[] {}));
//            if (publishMessageResponse.getId() != null) {
//                System.out.println("Message published successfully to Facebook profile of user " + req.getRemoteUser() + " with ID " + publishMessageResponse.getId());
//                out.println("Message published successfully to your Facebook profile!");
//            }
//        } catch (FacebookOAuthException foe) {
//            String exMessage = "Error occured: " + foe.getErrorCode() + " - " + foe.getErrorType() + " - " + foe.getErrorMessage();
//            System.out.println(exMessage);
//            out.println(exMessage + "<br>");
//            if (foe.getErrorMessage().contains("URL is not properly formatted")) {
//                // do nothing special
//            } else if (foe.getErrorMessage().contains("The user hasn't authorized the application to perform this action")) {
//                out.println("You need at least privileges of scope: publish_stream<br>");
//                System.out.println("You need at least privileges of scope: publish_stream");
//            } else {
//                foe.printStackTrace();
//            }
//        }

        if (log.isTraceEnabled()) {
            log.trace("Method actionSendStatus finished");
        }
    }


    private String renderInput(String inputName, boolean required, RenderRequest request) {
        String label = inputName.substring(0, 1).toUpperCase() + inputName.substring(1);
        StringBuilder result = new StringBuilder("<tr><td>" + label + ": </td><td><input name=\"").
                append(inputName + "\"");

        // Try to read value from renderRequest
        String value = request.getParameter(inputName);
        if (value != null) {
            result.append(" value=\"" + value + "\"");
        }

        result.append(" />");
        if (required) {
            result = result.append(" *");
        }
        return result.append("</td></tr>").toString();
    }

    private boolean isEmpty(String message) {
        return message == null || message.length() == 0;
    }

    private String getParameterAndSaveItToSession(String paramName, ActionRequest req, ActionResponse resp) {
        String paramValue = req.getParameter(paramName);
        if (paramValue != null) {
            resp.setRenderParameter(paramName, paramValue);
        }
        return paramValue;
    }
}
