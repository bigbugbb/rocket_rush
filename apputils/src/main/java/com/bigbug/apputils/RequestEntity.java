package com.bigbug.apputils;

import java.io.Serializable;

/**
 * Created by bigbug on 5/1/14.
 */
public class RequestEntity implements Serializable {

    // HTTP/HTTPS request url
    public String mURL;

    // Attached json to upload
    public String mJSON;

    // Response callbacks
    public ResponseCallbacks mCallbacks;

    public RequestEntity(String url, String json, ResponseCallbacks callbacks) {
        mURL = url;
        mJSON = json;
        mCallbacks = callbacks;
    }
}
