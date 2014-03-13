package com.localytics.android;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.localytics.android.LocalyticsProvider.AttributesDbColumns;
import com.localytics.android.LocalyticsProvider.IdentifiersDbColumns;

/**
 * This class implements the JavaScriptAPI interface.
 * <p>
 * This is not a public API.
 */
/* package */final class JavaScriptClient implements AmpDialogFragment.JavaScriptAPI 
{
	private Context mContext;
	
	private SessionHandler mHandler;
	
	private LocalyticsProvider mProvider;
	
	private DialogFragment mFragment;
	
	public JavaScriptClient(Context context, SessionHandler handler, LocalyticsProvider provider, DialogFragment fragment)
	{
		mContext  = context;
		mHandler  = handler;
		mProvider = provider;
		mFragment = fragment;
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
				"	   localytics.tagEventNative(event, JSON.stringify(attributes), JSON.stringify(customDimensions), customerValueIncrease);" +
				"    };" +
				"" +
				"    localytics.close = function() {" +
				"      localytics.closeNative();" +												
				"    };\';" +
				"  document.getElementsByTagName('body')[0].appendChild(localyticsScript);" +
				"})()", getIdentifiers(), getCustomDimensions(), getAttributes());
	}
	
	@JavascriptInterface
	public void tagEventNative(String event, String attributes, String customDimensions, long customerValueIncrease) 
	{
		Map<String, Object> nativeAttributes = null;
		List<String> nativeCustomDimensions = null;
		try 
    	{
			nativeAttributes = JsonHelper.toMap(new JSONObject(attributes));
			nativeCustomDimensions = JsonHelper.toList(new JSONArray(customDimensions));
		} 
    	catch (JSONException e) 
    	{
    		if (Constants.IS_LOGGABLE)
            {
                Log.w(Constants.LOG_TAG, "[JavaScriptClient]: Failed to parse the json object in tagEventNative"); //$NON-NLS-1$				                    
            }
    		return;
		}
		
		if (Constants.IS_PARAMETER_CHECKING_ENABLED)
        {
            if (null == event)
            {
                throw new IllegalArgumentException("event cannot be null"); //$NON-NLS-1$
            }

            if (0 == event.length())
            {
                throw new IllegalArgumentException("event cannot be empty"); //$NON-NLS-1$
            }

            if (null != attributes)
            {
                /*
                 * Calling this with empty attributes is a smell that indicates a possible programming error on the part of the
                 * caller
                 */
                if (nativeAttributes.isEmpty())
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.w(Constants.LOG_TAG, "attributes is empty.  Did the caller make an error?"); //$NON-NLS-1$
                    }
                }

                if (nativeAttributes.size() > Constants.MAX_NUM_ATTRIBUTES)
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.w(Constants.LOG_TAG, String.format("attributes size is %d, exceeding the maximum size of %d.  Did the caller make an error?", Integer.valueOf(nativeAttributes.size()), Integer.valueOf(Constants.MAX_NUM_ATTRIBUTES))); //$NON-NLS-1$
                    }
                }

                for (final Entry<String, Object> entry : nativeAttributes.entrySet())
                {
                    final String key = entry.getKey();
                    final String value = (String) entry.getValue();

                    if (null == key)
                    {
                        throw new IllegalArgumentException("attributes cannot contain null keys"); //$NON-NLS-1$
                    }
                    if (null == value)
                    {
                        throw new IllegalArgumentException("attributes cannot contain null values"); //$NON-NLS-1$
                    }
                    if (0 == key.length())
                    {
                        throw new IllegalArgumentException("attributes cannot contain empty keys"); //$NON-NLS-1$
                    }
                    if (0 == value.length())
                    {
                        throw new IllegalArgumentException("attributes cannot contain empty values"); //$NON-NLS-1$
                    }
                }
            }

            if (null != nativeCustomDimensions)
            {
                /*
                 * Calling this with empty dimensions is a smell that indicates a possible programming error on the part of the
                 * caller
                 */
                if (nativeCustomDimensions.isEmpty())
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.w(Constants.LOG_TAG, "customDimensions is empty.  Did the caller make an error?"); //$NON-NLS-1$
                    }
                }

                if (nativeCustomDimensions.size() > Constants.MAX_CUSTOM_DIMENSIONS)
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.w(Constants.LOG_TAG, String.format("customDimensions size is %d, exceeding the maximum size of %d.  Did the caller make an error?", Integer.valueOf(nativeCustomDimensions.size()), Integer.valueOf(Constants.MAX_CUSTOM_DIMENSIONS))); //$NON-NLS-1$
                    }
                }

                for (final Object element : nativeCustomDimensions)
                {
                    if (null == element)
                    {
                        throw new IllegalArgumentException("customDimensions cannot contain null elements"); //$NON-NLS-1$
                    }
                    if (0 == ((String) element).length())
                    {
                        throw new IllegalArgumentException("customDimensions cannot contain empty elements"); //$NON-NLS-1$
                    }
                }
            }
        }

        final String eventString = String.format(Constants.EVENT_FORMAT, mContext.getPackageName(), event);

        if (null == nativeAttributes && null == nativeCustomDimensions)
        {
            mHandler.sendMessage(mHandler.obtainMessage(SessionHandler.MESSAGE_TAG_EVENT, new Object[] { eventString, null, customerValueIncrease }));
        }
        else
        {
            /*
             * Convert the attributes and custom dimensions into the internal representation of packagename:key
             */

            final TreeMap<String, String> remappedAttributes = new TreeMap<String, String>();

            if (null != attributes)
            {
                final String packageName = mContext.getPackageName();
                for (final Entry<String, Object> entry : nativeAttributes.entrySet())
                {
                    remappedAttributes.put(String.format(AttributesDbColumns.ATTRIBUTE_FORMAT, packageName, entry.getKey()), (String) entry.getValue());
                }
            }

            if (null != customDimensions)
            {
                remappedAttributes.putAll(convertDimensionsToAttributes(nativeCustomDimensions));
            }

            /*
             * Copying the map is very important to ensure that a client can't modify the map after this method is called. This is
             * especially important because the map is subsequently processed on a background thread.
             *
             * A TreeMap is used to ensure that the order that the attributes are written is deterministic. For example, if the
             * maximum number of attributes is exceeded the entries that occur later alphabetically will be skipped consistently.
             */

            mHandler.sendMessage(mHandler.obtainMessage(SessionHandler.MESSAGE_TAG_EVENT, new Object[] { eventString, new TreeMap<String, String>(remappedAttributes), customerValueIncrease }));
        }        
	}
	
	@JavascriptInterface
	public void closeNative() 
	{
		new Handler(Looper.getMainLooper()).post(new Runnable() 
		{
			public void run() 
			{
				mFragment.dismiss();
			}
		});
	}						

	private String getIdentifiers()
    {
    	Cursor cursor = null;
        try
        {
        	cursor = mProvider.query(IdentifiersDbColumns.TABLE_NAME, null, null, null, null);
        	
            if (cursor.getCount() == 0)
            {
                return null;
            }

            final JSONObject identifiers = new JSONObject();

            final int keyColumn = cursor.getColumnIndexOrThrow(IdentifiersDbColumns.KEY);
            final int valueColumn = cursor.getColumnIndexOrThrow(IdentifiersDbColumns.VALUE);
            while (cursor.moveToNext())
            {
                final String key = cursor.getString(keyColumn);
                final String value = cursor.getString(valueColumn);

                identifiers.put(key.substring(mContext.getPackageName().length() + 1, key.length()), value);
            }

            return identifiers.toString();
        } 
        catch (JSONException e) 
        {
        	if (Constants.IS_LOGGABLE)
            {
                Log.w(Constants.LOG_TAG, "[JavaScriptClient]: Failed to get identifiers"); //$NON-NLS-1$				                    
            }
        	return null;
		}
        finally
        {
            if (null != cursor)
            {
                cursor.close();
                cursor = null;
            }
        }
    }
    
    private String getCustomDimensions()
    {
    	return null;
    }
    
    private String getAttributes()
    {
    	return null;
    }
    
    /**
     * Helper to convert a list of dimensions into a set of attributes.
     * <p>
     * The number of dimensions is capped at 4. If there are more than 4 elements in {@code customDimensions}, all elements after
     * 4 are ignored.
     *
     * @param customDimensions List of dimensions to convert.
     * @return Attributes map for the set of dimensions.
     */
    private static Map<String, String> convertDimensionsToAttributes(final List<String> customDimensions)
    {
        final TreeMap<String, String> attributes = new TreeMap<String, String>();

        if (null != customDimensions)
        {
            int index = 0;
            for (final String element : customDimensions)
            {
                if (0 == index)
                {
                    attributes.put(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_1, element);
                }
                else if (1 == index)
                {
                    attributes.put(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_2, element);
                }
                else if (2 == index)
                {
                    attributes.put(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_3, element);
                }
                else if (3 == index)
                {
                    attributes.put(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_4, element);
                }
                else if (4 == index)
                {
                    attributes.put(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_5, element);
                }
                else if (5 == index)
                {
                    attributes.put(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_6, element);
                }
                else if (6 == index)
                {
                    attributes.put(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_7, element);
                }
                else if (7 == index)
                {
                    attributes.put(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_8, element);
                }
                else if (8 == index)
                {
                    attributes.put(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_9, element);
                }
                else if (9 == index)
                {
                    attributes.put(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_10, element);
                }

                index++;
            }
        }

        return attributes;
    }
}
