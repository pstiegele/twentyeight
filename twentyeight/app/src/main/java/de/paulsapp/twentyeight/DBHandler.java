package de.paulsapp.twentyeight;

public class DBHandler {

	boolean connected=false;	//false=nicht verbunden true=verbunden
	Server server;
	public DBHandler(Server server){
		this.server=server;
		
	}
	
	public boolean createIrgendwas(){
		//erstelle Tabelle, oder Eintrag oder so.. (vlt mehrere Methoden...
		return false;
	}
	
	public Object askfor(){
		//fragt Sachen an, z.B. zuletzt gemessene Temperatur usw..
		return false;
	}
	
	
	public boolean disconnect(){
		
		return false;
	}
}
