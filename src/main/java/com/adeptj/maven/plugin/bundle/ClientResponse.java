package com.adeptj.maven.plugin.bundle;

public class ClientResponse {

    private final int code;

    private final String reasonPhrase;

    public ClientResponse(int code, String reasonPhrase) {
        this.code = code;
        this.reasonPhrase = reasonPhrase;
    }

    public int getCode() {
        return code;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }
}
