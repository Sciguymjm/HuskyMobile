package me.matthewmage.neumobile.app;

import org.json.JSONObject;

/**
 * Created by Matt on 11/19/2015.
 */
public class Config {
    public static String NU_TOKEN = "";
    public static String REFRESH_TOKEN = "";
    public static String SERVER_HOST = "https://prod-web.neu.edu/wasapp/";
    public static String NU_API_LOGIN_URI = "mob2/oauth/authorize";
    public static String NU_API_CHECKLOGIN_URI = "mob2/secure/user";
    public static String NU_API_TOKEN_URI = "mob2/oauth/token";
    public static String NU_API_TOKEN_URI2 = "mob2/open/refreshToken";
    public static String NU_API_REDIRECT_URI = "http://localhost";
    public static String NU_API_CLIENT_ID = "NUGO";
    public static String NU_API_SCOPE = "read";
    public static String NU_API_CLIENT_NAME = "NUGO";
    public static String NU_API_MYNEUDATA_URL = "mob2/secure/myneu";
    public static String NUZE_DATA_URL = "mob2/secure/myprivate String NUze";
    public static String MYNEU_CONFIG_URL = "mob2/mobileApp/config";
    public static String NU_BUILDINGS_DATA_URL = "mob2/open/buildings";
    public static String NU_NEARME_DATA_URL = "mob2/open/nearmedata";
    public static JSONObject data;
    public String code;
}
