package me.matthewmage.neumobile.app;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Schedule extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONArray arr = Config.data.getJSONArray("courses");
                    for (int i = 0; i < arr.length(); i++) {
                        CardView card = (CardView) getLayoutInflater().inflate(R.layout.card_view, (ViewGroup) findViewById(R.id.mainLayout));
                        JSONObject c = arr.getJSONObject(i);
                        String title = c.getString("title");
                        String subtitle = c.getString("subtitle");
                        String instr = c.getString("instr");
                        JSONArray meetings = c.getJSONArray("mtngs");
                        for (int j = 0; j < meetings.length(); j++) {
                            JSONObject m = meetings.getJSONObject(j);
                            String st = m.getString("st");
                            String ed = m.getString("ed");

                            DateFormat df = new SimpleDateFormat("yyyy MM dd", Locale.ENGLISH);
                            Date start = df.parse(st);
                            Date end = df.parse(ed);
                            SimpleDateFormat format = new SimpleDateFormat("E, MMM dd yyyy");
                            String start_string = format.format(start);
                            String end_string = format.format(end);


                            String stt = m.getString("stt");
                            String edt = m.getString("edt");
                            DateFormat tf = new SimpleDateFormat("HHmm", Locale.ENGLISH);
                            Date start_time = tf.parse(stt);
                            Date end_time = tf.parse(edt);

                            SimpleDateFormat t_format = new SimpleDateFormat("hh:mm a");
                            String start_time_string = t_format.format(start_time);
                            String end_time_string = t_format.format(end_time);


                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_schedule, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
