package cc.ecisr.jyutdict.utils;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUtil {
	private static final String TAG = "`HttpUtil";
	
	public static final Boolean POST = true;
	public static final Boolean GET = false;
	public static final int EMPTY_URL_OR_HANDLER = 9100;
	public static final int REQUEST_CONTENT_SUCCESSFULLY = 9200;
	public static final int REQUEST_CONTENT_FAIL = 9201;
	
	
	private String urlStr = "";
	private Handler handler;
	private GetThread getThread;
	private Boolean mode; // False For GET, True For POST
	
	private int messageWhat;
	
	public HttpUtil(Boolean mode) {
		this.mode = mode;
	}
	
	public HttpUtil setUrl(String urlStr) {
		this.urlStr = urlStr;
		return this;
	}
	public HttpUtil setHandler(Handler handler) {
		this.handler = handler;
		messageWhat = REQUEST_CONTENT_SUCCESSFULLY;
		return this;
	}
	public HttpUtil setHandler(Handler handler, int what) {
		this.handler = handler;
		this.messageWhat = what;
		return this;
	}
	
	
	public void start() {
		if (mode == GET) {
			if (getThread==null || !getThread.isAlive()){
				getThread = new GetThread();
				getThread.start();
			}
		}
	}
	
	public class GetThread extends Thread{
		public void run(){
			Message m = new Message();
			HttpURLConnection conn;
			InputStream is;
			StringBuilder resultData = new StringBuilder();
			if (null!=urlStr && !"".equals(urlStr) && handler!=null) {
				try {
					URL url = new URL(urlStr);
					Log.i(TAG, "request: " + urlStr);
					conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("GET"); //GETリクエストを設定
					conn.setConnectTimeout(5000);
					conn.setReadTimeout(5000);
					
					if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {//成功した
						is = conn.getInputStream();
						InputStreamReader isr = new InputStreamReader(is);
						BufferedReader bufferReader = new BufferedReader(isr);
						String inputLine;
						while ((inputLine = bufferReader.readLine()) != null) {
							resultData.append(inputLine).append("\n");
						}
						
						m.what = messageWhat;
						m.obj = resultData.toString();
						Log.i(TAG, "receive: " + resultData.toString());
					} else {
						m.what = REQUEST_CONTENT_FAIL;
						m.obj = String.valueOf(conn.getResponseCode());
					}
					handler.sendMessage(m);
				} catch (IOException e) {
					e.printStackTrace();
					handler.sendMessage(handler.obtainMessage(REQUEST_CONTENT_FAIL, 0));
				}
			} else {
				if (handler != null) {
					handler.sendMessage(handler.obtainMessage(EMPTY_URL_OR_HANDLER, ""));
				} else {
					Log.e(TAG, "空線程Handler！");
				}
			}
		}
	}
	
	
	class PostThread extends Thread {
	}
}
