package cc.ecisr.jyutdict;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;

/**
 * app 的關於頁面
 */
public class InfoActivity extends AppCompatActivity {
	WebView webView;
//	static final String INFO_HTML_FILE_URL = "https://www.jyutdict.org/about";
	static final String INFO_HTML_FILE_URL = "file:///android_asset/info/info.html";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_info);
		webView = findViewById(R.id.web_view);
		WebSettings webSettings = webView.getSettings();
		
		webView.loadUrl(INFO_HTML_FILE_URL);
		
		
	}
}
