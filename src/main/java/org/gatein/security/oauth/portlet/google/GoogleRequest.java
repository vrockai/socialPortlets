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
