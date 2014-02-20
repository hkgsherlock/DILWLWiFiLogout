package hdse.dilwlwifi.logout;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.*;

public class MainActivity extends Activity {

	TextView txtLoginStatus;
	TextView txtLogoutUrl;
	Button btnLoginLogout;
	Button btnRefresh;
	static String logoutURL;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		txtLoginStatus = (TextView) findViewById(R.id.txtLoginStatus);
		txtLogoutUrl = (TextView) findViewById(R.id.txtLogoutUrl);
		btnLoginLogout = (Button) findViewById(R.id.btnLoginLogout);
		btnRefresh = (Button) findViewById(R.id.btnRefresh);

		this.btnLoginLogout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View parent) {
				excuteGet(logoutURL, new AsyncHttpResponseHandler() {
					@Override
					public void onSuccess(String response) {
						if (checkLoggedOut(response)) {
							txtLoginStatus.setText("Not logged in");
							txtLogoutUrl.setText("N/A");
							btnLoginLogout.setEnabled(false);
							Toast.makeText(btnLoginLogout.getContext(), "Logout Successful", Toast.LENGTH_SHORT).show();
						} else {
							Toast.makeText(btnLoginLogout.getContext(), "Failed to logout! Refresh and try again?", Toast.LENGTH_LONG).show();
						}
					}

					@Override
					public void onFailure(Throwable e, String response) {
						Toast.makeText(btnLoginLogout.getContext(), "Failed to logout! Refresh and try again?", Toast.LENGTH_LONG).show();
					}
				});
			}
		});

		this.btnRefresh.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View parent) {
				fetchStatus();
			}
		});

		if (this.txtLoginStatus.getText().toString().compareTo("OK") == 0) {
			this.btnLoginLogout.setEnabled(true);
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		fetchStatus();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		this.finish();
	}
	
	private void fetchStatus() {
		btnLoginLogout.setEnabled(false);
		
		// check if WiFi
		WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		if (!wm.isWifiEnabled()) {
			txtLoginStatus.setText("Wi-Fi Unavailalbe");
			txtLogoutUrl.setText("N/A");
			Toast.makeText(btnRefresh.getContext(), "Wi-Fi is not available!", Toast.LENGTH_LONG).show();
			return;
		}
		
		String ssid = wm.getConnectionInfo().getSSID();
		
		if (ssid == null) {
			txtLoginStatus.setText("Wi-Fi not connected");
			txtLogoutUrl.setText("N/A");
			Toast.makeText(btnRefresh.getContext(), "Wi-Fi is not connecting to any AP!", Toast.LENGTH_LONG).show();
			return;
		}
		
		// check if current WiFi AP's SSID is "VTC"
		if (ssid.compareTo("VTC") != 0) {
			txtLoginStatus.setText("\"VTC\" not connected");
			txtLogoutUrl.setText("N/A");
			Toast.makeText(btnRefresh.getContext(), "Wi-Fi is not connecting to AP called \"VTC\"!", Toast.LENGTH_LONG).show();
			return;
		}

		// really fetch page and grab link
		excuteGet("https://dilwlbs.secured.vtc.edu.hk/login.pl", new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(String response) {
				if (!checkLoggedIn(response)) {
					txtLoginStatus.setText("Not logged in");
					txtLogoutUrl.setText("N/A");
					Toast.makeText(btnRefresh.getContext(), "You are not logged into the VTC WiFi.", Toast.LENGTH_LONG).show();
					return;
				}
				
				logoutURL = grabLogoutUrl(response);
				if (logoutURL == null) {
					new AlertDialog.Builder(btnRefresh.getContext())
									.setTitle("Error")
									.setMessage("The application cannot grab the link to logout the WiFi.\nTrace of the response:\n\n" + response)
									.show();
					txtLoginStatus.setText("Parser Error");
					txtLogoutUrl.setText("(Failed to grab logout URL)");
					return;
				}
				
				txtLoginStatus.setText("OK");
				txtLogoutUrl.setText(logoutURL);
				btnLoginLogout.setEnabled(true);
			}

			@Override
			public void onFailure(Throwable e, String response) {
				Toast.makeText(btnRefresh.getContext(), "Failed to get response from VTC WiFi service!", Toast.LENGTH_LONG).show();
			}
		});
	}
	
	private static boolean checkLoggedIn(String content) {
		return content.indexOf("Thank you for signing in.") != -1;
	}
	
	private static boolean checkLoggedOut(String content) {
		return content.indexOf("You have successfully logged out.") != -1;
	}

	private static String grabLogoutUrl(String content) {
		int startPos = content.indexOf("/login.pl?");

		if (startPos == -1) {
			return null;
		}

		int endPos = content.indexOf("\">", startPos);

		if (endPos == -1) {
			return null;
		}

		return "https://dilwlbs.secured.vtc.edu.hk" + content.substring(startPos, endPos);
	}

	private static void excuteGet(String targetURL, AsyncHttpResponseHandler handler) {
		RequestParams params = new RequestParams();

		AsyncHttpClient client = new AsyncHttpClient();
		client.get(targetURL, params, handler);
	}

}
