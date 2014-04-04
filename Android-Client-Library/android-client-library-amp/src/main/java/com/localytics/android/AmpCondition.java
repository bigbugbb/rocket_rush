package com.localytics.android;

import android.util.Log;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Vector;

/**
 * The helper class to deal with amp condition checking.
 */
/* package */final class AmpCondition
{    
	public enum Opt 
	{
		INVALID,
	    EQUAL,
	    NOT_EQUAL,
	    GREATER_THAN,
	    GREATER_THEN_OR_EQUAL,
	    LESS_THAN,
	    LESS_THAN_OR_EQUAL,
	    BETWEEN,
	    IN_LIST
	}
	
	/**
     * The name of the event
     */
	private String mName;
	
	/**
	 * The Opt type name
	 */
	private Opt mOpt;

    /**
     * The package name
     */
    private String mPkgName;
	
	/**
	 * The values associated with this event
	 */
	private Vector<String> mValues;    	    	
	
	public AmpCondition(final String name, final String operator, final Vector<String> values)
	{
		mName   = name;
		mOpt    = stringToOperator(operator);
		mValues = values;
	}

    public void setPackageName(final String pkgName)
    {
        mPkgName = pkgName;
    }

	/**
	 * This method checks whether this condition is satisfied by the input attributes
	 * 
	 * @param attributes The attributes to check
	 * @return true if this condition is satisfied by the attributes, otherwise false.
	 */
	public boolean isSatisfiedByAttributes(final Map<String, String> attributes)
	{    		    		    		
		if (null == attributes) 
		{
			return false;
		}
		
		// Get the attribute value from the event key
		Object attributeValue = attributes.get(mName);
        if (null == attributeValue)
        {
            attributeValue = attributes.get(mPkgName + ":" + mName);
        }
		
		// Check whether the attribute value does exist
		if (null == attributeValue)
		{
			if (Constants.IS_LOGGABLE)
            {        		
                Log.w(Constants.LOG_TAG, String.format("Could not find the AMP condition %s in the attributes dictionary.", mName)); //$NON-NLS-1$
            } 
			return false;
		}
		
		// Check whether the type of the attribute value is valid. 
		// Only string and numerical types are supported here for now.
		boolean satisfied = false;
		if (attributeValue instanceof String)
		{
			satisfied = isSatisfiedByString((String) attributeValue);
		}
		else if (attributeValue instanceof Number)
		{
			satisfied = isSatisfiedByNumber((String) attributeValue);
		}
		else
		{
			if (Constants.IS_LOGGABLE)
            {        		
                Log.w(Constants.LOG_TAG, String.format("Invalid value type %s in the attributes dictionary.", attributeValue.getClass().getCanonicalName())); //$NON-NLS-1$
            }   			
		}
		
		return satisfied;
	}
	
	private boolean isSatisfiedByString(final String attributeValue)
	{
		boolean satisfied = false;
		
		switch (mOpt)
		{
			case EQUAL:
				satisfied = attributeValue.equals(mValues.get(0));
				break;
			case NOT_EQUAL:
				satisfied = !attributeValue.equals(mValues.get(0));
				break;
			case IN_LIST:
				for (final String conditionValue : mValues)
				{
					if (attributeValue.equals(conditionValue))
					{
						satisfied = true;
						break;
					}
				}
				break;
			case GREATER_THAN:
			case GREATER_THEN_OR_EQUAL:
			case LESS_THAN:
			case LESS_THAN_OR_EQUAL:
			case BETWEEN:    			
			default:
				// The only other operations left don't make sense for strings.
				// Check to see if the string can be expressed as a number
				//
				satisfied = isSatisfiedByNumber(attributeValue);
		}  
		
		return satisfied;
	}
	
	private boolean isSatisfiedByNumber(final String attributeValue)
	{
		boolean satisfied = false;
		
		// Compare the value and check the results
		final BigDecimal attribute = new BigDecimal(attributeValue);    		    		
		final int result1 = attribute.compareTo(new BigDecimal(mValues.get(0))); 
		final int result2 = (mValues.size() > 1) ? attribute.compareTo(new BigDecimal(mValues.get(1))) : 0;
		
		switch (mOpt)
		{
			case EQUAL:
				satisfied = result1 == 0;
				break;
			case NOT_EQUAL:
				satisfied = result1 != 0;
				break;
			case GREATER_THAN:
				satisfied = result1 > 0;
				break;
			case GREATER_THEN_OR_EQUAL:
				satisfied = result1 >= 0;
				break;
			case LESS_THAN:
				satisfied = result1 < 0;
				break;
			case LESS_THAN_OR_EQUAL:
				satisfied = result1 <= 0;
				break;
			case BETWEEN:
				satisfied = result1 >= 0 && result2 <= 0;
				break;
			case IN_LIST:
				for (final String conditionValue : mValues)
				{
					if (attribute.compareTo(new BigDecimal(conditionValue)) == 0)
					{
						satisfied = true;
						break;
					}
				}
				break;
			default:
				;
		}  
		
		return satisfied;
	}
	
	private Opt stringToOperator(final String operator)
	{
		if (operator.equals("eq"))
		{
			return Opt.EQUAL;
		}
		
		if (operator.equals("neq"))
		{
			return Opt.NOT_EQUAL;
		}
		
		if (operator.equals("gt"))
		{
			return Opt.GREATER_THAN;
		}
		
		if (operator.equals("gte"))
		{
			return Opt.GREATER_THEN_OR_EQUAL;
		}
		
		if (operator.equals("lt"))
		{
			return Opt.LESS_THAN;
		}
		
		if (operator.equals("lte"))
		{
			return Opt.LESS_THAN_OR_EQUAL;
		}
		
		if (operator.equals("btw"))
		{
			return Opt.BETWEEN;
		}
		    		
		if (operator.equals("in"))
		{
			return Opt.IN_LIST;
		}
		
		return Opt.INVALID;
	}

	@SuppressWarnings("unused")
	private String operatorToString(final Opt opt)    	
	{
		switch (opt)
		{
			case EQUAL:
				return "is equal to";
			case NOT_EQUAL:
				return "not equal to";
			case GREATER_THAN:
				return "is greater than";
			case GREATER_THEN_OR_EQUAL:
				return "is greater than or equal to";
			case LESS_THAN:
				return "is less than";
			case LESS_THAN_OR_EQUAL:
				return "is less than or equal to";
			case BETWEEN:
				return "is in between values";
			case IN_LIST:
				return "is a member of the list";
			case INVALID:
			default:
				return "INVALID OPERATOR";
		}    		    		
	}
}
