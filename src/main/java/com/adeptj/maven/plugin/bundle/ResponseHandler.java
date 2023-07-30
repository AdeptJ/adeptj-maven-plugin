package com.adeptj.maven.plugin.bundle;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

public class ResponseHandler implements HttpClientResponseHandler<ClientResponse> {

    @Override
    public ClientResponse handleResponse(ClassicHttpResponse response) {
        return new ClientResponse(response);
    }
}
