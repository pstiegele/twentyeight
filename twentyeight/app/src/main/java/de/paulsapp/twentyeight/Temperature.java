package de.paulsapp.twentyeight;

public class Temperature {

	boolean measured=false; 	//false=set true=measured
	boolean indoor=true;		//true=indoor false=outdoor
	TemperatureValue current;	
	TemperatureValue max;	
	TemperatureValue min;	
	public Temperature(){
		
		
	}
	
	public TemperatureValue returnValue(){
		
		return current;
	}
}
