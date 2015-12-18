package de.paulsapp.twentyeight;

/**
 * Created by pstiegele on 16.12.2015.
 */
public class Dosen {


    public static class SammelParamsStatic {
        String name;
        String status;
        Server server;

        SammelParamsStatic(String name, String status, Server server) {
            this.name = name;
            this.status = status;
            this.server = server;
        }

        SammelParamsStatic(Server server) {
            this.server = server;
        }


    }

}
