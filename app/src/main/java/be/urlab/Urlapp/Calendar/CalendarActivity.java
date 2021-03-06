package be.urlab.Urlapp.Calendar;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import be.urlab.Urlapp.R;
import utils.date.DateUtil;
import utils.format.MarkDownToHtml;

public class CalendarActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private final String url = "https://urlab.be/api/events/?ordering=-start";
    private ArrayList<Event> events = new ArrayList<Event>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calendar_side_menu_main);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, null, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();

        final Activity activity = this;
        events.clear();

        RequestQueue queue = Volley.newRequestQueue(this);

        // Request a string response from the provided URL.
        JsonObjectRequest stringRequest = new JsonObjectRequest(Request.Method.GET, url,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            for (int i = 0; i < response.getJSONArray("results").length(); ++i) {
                                JSONObject eventJSON = (JSONObject) response.getJSONArray("results").get(i);
                                try {
                                    String status = eventJSON.getString("status");
                                    Date start = null;
                                    Date stop = null;
                                    if (status.equals("r")) {
                                        start = DateUtil.fromString(eventJSON.getString("start"), "yyyy-MM-dd'T'HH:mm:ss");
                                        stop = DateUtil.fromString(eventJSON.getString("stop"), "yyyy-MM-dd'T'HH:mm:ss");
                                    }
                                    String imageUrl = eventJSON.getString("picture");
                                    String description = eventJSON.getString("description");
                                    Event anEvent = new Event(eventJSON.getString("title"),eventJSON.getString("place"),start,stop,imageUrl,description,status);
                                    events.add(i, anEvent);
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }
                            updateListView(null);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i("JSONObject response", "Error");
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);

        ListView eventsListView = (ListView) this.findViewById(R.id.eventsListView);
        eventsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Event anEvent = (Event) parent.getItemAtPosition(position);

                LinearLayout r = (LinearLayout) findViewById(R.id.calendar_main);

                final PopupWindow popup = new PopupWindow(r,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        true);
                popup.setBackgroundDrawable(getDrawable(R.drawable.event_selected_window));
                popup.setOutsideTouchable(true);

                LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View v = inflater.inflate(R.layout.event_selected, parent, false);
                TextView title = (TextView) v.findViewById(R.id.title);
                title.setText(anEvent.getTitle());
                WebView description = (WebView) v.findViewById(R.id.description);
                description.loadData(MarkDownToHtml.format(anEvent.getDescription()), "text/html; charset=UTF-8", null);
                description.setBackgroundColor(Color.TRANSPARENT);
                description.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

                TextView place = (TextView) v.findViewById(R.id.place);
                place.setText(anEvent.getPlace());
                TextView date = (TextView) v.findViewById(R.id.date);
                if (!anEvent.isIncubated()) {
                    date.setText(anEvent.getTime());
                } else {
                    date.setVisibility(View.GONE);
                }

                popup.setContentView(v);
                popup.showAtLocation(parent, Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);

                View container = null;
                if (android.os.Build.VERSION.SDK_INT==Build.VERSION_CODES.M) {
                    container = (View) popup.getContentView().getParent().getParent();
                } else {
                    container = (View) popup.getContentView().getParent();
                }
                WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                WindowManager.LayoutParams pe = (WindowManager.LayoutParams) container.getLayoutParams();
                pe.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
                pe.dimAmount = 0.5f;
                wm.updateViewLayout(container, pe);

            }
        });

    }

    public void updateListView(View clickedCheckbox) {
        ListView eventsListView = (ListView) this.findViewById(R.id.eventsListView);

        List<Event> filteredEventArray = new ArrayList<Event>();
        int j=0;
        for (Event anEvent : events) {
            CheckBox pastEventsCheckbox = (CheckBox) this.findViewById(R.id.past_events);
            CheckBox upcomingEventsCheckbox = (CheckBox) this.findViewById(R.id.upcoming_events);
            CheckBox incubatedEventsCheckbox = (CheckBox) this.findViewById(R.id.incubated_events);
            if (    ((!anEvent.isIncubated()) && anEvent.isPassed() && pastEventsCheckbox.isChecked()) ||
                    ((!anEvent.isIncubated()) && (!anEvent.isPassed()) && upcomingEventsCheckbox.isChecked()) ||
                    (anEvent.isIncubated() && incubatedEventsCheckbox.isChecked())  ){
                filteredEventArray.add(j++, anEvent);
            }
        }
        EventListAdapter adapter = new EventListAdapter(this,filteredEventArray);
        eventsListView.setAdapter(adapter);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
