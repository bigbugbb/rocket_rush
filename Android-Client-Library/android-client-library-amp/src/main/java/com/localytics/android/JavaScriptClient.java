package com.localytics.android;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;

import java.util.Map;

/**
 * This class implements the JavaScriptAPI interface.
 * <p>
 * This is not a public API.
 */
/* package */final class JavaScriptClient
{
    /**
     * Callbacks which implement the corresponding methods.
     * The keys are defined in AmpCallable. Use callbacks to minimize the coupling.
     */
	private Map<Integer, AmpCallable> mCallbacks;
	
	public JavaScriptClient(Map<Integer, AmpCallable> callbacks)
	{
		mCallbacks = callbacks;
	}

    public Map<Integer, AmpCallable> getCallbacks()
    {
        return mCallbacks;
    }

	public String getJsGlueCode()
	{																												
		return String.format("javascript:(" +
	        	"function() {" + 
	        	"  var localyticsScript = document.createElement('script');" +
				"  localyticsScript.type = 'text/javascript';" +
				"  localyticsScript.text = \'" +					
				" 	 localytics.identifers = %s;" +
				"	 localytics.customDimensions = %s;" +
				"	 localytics.attributes = %s;" +
				"" +
				"    localytics.tagEvent = function(event, attributes, customDimensions, customerValueIncrease) {" +
				"	   localytics.nativeTagEvent(event, JSON.stringify(attributes), JSON.stringify(customDimensions), customerValueIncrease);" +
				"    };" +
				"" +
				"    localytics.close = function() {" +
				"      localytics.nativeClose();" +
				"    };\';" +
				"  document.getElementsByTagName('body')[0].appendChild(localyticsScript);" +
				"})()", getIdentifiers(), getCustomDimensions(), getAttributes());
	}
	
	@JavascriptInterface
	public void nativeTagEvent(String event, String attributes, String customDimensions, long customerValueIncrease)
	{
        if (Constants.IS_LOGGABLE)
        {
            Log.w(Constants.LOG_TAG, "[JavaScriptClient]: nativeTagEvent is being called"); //$NON-NLS-1$
        }

        invoke(AmpCallable.ON_AMP_JS_TAG_EVENT, new Object[]{event, attributes, customDimensions, customerValueIncrease});
	}
	
	@JavascriptInterface
	public void nativeClose()
	{
        if (Constants.IS_LOGGABLE)
        {
            Log.w(Constants.LOG_TAG, "[JavaScriptClient]: nativeClose is being called"); //$NON-NLS-1$
        }

		new Handler(Looper.getMainLooper()).post(new Runnable() 
		{
			public void run() 
			{
                invoke(AmpCallable.ON_AMP_JS_CLOSE_WINDOW, null);
			}
		});
	}						

	public String getIdentifiers()
    {
        String identifiers = (String) invoke(AmpCallable.ON_AMP_JS_GET_IDENTIFIERS, null);
        return identifiers;
    }
    
    public String getCustomDimensions()
    {
        String customDimensions = (String) invoke(AmpCallable.ON_AMP_JS_GET_CUSTOM_DIMENSIONS, null);
    	return customDimensions;
    }
    
    public String getAttributes()
    {
        String customDimensions = (String) invoke(AmpCallable.ON_AMP_JS_GET_ATTRIBUTES, null);
        return customDimensions;
    }

    private Object invoke(int methodId, final Object[] params)
    {
        Object result = null;

        if (mCallbacks != null)
        {
            AmpCallable callable = mCallbacks.get(methodId);
            if (callable != null)
            {
                result = callable.call(params);
            }
        }

        return result;
    }
}
