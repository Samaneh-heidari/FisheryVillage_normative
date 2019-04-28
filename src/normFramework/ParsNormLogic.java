package normFramework;

import org.apache.commons.lang3.math.NumberUtils;

import fisheryvillage.common.Constants;
import fisheryvillage.common.Logger;
import valueframework.common.Log;


public class ParsNormLogic {
	public static String getDonationOperation(String normTitle){
		int indexOfOperator = -1;
		for(int i = Constants.VALID_OPERATORS.length-1; i >= 0; i--)
        {
			indexOfOperator = normTitle.indexOf(Constants.VALID_OPERATORS[i]);
			if(indexOfOperator >= 0)
				break;
        }
		
//		normTitle = normTitle.replaceAll(" ", "");
//		System.out.println("mynorm " + normTitle + " operator index is " + indexOfOperator + ", operator is " + normTitle.substring(indexOfOperator, indexOfOperator+1));
		return normTitle.substring(indexOfOperator, indexOfOperator+1);
	}
	
	public static boolean isNumeric(String str)
	{
		try
	    {
	      double d = Double.parseDouble(str);
	    }
	    catch(NumberFormatException nfe)
	    {
	      return false;
	    }
	    return true;
	}
	
	//it will return [min, max]
	public static double[] getDonationAmount(String normTitle){
		String operator = getDonationOperation(normTitle);
		String[] normTtl = normTitle.split(operator);
		Logger.logDebug("Parsing " + normTitle + ", operator is " + operator + " and after split size is " + normTtl.length);
		double[] minMaxDonation = new double[2];
		for(int idx = 0; idx < minMaxDonation.length; idx++){
			minMaxDonation[idx] = -1.0;
		}
//		if(normTtl.length > 2 | normTtl.length == 1)
//			Log.printError("trying to parse a norm. But it has too much elements " + normTitle);
		Logger.logDebug("ParsNorm "+ normTtl.length);
		if(normTtl.length == 3){//it has min and max
			return twoConditionsArithmeticOperation(normTtl, operator);
		}else if (normTtl.length == 2) //it only has min or max
			return singleConditionArithmeticOperation(normTtl, operator);
		else 
			Log.printError("trying to parse a norm. but the format is not correct! " + normTitle);
		Logger.logDebug(" min " + minMaxDonation[0] + " , max " + minMaxDonation[1]);
		return minMaxDonation;
	}

	private static double[] twoConditionsArithmeticOperation(
			String[] normTtl, String operator) {
		double[] minMaxDonation = new double[2];
		if(isNumeric(normTtl[0]) && isNumeric(normTtl[2])){
			if(operator.equals(">")){
				minMaxDonation[0] = new Double(normTtl[2]);
				minMaxDonation[1] = new Double(normTtl[0]);
			}else if (operator.equals("<")){
				minMaxDonation[0] = new Double(normTtl[0]);
				minMaxDonation[1] = new Double(normTtl[2]);
			}
			else if (operator.equals("=")){
				minMaxDonation[0] = new Double(normTtl[0]);
				minMaxDonation[1] = new Double(normTtl[0]);
			}
			else{
				Log.printError("trying to parse a norm. but the operator is not known! " + operator);
				minMaxDonation[0] = -1;
				minMaxDonation[1] = -1;
			}
		}
		
		return minMaxDonation;
	}


	
	private static double[] singleConditionArithmeticOperation(String[] normTtl,
			String operator) {
		double[] minMaxDonation = new double[2];
		Logger.logDebug("singleConditionArithmeticOperation " + normTtl[0] + ", " + normTtl[1] + ", op " + operator );
		
		int numberIndex = -1;
		if(isNumeric(normTtl[0]))
			numberIndex = 0;
		else if(isNumeric(normTtl[1]))
			numberIndex = 1;
		Logger.logDebug("index of number " + numberIndex);
		
		
		if(numberIndex >=0){
			if((operator.equals(">") && numberIndex ==0)||(operator.equals("<") && numberIndex == 1)){
				minMaxDonation[0] = -1;
				minMaxDonation[1] = new Double(normTtl[numberIndex]);
			}else if ((operator.equals("<") && numberIndex == 0)||(operator.equals(">") && numberIndex == 1)){
				minMaxDonation[0] = new Double(normTtl[numberIndex]);
				minMaxDonation[1] = -1;
			}
			else if (operator.equals("=")){
				minMaxDonation[0] = new Double(normTtl[numberIndex]);
				minMaxDonation[1] = new Double(normTtl[numberIndex]);
			}
			else{
				Log.printError("trying to parse a norm. but the oprand is not known! " + normTtl[numberIndex]);
				minMaxDonation[0] = -1;
				minMaxDonation[1] = -1;
			}
		}
		else if(isNumeric(normTtl[1])){
			Logger.logDebug("singleConditionArithmeticOperation " + normTtl[1] + ", op " + operator );
			if(operator.equals("<")){
				minMaxDonation[0] = -1;
				minMaxDonation[1] = new Double(normTtl[1]);
			}else if (operator.equals(">")){
				minMaxDonation[0] = new Double(normTtl[1]);
				minMaxDonation[1] = -1;
			}
			else if (operator.equals("=")){
				minMaxDonation[0] = new Double(normTtl[1]);
				minMaxDonation[1] = new Double(normTtl[1]);
			}
			else{
				Log.printError("trying to parse a norm. but the operator is not known! " + operator);
				minMaxDonation[0] = -1;
				minMaxDonation[1] = -1;
			}
		}
		return minMaxDonation;
	}
}
