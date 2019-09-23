package ch.teko.broadcastrecieverexample;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private final String LOG_TAG = "MainActivity";
    private final String api_url = "http://172.20.10.7:8080/api";
    private final int RELOADING_TIME_IN_MILLIS = 5000;
    private boolean isConnected = false;
    private boolean isDisplayOn = false;
    private String shoppingListJSON = "";
    private EditText editText_new_item;
    private String newItem;
    private BroadcastReceiver connectivityReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreate() called");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectivityReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    Log.d(LOG_TAG, intent.getAction());
                    Log.d(LOG_TAG, ConnectivityManager.CONNECTIVITY_ACTION);

                    if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                        isConnected = isNetworkInterfaceAvailable(context);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            private boolean isNetworkInterfaceAvailable(Context context) {
                final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = connMgr.getActiveNetworkInfo();
                return (activeNetwork != null && activeNetwork.isConnected());
            }
        };
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connectivityReceiver, filter);

        editText_new_item = findViewById(R.id.editText_new_item);

        // hide keyboard when not focused on editText_new_item
        editText_new_item.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            }
        });

        Log.d(LOG_TAG, "onCreate() ended");
    }

    @Override
    protected void onStart() {
        Log.d(LOG_TAG, "onStart() called");
        super.onStart();
        isDisplayOn = true;
        runReloadThread();
        Log.d(LOG_TAG, "onStart() ended");
    }

    private void runReloadThread() {
        Thread reloadThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        if (isConnected == true) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    loadTable();
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showNoInternet("NO INTERNET CONNECTION");
                                }
                            });
                        }
                        Thread.sleep(RELOADING_TIME_IN_MILLIS);

                        if(!isDisplayOn) break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        reloadThread.start();
    }

    private void loadTable() {
        getShoppingListJSON();
        Log.d(LOG_TAG, shoppingListJSON);

        TableLayout tableLayout = findViewById(R.id.tableLayoutContents);

        // clear table when reloading
        if (tableLayout.getChildCount() != 0) {
            tableLayout.removeAllViews();
            Log.d(LOG_TAG, "tableLayout all children removed");
        }

        try {
            JSONArray jsonArray = new JSONArray(shoppingListJSON);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject itemObject = jsonArray.getJSONObject(i);
                String id = itemObject.getString("id");
                String name = itemObject.getString("name");
                String timeInMillis = itemObject.getString("time");
                String marked = itemObject.getString("marked");

                Date date = new Date(Long.parseLong(timeInMillis));
                String date_str = DateFormat.format("MM/dd/yyyy HH:mm", date).toString();

                Boolean marked_boolean = false;
                if (marked.equals("0")) {
                    marked_boolean = false;
                } else {
                    marked_boolean = true;
                }

                TableRow tableRow = new TableRow(this);

                TextView tv_item = new TextView(this);
                tv_item.setText(name);
                tv_item.setTextColor(Color.BLUE);
                tv_item.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                tv_item.setGravity(Gravity.CENTER);
                tableRow.addView(tv_item);

                TextView tv_date = new TextView(this);
                tv_date.setText(date_str);
                tv_date.setTextColor(Color.BLUE);
                tv_date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                tv_date.setGravity(Gravity.CENTER);
                tableRow.addView(tv_date);

                CheckBox checkBox = new CheckBox(this);
                checkBox.setId(Integer.parseInt(id)); // set checkBox id to item id from Database
                checkBox.setChecked(marked_boolean);
                checkBox.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); // checkbox align right
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        final int itemId = buttonView.getId();
                        Thread communicationThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    new URL(api_url + "/?command=mark&id=" + itemId).openStream();

                                } catch (UnknownHostException e) {
                                    e.printStackTrace();
                                } catch (MalformedURLException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        communicationThread.start();
                    }
                });

                tableRow.addView(checkBox);
                tableLayout.addView(tableRow);
                Log.d(LOG_TAG, "tableLayout one TableRow added");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // called inside loadTable()
    private void getShoppingListJSON() {

        Thread communicationThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream inputStream = new URL(api_url).openStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                    StringBuffer buffer = new StringBuffer();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        buffer.append(line);
                    }
                    shoppingListJSON = buffer.toString();

                } catch (ConnectException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showNoInternet("404 !!! ");
                        }
                    });
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        communicationThread.start();

        // *** wait until the communicationThread to get transportsJsonStr from url
        try {
            communicationThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void showNoInternet(String msg) {
        TableLayout tableLayout = findViewById(R.id.tableLayoutContents);

        // clear table when reloading
        if (tableLayout.getChildCount() != 0) {
            tableLayout.removeAllViews();
            Log.d(LOG_TAG, "tableLayout all children removed");
        }

        TableRow tableRow = new TableRow(this);
        TextView tv = new TextView(this);
        tv.setText(msg);
        tv.setTextColor(Color.RED);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tv.setGravity(Gravity.CENTER);
        tableRow.addView(tv);
        tableLayout.addView(tableRow);
    }

    public void onButtonAddClicked(View v) {
        editText_new_item.clearFocus();
        newItem = editText_new_item.getText().toString();
        editText_new_item.setText("");

        Thread communicationThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new URL(api_url + "/?command=insert&name=" + newItem).openStream();

                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        communicationThread.start();

        // *** wait until the communicationThread to get transportsJsonStr from url
        try {
            communicationThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        loadTable();
    }

    public void onButtonDeleteClicked(View v) {
        Thread communicationThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new URL(api_url + "/?command=delete").openStream();

                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        communicationThread.start();

        // *** wait until the communicationThread to get transportsJsonStr from url
        try {
            communicationThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        loadTable();
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    protected void onStop() {
        Log.d(LOG_TAG, "onStop() called");
        super.onStop();
        isDisplayOn = false;
        Log.d(LOG_TAG, "onStop() ended");
    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "onDestroy() called");
        super.onDestroy();
        unregisterReceiver(connectivityReceiver);
        Log.d(LOG_TAG, "onDestroy() ended");
    }
}
