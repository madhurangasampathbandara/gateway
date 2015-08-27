package org.wso2.carbon.mediation.camel;

/**
 * Contains the URL Parameters
 */
public class CamelEndPointURL {
    private String host;
    private int port;
    private String uri;

    public CamelEndPointURL(String host, int port, String uri) {
        this.host = host;
        this.port = port;
        this.uri = uri;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUri() {
        return uri;
    }
}