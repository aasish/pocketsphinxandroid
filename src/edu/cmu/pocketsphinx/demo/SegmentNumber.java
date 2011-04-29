package edu.cmu.pocketsphinx.demo;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;


public class SegmentNumber{
	public static String segmentNum(String wordNumber)
	{
		
		
		String[] arrayStr = wordNumber.split(" ");
		
		ArrayList<String> items = new ArrayList<String>();
		for (int i = 0; i < arrayStr.length; i++){items.add(arrayStr[i]);}
		String[] digit = {"one","two","three","four","five","six","seven","eight","nine"};
		List list_digit = Arrays.asList(digit);
		String[] special = {"zero","eleven","twelve","thirteen","fourteen","fifteen","sixteen","seventeen","eighteen","nineteen"};
		List list_special = Arrays.asList(special);
		String[] ten = {"twenty","thirty","forty","fifty","sixty","seventy","eighty","ninety"};
		List list_ten = Arrays.asList(ten);
		String[] hundred = {"one hundred","two hundred","three hundred","four hundred","five hundred","six hundred","seven hundred","eight hundred","nine hundred"};
		List list_hundred = Arrays.asList(hundred);
		String[] AND = {"and"};
		List list_and = Arrays.asList(AND);

		ArrayList<String> list = new ArrayList<String>();
		
		int prev_class = 0;
		for (int i = 0; i < items.size();i++){
			int present_class = 0;
			String add_element;
			//System.out.println(items.get(i));
			
			if (items.get(i).equals("hundred"))
			{
				//System.out.println("find hundred");
				add_element = list.get(list.size()-1);
				add_element = add_element + " hundred";
				//System.out.println(add_element);
				list.remove(list.size()-1);
				list.add(add_element);
			}
			else{
				list.add(items.get(i));
				}
		}
		String final_str = "";
		String i = null;
		int prev_is_and = 0;
		for(int j = 0; j < list.size();j++){
			int present_class = 999;
			//System.out.println(list.get(j));
			i = list.get(j);
			if (list_and.contains(i)){prev_is_and = 1;continue;}
			if (list_digit.contains(i)){present_class = 1;}
			else if(list_ten.contains(i)){present_class = 2;}
			else if(list_hundred.contains(i)){present_class = 3;}
			else if(list_special.contains(i)){present_class = 0;}
			if (prev_is_and == 1){
				final_str = final_str + i +' ';
				prev_is_and = 0;
				
				
			}
			else if (present_class != 0){
				if(present_class >= prev_class){final_str = final_str +"| "+i+" ";}
				else {final_str = final_str + i + " ";}
				prev_class = present_class;
			}
			else {
				if (prev_class != 3){
				//System.out.println("Now need add | ");
				final_str = final_str + "| " +i + " ";
				}
				else{final_str = final_str + i + " ";}
				}
				prev_class = present_class;
			}
		System.out.println(final_str);

		return final_str;

	}
	public static void main(String[] args){
		String str = "one two one hundred twenty eleven four hundred one twenty twelve three eleven three";
		SegmentNumber.segmentNum(str);
	}
}

