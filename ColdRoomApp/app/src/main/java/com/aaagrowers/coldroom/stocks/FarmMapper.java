package com.aaagrowers.coldroom.stocks;

public class FarmMapper {

    public static int getFarmId(String farmName){

        if(farmName == null) return 0;

        switch(farmName.toLowerCase()){

            case "simba":
                return 1;

            case "chui":
                return 2;

            case "chestnut":
                return 3;

            default:
                return 0;
        }
    }
}