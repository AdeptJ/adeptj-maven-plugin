package com.adeptj.maven.plugin.bundle;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;

import static org.apache.hc.core5.http.HttpStatus.SC_OK;

public class ClientResponse {

    private final int code;

    private final String reasonPhrase;

    private final Header[] headers;

    public ClientResponse(ClassicHttpResponse response) {
        this.code = response.getCode();
        this.reasonPhrase = response.getReasonPhrase();
        this.headers = response.getHeaders();
    }

    public int getCode() {
        return code;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public Header[] getHeaders() {
        return headers;
    }

    public boolean isOk() {
        return this.code == SC_OK;
    }
}
