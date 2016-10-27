package sexy.debug.weather_send;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Timer;
import java.util.TimerTask;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity implements Handler.Callback {

    public final static String TAG = "WEATHERSEND";

    // UI
    private TextView tvToday;
    private TextView tvTmx;
    private TextView tvTmn;
    private TextView tvSky;
    private TextView tvPty;
    private TextView tvSendWeather;
    private TextView tvSendTime;
    private Button btConnect;
    private Button btSendTime;
    private Button btNorain;
    private Button btRain;

    //
    private BluetoothService btService;
    private BluetoothThread btThread;
    private Handler btHandler;

    private AsyncHttpClient client;
    private Date nowDate;       // base 날짜

    // Timer
    private TimerTask timerTask;
    private Timer timer;

    private Handler updateLabelHandler;

    // 발송 bytes
    private byte[] weather;
    private byte[] time;

    // 데이터들
    private double tmx;            // 최고온도
    private double tmn;            // 최저온도
    private int sky;
    private int pty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ui init
        tvToday = (TextView) findViewById(R.id.today);
        tvTmx = (TextView) findViewById(R.id.tmx);
        tvTmn = (TextView) findViewById(R.id.tmn);
        tvSky = (TextView) findViewById(R.id.sky);
        tvPty = (TextView) findViewById(R.id.pty);
        tvSendWeather = (TextView) findViewById(R.id.send_weather);
        tvSendTime = (TextView) findViewById(R.id.send_time);
        btConnect = (Button) findViewById(R.id.connect_button);
        btSendTime = (Button) findViewById(R.id.send_time_button);
        btNorain = (Button) findViewById(R.id.send_norain_weather_button);
        btRain = (Button) findViewById(R.id.send_rain_weather_button);

        // listener
        btConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // bluetooth
                btHandler = new Handler(MainActivity.this);
                btService = new BluetoothService(MainActivity.this, btHandler);
                BluetoothDevice sunk = btService.getEceDevice();
                if (sunk == null) {
                    Log.d(TAG, "Failed to find sunk");
                    return;
                }

                btThread = new BluetoothThread(sunk);
                if (!btService.getDeviceState()) {
                    Log.d(TAG, "Service Fail and Enable");
                    btService.enableBluetooth();
                }

                Log.d(TAG, "Click button and success.");
                btThread.start();
            }
        });

        btSendTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendTime();
            }
        });

        btNorain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pty = 0;
                sendWeather();
            }
        });

        btRain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pty = 1;
                sendWeather();
            }
        });

        // byte init
        weather = new byte[8];
        time = new byte[8];

        // http
        client = new AsyncHttpClient();

        // timer
        timerTask = new TimerTask() {
            @Override
            public void run() {
                updateWeatherBytes();
                updateTimeBytes();

                sendWeather();
                sendTime();

                // label update
                updateLabels();
            }
        };

        timer = new Timer();
        updateLabelHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                updateLabelsByHandler();
            }
        };

        requestWeather();
    }

    private void requestWeather() {
        // now date
        nowDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(nowDate);

        calendar.add(Calendar.DATE, -1);
        Date targetDate = calendar.getTime();

        // server resource
        String base = getResources().getString(R.string.weather_url);
        String key = getResources().getString(R.string.weather_key);

        // 원천동
        int nx = 61;
        int ny = 120;

        // time
        String basedate = sdf.format(targetDate);
        String basetime = "2000";

        RequestParams params = new RequestParams();
        params.add("ServiceKey", key);
        params.add("base_date", basedate);
        params.add("base_time", basetime);
        params.add("nx", Integer.toString(nx));
        params.add("ny", Integer.toString(ny));
        params.add("_type", "json");
        params.add("numOfRows", "255");

        Log.d(TAG, base);
        Log.d(TAG, params.toString());
        client.get(base, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.d(TAG, "=============== SUCCESS ===============");
                String body = new String(responseBody);
                Log.d(TAG, body);

                parseJson(body);

                // timer 통해 send data
                timer.schedule(timerTask, 0, 18000000);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d(TAG, "=============== FAILED ===============");
            }
        });
    }

    private void parseJson(String body) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String now = sdf.format(nowDate);

        tmx = 0.0;
        tmn = 0.0;
        sky = 0;
        pty = 0;

        try {
            JSONObject total = new JSONObject(body);
            JSONObject totalBody = total.getJSONObject("response").getJSONObject("body");
            JSONArray items = totalBody.getJSONObject("items").getJSONArray("item");

            for (int index = 0; index < items.length(); index++) {
                JSONObject item = items.getJSONObject(index);
                int fcstDate = item.getInt("fcstDate");
                int nowIntDate = Integer.parseInt(now);
                if (fcstDate == nowIntDate) {
                    String category = item.getString("category");
                    double value = item.getDouble("fcstValue");

                    if (category.equals("TMX")) {
                        tmx = value;
                    } else if (category.equals("TMN")) {
                        tmn = value;
                    } else if (category.equals("SKY")) {
                        int intval = ((int) value) - 1;
                        sky = (sky < intval) ? intval : sky;
                    } else if (category.equals("PTY")) {
                        int intval = (int) value;
                        pty = (pty < intval) ? intval : pty;
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void updateLabelsByHandler() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        tvToday.setText(sdf.format(nowDate));

        tvTmx.setText(Double.toString(tmx));
        tvTmn.setText(Double.toString(tmn));

        tvSky.setText(Integer.toString(sky));
        tvPty.setText(Integer.toString(pty));

        StringBuilder w = new StringBuilder();
        StringBuilder t = new StringBuilder();
        for (byte element : weather) {
            w.append(String.format("%02x ", element & 0xFF));
        }
        for (byte element : time) {
            t.append(String.format("%02x ", element & 0xFF));
        }

        tvSendWeather.setText(w.toString());
        tvSendTime.setText(t.toString());
    }

    private void updateLabels() {
        updateLabelHandler.sendEmptyMessage(0);
    }

    private void updateWeatherBytes() {
        int weatherCode = sky * 4 + pty;
        int intTmx = (int) Math.round(tmx);
        int intTmn = (int) Math.round(tmn);

        int tmxTen = intTmx / 10;
        int tmxOne = intTmx % 10;
        int tmnTen = intTmn / 10;
        int tmnOne = intTmn % 10;

        weather[0] = 0x02;
        weather[1] = 0x31;
        weather[2] = (byte) (0x30 + weatherCode);
        weather[3] = (byte) (0x30 + tmxTen);
        weather[4] = (byte) (0x30 + tmxOne);
        weather[5] = (byte) (0x30 + tmnTen);
        weather[6] = (byte) (0x30 + tmnOne);
        weather[7] = 0x03;
    }

    private void updateTimeBytes() {
        Date now = new Date();
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(now);

        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int min = calendar.get(Calendar.MINUTE);

        int hourTen = hour / 10;
        int hourOne = hour % 10;
        int minTen = min / 10;
        int minOne = min % 10;

        time[0] = 0x02;
        time[1] = 0x32;
        time[2] = (byte) (0x30 + hourTen);
        time[3] = (byte) (0x30 + hourOne);
        time[4] = (byte) (0x30 + minTen);
        time[5] = (byte) (0x30 + minOne);
        time[6] = 0x00;
        time[7] = 0x03;
    }

    private void sendWeather() {
        updateWeatherBytes();
        updateLabels();

        if (btThread != null) {
            Log.d(TAG, "Send weather");
            btThread.write(weather);
        }
    }

    private void sendTime() {
        updateTimeBytes();
        updateLabels();

        if (btThread != null) {
            Log.d(TAG, "Send time");
            btThread.write(time);
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }
}
