package me.matthewmage.neumobile.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Pattern;


public class MainActivity extends ActionBarActivity {
    private final Config config = new Config();
    private final String tokenPref = "NU_TOKEN";
    private final String refreshPref = "REFRESH_TOKEN";

    private enum balanceType {
        H, L, M, P
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        // actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#D30000"))); // set your desired color

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        Config.NU_TOKEN = sharedPref.getString(tokenPref, "");
        Config.REFRESH_TOKEN = sharedPref.getString(refreshPref, "");
        new RetrieveMyNEUConfig().execute();

        Button schedule = (Button) findViewById(R.id.viewSchedule);
        schedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), Schedule.class);
                startActivity(intent);
            }
        });
    }

    private void showLogin() {
        final WebView login = (WebView) findViewById(R.id.loginWeb);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                login.setVisibility(View.VISIBLE);
            }
        });
        login.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // do your handling codes here, which url is the requested url
                // probably you need to open that url rather than redirect:

                if (url.startsWith("https://localhost")) {
                    log(url);
                    view.stopLoading();
                    config.code = url.split(Pattern.quote("?code="))[1];
                    new RetrieveTokens().execute(config.code);
                    return false;

                }
                view.loadUrl(url);
                return false; // then it is not handled by default action
            }
        });
        String uri = Config.SERVER_HOST + Config.NU_API_LOGIN_URI + "?client_id=" + Config.NU_API_CLIENT_ID
                + "&redirect_uri=" + Config.NU_API_REDIRECT_URI
                + "&response_type=code"
                + "&scope=" + Config.NU_API_SCOPE;
        login.loadUrl(uri);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (R.id.action_settings == id) {
            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(tokenPref, "");
            editor.putString(refreshPref, "");
            editor.apply();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // CookieManager cookieManager = CookieManager.getInstance();
                    // cookieManager.removeAllCookies(null);
                    showLogin();
                }
            });
            return true;
        }
        if (R.id.refresh_tokens == id) {
            new RefreshTokens().execute();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private static final String USER_AGENT = "Mozilla/5.0 (Linux; U; Android 4.0.3; ko-kr; LG-L160L Build/IML74K) AppleWebkit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30";

    private class RetrieveTokens extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String[] code) {
            JSONObject parent = new JSONObject();
            try {
                parent.put("code", code[0]);
                parent.put("client", Config.NU_API_CLIENT_NAME);
                parent.put("scope", Config.NU_API_SCOPE);
                parent.put("redirect_uri", Config.NU_API_REDIRECT_URI);
                parent.put("grant_type", "authorization_code");
            } catch (JSONException e) {
                e.printStackTrace();
            }


            String uri = Config.SERVER_HOST + Config.NU_API_TOKEN_URI2;
            log(uri);
            return makeRequest(uri, parent);
        }

        @Override
        protected void onPostExecute(String code) {
            if (code == null) {
                return;
            }
            storeTokens(code);
        }
    }

    private class RefreshTokens extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String[] code) {
            log("Refreshing Tokens....");
            JSONObject parent = new JSONObject();
            try {
                parent.put("refresh_token", Config.REFRESH_TOKEN);
                parent.put("client", Config.NU_API_CLIENT_NAME);
                parent.put("scope", Config.NU_API_SCOPE);
                parent.put("grant_type", "refresh_token");
            } catch (JSONException ignored) {

            }


            String uri = Config.SERVER_HOST + Config.NU_API_TOKEN_URI2;
            log(uri);
            return makeRequest(uri, parent);

        }

        @Override
        protected void onPostExecute(String code) {
            if (code == null) {
                return;
            }
            storeTokens(code);
        }
    }

    private class RetrieveMyNEU extends AsyncTask<String, Void, String> {


        @Override
        protected String doInBackground(String[] code) {


            String uri = Config.SERVER_HOST + Config.NU_API_MYNEUDATA_URL;
            log(uri);
            URL url = null;
            try {
                url = new URL(uri);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            try {
                HttpsURLConnection con = (HttpsURLConnection) ((url != null) ? url.openConnection() : null);

                //add request header
                assert con != null;
                con.setRequestMethod("GET");
                con.setRequestProperty("User-Agent", USER_AGENT);
                con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
                con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                con.setRequestProperty("Authorization", "Bearer " + Config.NU_TOKEN);

                final String responseString = con.getResponseMessage();
                final int responseCode = con.getResponseCode();
                log("NEU Response code: " + responseCode);
                if (responseCode > 200) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Retrieve ERROR" + responseCode + ": " + responseString, Toast.LENGTH_LONG).show();
                            new RefreshTokens().execute();
                        }
                    });
                    return null;

                }
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                return response.toString();
            } catch (FileNotFoundException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // new RefreshTokens().execute();
                    }
                });
                return null;
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String code) {
            if (code == null) {
                return;
            }
            try {
                parseData(code);
//                final String info = code;
//                final JSONObject mainObject = new JSONObject(code);
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        StringBuilder s = new StringBuilder();
//                        TextView text = (TextView) findViewById(R.id.name);
//                        Iterator<?> keys = mainObject.keys();
//                        while (keys.hasNext())
//                        {
//                            String key = (String) keys.next();
//                            try {
//                                if ( mainObject.get(key) instanceof JSONObject ) {
//                                    s.append(key + ": " + mainObject.get(key).toString() + "\n\n");
//                                }
//                            } catch (JSONException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                        text.setText(info);
//                    }
//                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class RetrieveMyNEUConfig extends AsyncTask<String, Void, String> {


        @Override
        protected String doInBackground(String[] code) {

            String uuid = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
            String uri = Config.SERVER_HOST + Config.MYNEU_CONFIG_URL +
                    "?myConfigVersion=0" +
                    "&appName=myneu" +
                    "&appversion=v0.8.907a" +
                    "&deviceId=" + uuid;
            log(uri);
            URL url = null;
            try {
                url = new URL(uri);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            try {
                HttpsURLConnection con = (HttpsURLConnection) ((url != null) ? url.openConnection() : null);

                //add request header
                assert con != null;
                con.setRequestMethod("GET");
                con.setRequestProperty("User-Agent", USER_AGENT);
                con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
                con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                con.setRequestProperty("Authorization", "Bearer " + Config.NU_TOKEN);

                final String responseString = con.getResponseMessage();
                final int responseCode = con.getResponseCode();
                log("NEU Config Response code: " + responseCode);
                if (responseCode > 200) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Retrieve ERROR" + responseCode + ": " + responseString, Toast.LENGTH_LONG).show();
                            // new RefreshTokens().execute();
                        }
                    });
                    return null;

                }
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                return response.toString();
            } catch (FileNotFoundException e) {
                log("ERROR GETTING NEU CONFIG");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // new RefreshTokens().execute();
                    }
                });
                return null;
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();

            }
            return null;
        }

        @Override
        protected void onPostExecute(String code) {
            if (code == null) {
                return;
            }
            try {
                Config.NU_API_MYNEUDATA_URL = new JSONObject(code).getString("NU_API_MYNEUDATA_URL") + '/' + Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new RetrieveMyNEU().execute();
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if (Config.NU_TOKEN.isEmpty() || Config.REFRESH_TOKEN.isEmpty()) {
                    showLogin();
                } else {
                    new RetrieveMyNEU().execute();
                }
            }
        }
    }

    private class RetrieveClassInfo extends AsyncTask<String, Void, String> {


        @Override
        protected String doInBackground(String[] code) {

            String uri = "http://coursepro.io/listSections";
            log(uri);
            URL url = null;
            try {
                url = new URL(uri);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            try {
                HttpURLConnection con = (HttpURLConnection) ((url != null) ? url.openConnection() : null);

                //add request header
                con.setRequestMethod("GET");
                con.setRequestProperty("User-Agent", USER_AGENT);
                con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
                con.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                final String responseString = con.getResponseMessage();
                final int responseCode = con.getResponseCode();
                log("CoursePro Response code: " + responseCode);
                if (responseCode > 200) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "CoursePro ERROR" + responseCode + ": " + responseString, Toast.LENGTH_LONG).show();
                            // new RefreshTokens().execute();
                        }
                    });
                    return null;

                }
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                return response.toString();
            } catch (FileNotFoundException e) {
                log("ERROR GETTING NEU CONFIG");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // new RefreshTokens().execute();
                    }
                });
                return null;
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();

            }
            return null;
        }

        @Override
        protected void onPostExecute(String code) {
            if (code == null) {
                return;
            }
            try {
                Config.NU_API_MYNEUDATA_URL = new JSONObject(code).getString("NU_API_MYNEUDATA_URL") + '/' + Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new RetrieveMyNEU().execute();
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void storeTokens(String code) {
        log("CODE: " + code);
        try {
            final JSONObject mainObject = new JSONObject(code);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    WebView login = (WebView) findViewById(R.id.loginWeb);
                    login.setVisibility(View.INVISIBLE);
                    try {
                        Config.NU_TOKEN = mainObject.getString("access_token");
                        Config.REFRESH_TOKEN = mainObject.getString("refresh_token");
                        // SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                        // SharedPreferences.Editor editor = sharedPref.edit();
                        // editor.putString(tokenPref, mainObject.getString("access_token"));
                        // editor.putString(refreshPref, mainObject.getString("refresh_token"));
                        // editor.commit();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    new RetrieveMyNEUConfig().execute();
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private String makeRequest(String uri, JSONObject parent) {
        try {
            URL url = new URL(uri);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

            // add request headers
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            con.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(parent.toString());
            wr.flush();
            wr.close();
            final String responseString = con.getResponseMessage();
            final int responseCode = con.getResponseCode();
            log("Sending 'POST' request to URL: " + url);
            log("Post parameters: " + parent);
            log("Response Code: " + responseCode);
            if (responseCode > 200) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Refresh ERROR" + responseCode + ": " + responseString, Toast.LENGTH_LONG).show();
                    }
                });
                return null;
            }


            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // print result
            log("Response Data: " + response);
            return response.toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private void parseData(String data) {
        try {
            final JSONObject main = new JSONObject(data);
            Config.data = main;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    // NAME FORMATTING //
                    TextView name = (TextView) findViewById(R.id.name);
                    TextView last_updated = (TextView) findViewById(R.id.last_updated);
                    try {
                        name.setText(String.format("%s %s", main.getString("fname"), main.getString("lname")));
                        last_updated.setText(String.format("Last updated: %s", DateUtils.getRelativeTimeSpanString((main.getLong("asOf") - 1L) / 1000L, System.currentTimeMillis() / 1000L, 0L, DateUtils.FORMAT_ABBREV_ALL)));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // HOLDS FORMATTING //
                    TextView holds = (TextView) findViewById(R.id.holds);
                    try {
                        holds.setText("");
                        JSONArray array = main.getJSONArray("holds");
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject adv = array.getJSONObject(i);
                            String title = adv.getString("title");
                            String description = adv.getString("description");
                            String telno = adv.getString("telno");
                            holds.setText(String.format(holds.getText() + "%s\n%s\nNumber: %s\n\n", title, description, telno));
                            findViewById(R.id.holds_card).setVisibility(View.VISIBLE);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                    // BALANCE FORMATTING //

                    try {
                        JSONObject b = main.getJSONObject("balances");
                        for (Iterator iterator = b.keys(); iterator.hasNext(); ) {
                            String key = (String) iterator.next();
                            for (int i = 0; i < b.getJSONArray(key).length(); i++) {
                                JSONObject bal = b.getJSONArray(key).getJSONObject(i);
                                String type = bal.getString("type");
                                String text = bal.getString("text");
                                MainActivity.balanceType ba = MainActivity.balanceType.valueOf(key);
                                TextView title;
                                TextView value;
                                switch (ba) {
                                    case H:
                                        title = (TextView) findViewById(R.id.huskyTitle);
                                        value = (TextView) findViewById(R.id.huskyBalance);
                                        title.setText("Husky Card:");
                                        value.setText(text);
                                        break;
                                    case L:
                                        title = (TextView) findViewById(R.id.laundryTitle);
                                        value = (TextView) findViewById(R.id.laundryBalance);
                                        title.setText("Laundry Bucks:");
                                        value.setText(text);
                                        break;
                                    case M:
                                        if (i == 0) {
                                            title = (TextView) findViewById(R.id.meal1Title);
                                            value = (TextView) findViewById(R.id.meal1Balance);
                                            title.setText(type.substring(1) + ':');
                                            value.setText(text);

                                        } else {
                                            title = (TextView) findViewById(R.id.meal2Title);
                                            value = (TextView) findViewById(R.id.meal2Balance);
                                            title.setText(type + ':');
                                            value.setText(text);
                                        }

                                        break;
                                    case P:
                                        title = (TextView) findViewById(R.id.printTitle);
                                        value = (TextView) findViewById(R.id.printBalance);
                                        title.setText("Printing:");
                                        value.setText(text);
                                        break;
                                }
                            }
                        }
                    } catch (JSONException e) {
                        //e.printStackTrace();
                    }

                    // SCHEDULE //
                    int currentDiff = -1;
                    Date nextClass = new Date();
                    try {
                        JSONArray arr = main.getJSONArray("courses");
                        TextView schedule = (TextView) findViewById(R.id.schedule);
                        schedule.setText("");
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject c = arr.getJSONObject(i);
                            String id = c.getString("id");
                            String title = c.getString("title");
                            String subtitle = c.getString("subtitle");
                            String instr = c.getString("instr");
                            JSONArray meetings = c.getJSONArray("mtngs");

                            StringBuilder meeting = new StringBuilder();

                            for (int j = 0; j < meetings.length(); j++) {
                                JSONObject m = meetings.getJSONObject(j);
                                String type = m.getString("mtngtyp");
                                String st = m.getString("st");
                                String ed = m.getString("ed");
                                String start_string = "";
                                String end_string = "";
                                if ((st != "null") && (ed != "null")) {
                                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                                    Date start = df.parse(st);
                                    Date end = df.parse(ed);
                                    SimpleDateFormat format = new SimpleDateFormat("MMM dd yyyy");
                                    start_string = format.format(start);
                                    end_string = format.format(end);
                                }

                                String stt = m.getString("stt");
                                String edt = m.getString("edt");

                                String start_time_string = "";
                                String end_time_string = "";
                                if ((stt != "null") && (edt != "null")) {
                                    DateFormat tf = new SimpleDateFormat("HHm", Locale.ENGLISH);
                                    Date start_time = tf.parse(stt);
                                    Date end_time = tf.parse(edt);

                                    SimpleDateFormat t_format = new SimpleDateFormat("hh:mma");
                                    start_time_string = t_format.format(start_time);
                                    end_time_string = t_format.format(end_time);
                                }
                                if (start_string.equals(end_string)) {
                                    meeting.append(String.format("Type: %s\nDate: %s\n%s - %s\n\n", type, start_string, start_time_string, end_time_string));
                                } else {
                                    meeting.append(String.format("Type: %s\nDates: %s - %s\n%s - %s\n\n", type, start_string, end_string, start_time_string, end_time_string));
                                }
                            }
                            schedule.setText(schedule.getText() + String.format("%s\n%s\n%s\n%s\n%s\n\n", id, title, subtitle, instr, meeting.toString()));

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    // ADVISOR FORMATTING //
                    TextView advisors = (TextView) findViewById(R.id.advisors);
                    try {
                        advisors.setText("");
                        JSONArray adv_array = main.getJSONArray("advisors");
                        for (int i = 0; i < adv_array.length(); i++) {
                            JSONObject adv = adv_array.getJSONObject(i);
                            String adv_name = adv.getString("name");
                            String phone = adv.getString("phone");
                            String email = adv.getString("email");
                            String type = adv.getString("advrType");
                            advisors.setText(String.format(advisors.getText() + "%s\n%s\nPhone: %s\nEmail: %s\n\n", adv_name, type, phone, email));
                        }

                    } catch (JSONException e) {
                        //e.printStackTrace();
                    }


                    // GRADES FORMATTING //

                    TextView grades = (TextView) findViewById(R.id.grades);
                    try {
                        log(main.getJSONObject("grades").toString());
                        grades.setText(String.format("Overall GPA: %s", main.getJSONObject("grades").getString("overallGPA")));

                    } catch (JSONException e) {
                        //e.printStackTrace();
                        findViewById(R.id.grades_card).setVisibility(View.GONE);
                    }

                    // EMERGENCY FORMATTING //
                    TextView emer = (TextView) findViewById(R.id.emergency);
                    try {
                        StringBuilder text = new StringBuilder();
                        JSONArray emer_a = main.getJSONArray("emergencies");
                        for (int i = 0; i < emer_a.length(); i++) {
                            JSONObject e = emer_a.getJSONObject(i);
                            String title = e.getString("title");
                            String description = e.getString("description");
                            text.append(String.format("%s: %s\n", title, description));

                        }
                        emer.setText(text);

                    } catch (JSONException ignored) {

                    }
                }
            });


        } catch (JSONException e) {
            e.printStackTrace();
        }


    }


// --Commented out by Inspection START (12/2/2015 10:54 AM):
//    private void log(int responseCode) {
//        Log.i(tag, "INT:" + responseCode);
//    }
// --Commented out by Inspection STOP (12/2/2015 10:54 AM)


    private static void log(String msg) {
        String tag = "NEU";
        Log.i(tag, msg);
    }

}
