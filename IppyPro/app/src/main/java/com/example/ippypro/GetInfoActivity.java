package com.example.ippypro;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;

public class GetInfoActivity extends AppCompatActivity {
    IPContainer container;
    IPInfo ipInfo;
    Location location;
    ASN ASN;
    Currency Currency;
    Timezone Timezone;
    Security Security;
    String ip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_info);
        Bundle addresses = getIntent().getExtras();
        utilizeAddresses(addresses);
    }

    public void utilizeAddresses(Bundle addresses) {
        ArrayList<String> ipArr = addresses.getStringArrayList("addresses");
        ip = ipArr.get(0);
        String binary = ipArr.get(1);
        String hex = ipArr.get(2);
        String octal = ipArr.get(3);
        ipInfo = new IPInfo(ip, binary, hex, octal);
        SendfeedbackJob job = new SendfeedbackJob();
        job.execute(ip);
    }

    private class SendfeedbackJob extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String[] params) {
            Secret secret = new Secret();
            HttpURLConnection httpClient;
            String address = params[0];
            String apiKey = secret.getKey();
            String configuredURL;
            URL url;
            int status;

            try {
                configuredURL = "https://api.smartip.io/" + address + "?api_key=" + apiKey;
                url = new URL(configuredURL);
                httpClient = (HttpURLConnection) url.openConnection();
                httpClient.setRequestMethod("GET");
                status = httpClient.getResponseCode();

                if (status != 200) {
                    return "Unable to retrieve IP Address";
                } else {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(httpClient.getInputStream()));
                    StringBuilder builder = new StringBuilder();
                    String line;
                    while((line = reader.readLine()) != null) {
                        builder.append(line + "\n");
                    }
                    reader.close();
                    httpClient.disconnect();
                    return builder.toString();
                }
            } catch(Exception e) {
                return e.toString();
            }
        }
        @Override
        protected void onPostExecute(String message) {
            TextView display = findViewById(R.id.display);
            try {
                JSONObject jsonAddress = new JSONObject(message);

//                DISPLAY INFORMATION
                String requesterIP = jsonAddress.getString("requester-ip");
                String execTime = jsonAddress.getString("execution-time");
                ipInfo.setIPAndTime(requesterIP, execTime);

//                GEOGRAPHY

                if (jsonAddress.isNull("geo")) {
                    location = new Location("N/A", "N/A", "N/A", "N/A", 0.1d, 0.1d);
                } else {
                    JSONObject geo = jsonAddress.getJSONObject("geo");
                    String countryName = geo.getString("country-name");
                    String capital = geo.optString("capital", "N/A");
                    String iso = geo.getString("country-iso-code");
                    String city = geo.getString("city");
                    double longitude = geo.getDouble("longitude");
                    double latitude = geo.getDouble("latitude");
                    location = new Location(countryName, capital, iso, city, longitude, latitude);
                }

//                 CURRENCY
                if (jsonAddress.isNull("currency")) {
                    Currency = new Currency("N/A", "N/A", "N/A", "N/A");
                } else {
                    JSONObject currency = jsonAddress.getJSONObject("currency");
                    String currencyNativeName = currency.getString("native-name");
                    String currencyCode = currency.getString("code");
                    String currencyName = currency.getString("name");
                    String currencySymbol = currency.getString("symbol");
                    Currency = new Currency(currencyNativeName, currencyCode, currencyName, currencySymbol);
                }

//                  TIMEZONE
                if (jsonAddress.isNull("timezone")) {
                    Timezone = new Timezone("N/A", "N/A", "N/A");
                } else {
                    JSONObject timezone = jsonAddress.getJSONObject("timezone");
                    String timezoneName = timezone.getString("microsoft-name");
                    String dateTime = timezone.getString("date-time");
                    String ianaName = timezone.getString("iana-name");
                    Timezone = new Timezone(timezoneName, dateTime, ianaName);
                }

//                  SECURITY
                JSONObject security = jsonAddress.getJSONObject("security");
                boolean isCrawler = security.getBoolean("is-crawler");
                boolean isProxy = security.getBoolean("is-proxy");
                boolean isTor = security.getBoolean("is-tor");
                Security = new Security(isCrawler, isProxy, isTor);

//                    ASN
                if (jsonAddress.isNull("asn")) {
                    ASN = new ASN("N/A", "N/A", "N/A", "N/A", "N/A");
                } else {
                    JSONObject asn = jsonAddress.getJSONObject("asn");
                    String asnName = asn.getString("name");
                    String asnDomain = asn.getString("domain");
                    String asnOrganization = asn.getString("organization");
                    String asnCode = asn.getString("asn");
                    String asnType = asn.getString("type");
                    ASN = new ASN(asnName, asnDomain, asnOrganization, asnCode, asnType);
                }

                container = new IPContainer(ipInfo, Currency, location, Security, ASN, Timezone);
                display.setText(container.displayGeneral());
            } catch (Exception e) {
                display.setText(e.toString());
                e.printStackTrace();
            }
        }
    }

    public void onBack(View view) {
        Bundle bundle = new Bundle();
        Intent intent = new Intent(this, MainActivity.class);
        bundle.putString("ip", ip);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    public void onSave(View view) {
        TextView display = findViewById(R.id.display);
        display.setText("This feature will be available in the near future :)");
    }

    public void onDisplayGeneral(View view) {
        TextView display = findViewById(R.id.display);
        display.setText(container.displayGeneral());
    }

    public void onDisplayASN(View view) {
        TextView display = findViewById(R.id.display);
        display.setText(container.displayASN());
    }

    public void onDisplayGeographics(View view) {
        TextView display = findViewById(R.id.display);
        display.setText(container.displayLocation());
    }

    public void onDisplayAll(View view) {
        TextView display = findViewById(R.id.display);
        display.setText(container.displayAll());
    }
}
