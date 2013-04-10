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


package org.gatein.security.oauth.portlet.google;

import java.io.IOException;
import java.io.PrintWriter;

import javax.portlet.RenderResponse;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
abstract class GoogleRequest<T> {

    private final RenderResponse response;
    private final String requiredScope;

    GoogleRequest(RenderResponse response, String requiredScope) {
        this.response = response;
        this.requiredScope = requiredScope;
    }


    abstract T run() throws IOException;


    T sendRequest() throws IOException {
        try {
            return run();
        } catch (GoogleJsonResponseException googleEx) {
            PrintWriter writer = response.getWriter();
            writer.println("Error occured. Your accessToken is invalid or scope is insufficient. You will need scope: " + requiredScope + "<br><br>");
            writer.println("Error details: " + googleEx.getDetails() + "<br><br>");
            writer.println("See server log for more info<br><br>");
            googleEx.printStackTrace();
            return null;
        } catch (IOException ioe) {
            PrintWriter writer = response.getWriter();
            writer.println("I/O error occured. Error details: " + ioe.getMessage() + "<br><br>");
            writer.println("See server log for more info<br><br>");
            ioe.printStackTrace();
            return null;
        }
    }

}
