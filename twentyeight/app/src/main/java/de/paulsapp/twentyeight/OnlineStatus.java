package de.paulsapp.twentyeight;

public class OnlineStatus {

	OnlineStatusValue value=OnlineStatusValue.Red;

	public OnlineStatus() {
		// ping RaspberryPi = gelb
		// verbinde mit DB = grün
		// sonst = rot
	}
	public boolean isConnected() {
		if (value==OnlineStatusValue.Green) {
			return true;
		}
		return false;
	}
	public OnlineStatusValue getStatus(){
		return value;
	}

	public void setStatusAsOnline(){
		value=OnlineStatusValue.Green;
	}
	public void setStatusAsPingAble(){
		value=OnlineStatusValue.Yellow;
	}
	public void setStatusAsOffline(){
		value=OnlineStatusValue.Red;
	}
}
