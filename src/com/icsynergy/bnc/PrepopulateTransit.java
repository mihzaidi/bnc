package com.icsynergy;


import java.util.Arrays;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class PrepopulateTransit {
    public PrepopulateTransit() {
        super();
    }

    public static void main(String[] args) {
        //  String b = Transit(a);
      List<String> a = new ArrayList<String>();
      String Alpha = "a";  
      String Beta = "b";
      String Gamma = "g";
      String Delta = "d";
      for (int i=0; i<2; i++) {
          a.add(Alpha);
          a.add(Beta);
          System.out.println(a);
      }
     System.out.println(a);
    }
    public static String Transit(String inputvalue) {
        //Determine if Values are strictly numeric with no spaces or letters
        if (inputvalue.matches("^[0-9]+$")) {
            //Transit Logic where there are five digits then add - between second last and last digit
            if (inputvalue.length() == 5) {
                inputvalue = new StringBuilder(inputvalue).insert(inputvalue.length() - 1, "-").toString();
                return inputvalue;
            }
            //If value is only 4 digits then add -1 to end
            else if (inputvalue.length() == 4) {
                inputvalue = new StringBuilder(inputvalue).insert(inputvalue.length(), "-1").toString();
                return inputvalue;
            }
            //Any other value returns as is
            else
                return inputvalue;
        }
        //Any Values that are alphanumeric return as is
        else {
            return inputvalue;
        }
    }
}


