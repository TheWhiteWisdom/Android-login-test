package testing.hyyrynen.fredrik.loginview_test;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class LogInActivity extends AppCompatActivity implements AsyncProcessListener {

    Button signInButton;
    EditText password_field;
    EditText email_field;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        email_field = (EditText) findViewById(R.id.email_editText);
        password_field = (EditText) findViewById(R.id.password_editText);
        signInButton = (Button) findViewById(R.id.signIn_button);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Send request
                sendSignInRequest(email_field.getText().toString(), password_field.getText().toString());
                // Disable fields and buttons.
                // Also indicates an attempt to sign in.
                email_field.setEnabled(false);
                password_field.setEnabled(false);
                signInButton.setEnabled(false);
            }
        });
    }

    void sendSignInRequest(String email, String password){
        try {
            /* Setup JSON object for message */
            JSONObject signInDetail = new JSONObject();
            signInDetail.put("email", email);
            signInDetail.put("password", password);

            /* Send message and wait for response */
            HTTPCommunicator httpCommunicator = new HTTPCommunicator();
            System.err.println("Data:\n" + signInDetail.toString());
            httpCommunicator.asyncProcessListener = this;
            httpCommunicator.execute("http://stagecast.se/api/users/login", signInDetail.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void processFinished(String response) {
        // Enable fields and buttons
        // Sign in attempt finished
        email_field.setEnabled(true);
        password_field.setEnabled(true);
        signInButton.setEnabled(true);

        // Read response and act accordingly

        // Used to display messages
        AlertDialog.Builder ADBuilder = new AlertDialog.Builder(this);

        if(response.equals("")){
            //No response! Send error message
            ADBuilder.setTitle("Sign in failed");
            ADBuilder.setMessage("Wrong email or password!\nPlease try again.");
            ADBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Do nothing
                    return;
                }
            }).show();
            // Jump out of method.
            return;
        }

        JSONObject jsonResponse = null;
        try {
            jsonResponse = new JSONObject(response);
        if(jsonResponse != null && jsonResponse.get("token") != null && !jsonResponse.get("token").toString().equals("")){
            ADBuilder.setTitle("Success!");
            ADBuilder.setMessage("Sign in successfull!\nToken gotten from server:\n" + jsonResponse.get("token"));
            ADBuilder.setPositiveButton("NICE!", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Send user to another dimension. But for now, do nothing.
                    return;
                }
            }).show();
        }
        else{
            //Faulty response format. Send error message
            ADBuilder.setTitle("Sign in failed");
            ADBuilder.setMessage("Something went wrong!\nPlease try again.");
            ADBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Do nothing
                    return;
                }
            }).show();
        }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}

// Run HTTP requests on separate threads
class HTTPCommunicator extends AsyncTask<String, Void, String>{

    AsyncProcessListener asyncProcessListener = null;

    @Override
    protected String doInBackground(String... params) {
        String resultData = "";
        HttpURLConnection urlConnection = null;
        System.err.println(params[1]);
        try {
            // Setup and open connection
            URL url = null;
            url = new URL(params[0]);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Accept","application/json");
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setConnectTimeout(1000);

            // Send data
            DataOutputStream dos = new DataOutputStream(urlConnection.getOutputStream());
            System.err.println("Posting: " + params[1]);
            dos.writeBytes(params[1]);
            dos.flush();
            dos.close();

            // Recieve response
            if(urlConnection.getResponseCode() == 200) {
                InputStream in = urlConnection.getInputStream();
                InputStreamReader isr = new InputStreamReader(in);

                int inputData = isr.read();
                while (inputData != -1) {
                    resultData += (char) inputData;
                    inputData = isr.read();
                }
            }
            else
                System.err.println("Response code: " + urlConnection.getResponseCode());

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(urlConnection != null)
                urlConnection.disconnect();
        }
        return resultData;
    }

    /*
    *   Executes when HTTP POST request finishes
     */
    @Override
    protected void onPostExecute(String response){
        super.onPostExecute(response);
        // Call for response handling
        asyncProcessListener.processFinished(response);
    }
}

interface AsyncProcessListener{
    void processFinished(String result);
}