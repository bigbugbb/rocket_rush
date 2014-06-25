package com.bigbug.apputils;

import java.io.IOException;

/**
 * Created by jefflopes on 5/2/14.
 */
public abstract class ResponseCallbacks<T> {

    /**
     * Subclasses should override this to implement a handler method to process
     * the results. If not overridden, the result will be discarded.
     *
     * @param response The result value (usually, a json string).
     */
    public abstract void onComplete(String response);

    /**
     * Subclasses may override this to implement an exception handler. If not
     * overridden, the exception will be ignored.
     *
     * @param exception {@link IOException} that would be thrown on the request
     *            call.
     */
    public void onError(IOException exception) {}
}
