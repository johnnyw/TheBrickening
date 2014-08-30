package com.vigasotech.brickening;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class HowtoActivity extends Activity {
	WebView mWebView;
	
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.webview);
        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.setBackgroundColor(Color.BLACK);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setSavePassword(false);
        webSettings.setSaveFormData(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(false);
        mWebView.loadUrl("file:///android_asset/howto.html");
	}
}
