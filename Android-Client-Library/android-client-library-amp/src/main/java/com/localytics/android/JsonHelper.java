package com.localytics.android;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The helper class to convert between java object and JSON object.
 */
/* package */final class JsonHelper
{
    @SuppressWarnings("rawtypes")
	public static Object toJSON(Object object) throws JSONException 
    {
        if (object instanceof Map) 
        {
            JSONObject json = new JSONObject();
            Map map = (Map) object;
            for (Object key : map.keySet()) 
            {
                json.put(key.toString(), toJSON(map.get(key)));
            }
            return json;
        }  
        else if (object instanceof Iterable) 
        {
            JSONArray json = new JSONArray();
            for (Object value : ((Iterable) object)) 
            {
                json.put(value);
            }
            return json;
        } 
        else 
        {
            return object;
        }
    }

    public static boolean isEmptyObject(JSONObject object) 
    {
        return object.names() == null;
    }

    public static Map<String, Object> getMap(JSONObject object, String key) throws JSONException 
    {
        return toMap(object.getJSONObject(key));
    }

    public static Map<String, Object> toMap(JSONObject object) throws JSONException 
    {
        @SuppressWarnings({ "rawtypes", "unchecked" })
		Map<String, Object> map = new HashMap();
        @SuppressWarnings("rawtypes")
		Iterator keys = object.keys();
        while (keys.hasNext()) 
        {
            String key = (String) keys.next();
            map.put(key, fromJson(object.get(key)));
        }
        return map;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
	public static List toList(JSONArray array) throws JSONException 
    {
		List list = new ArrayList();
        for (int i = 0; i < array.length(); i++) 
        {
            list.add(fromJson(array.get(i)));
        }
        return list;
    }

    public static Object fromJson(Object json) throws JSONException
    {
        if (json == JSONObject.NULL) 
        {
            return null;
        } 
        else if (json instanceof JSONObject) 
        {
            return toMap((JSONObject) json);
        } 
        else if (json instanceof JSONArray) 
        {
            return toList((JSONArray) json);
        } 
        else 
        {
            return json;
        }
    }

    public static String getSafeStringFromValue(Object value)
    {
        String stringValue = null;

        if (null == value)
        {
            return null;
        }
        else if (value instanceof Integer)
        {
            stringValue = Integer.toString((Integer) value);
        }
        else if (value instanceof String)
        {
            stringValue = (String) value;
        }

        return stringValue;
    }

    public static int getSafeIntegerFromMap(Map<String, Object> map, String key)
    {
        int integerValue = 0;
        Object value = map.get(key);

        if (null == value)
        {
            return 0;
        }
        else if (value instanceof Integer )
        {
            integerValue = (Integer) value;
        }
        else if (value instanceof String)
        {
            integerValue = Integer.parseInt((String) value);
        }

        return integerValue;
    }

    public static String getSafeStringFromMap(Map<String, Object> map, String key)
    {
        String stringValue = null;
        Object value = map.get(key);

        if (null == value)
        {
            return null;
        }
        else if (value instanceof Integer)
        {
            stringValue = Integer.toString((Integer) value);
        }
        else if (value instanceof String)
        {
            stringValue = (String) value;
        }

        return stringValue;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getSafeMapFromMap(Map<String, Object> map, String key)
    {
        Map<String, Object> mapValue = null;
        Object value = map.get(key);

        if (null == value)
        {
            return null;
        }
        else if (value instanceof Map)
        {
            mapValue = (Map<String, Object>) value;
        }

        return mapValue;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> getSafeListFromMap(Map<String, Object> map, String key)
    {
        List<Object> listValue = null;
        Object value = map.get(key);

        if (null == value)
        {
            return null;
        }
        else if (value instanceof List)
        {
            listValue = (List<Object>) value;
        }

        return listValue;
    }
}