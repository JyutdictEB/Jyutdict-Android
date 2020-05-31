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
	public static final int POSTING_CONTENT_NOTIFY = 9202;
	public static final int POSTED_CONTENT_MAIN_PART = 9203;
	public static final int REQUEST_CONTENT_CANCELED = 9204;
	
	
	private String urlStr = "";
	private Handler handler;
	private GetThread getThread;
	private PostThread postThread;
	private Boolean mode; // False For GET, True For POST
	private Boolean disconnect = false;
	
	int messageWhat;
	
	
	public HttpUtil(Boolean mode) {
		this.mode = mode;
	}
	
	public void setUrl(String urlStr) {
		this.urlStr = urlStr;
		
	}
	public void setHandler(Handler handler) {
		this.handler = handler;
		messageWhat = REQUEST_CONTENT_SUCCESSFULLY;
	}
	public void setHandler(Handler handler, int what) {
		this.handler = handler;
		this.messageWhat = what;
	}
	
	public void setDisconnect () {
		disconnect = true;
		if (mode && postThread!=null && postThread.conn != null) {
			postThread.conn.disconnect();
		}
	}
	
	public void start() {
		if (mode == GET) {
			if (getThread==null || !getThread.isAlive()){
				getThread = new GetThread();
				getThread.start();
				Log.d(TAG, "GetThreadStart");
			}
		} else {
			if (postThread==null || !postThread.isAlive()){
				postThread = new PostThread();
			}
			if (postThread.set) {
				postThread.start();
				Log.d(TAG, "PostThreadStart");
			} else {
				Log.e(TAG, "PostThread Unset Yet");
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
						Log.i(TAG, "run: " + urlStr);
						Log.i(TAG, "run: " + resultData.toString());
					} else {
						m.what = REQUEST_CONTENT_FAIL;
						m.obj = String.valueOf(conn.getResponseCode());
					}
					handler.sendMessage(m);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				if (handler != null) {
					m.what = EMPTY_URL_OR_HANDLER;
					m.obj = "";
					handler.sendMessage(m);
				} else {
					Log.e(TAG, "空線程Handler！");
				}
			}
		}
	}
	
	
	class PostThread extends Thread {
		private static final int TIME_OUT = 10; //超時
		private static final String CHARSET = "utf-8";
		private static final String BOUNDARY = "FlPm4LpSXsE"; //UUID.randomUUID().toString();
		private static final String PREFIX = "--";
		//private final String LINE_END = System.getProperty("line.separator");
		private static final String LINE_END = "\r\n";
		private static final String CONTENT_TYPE = "multipart/form-data";
		
		String[] videoInfo;
		String videoExtra;
		String previewPath;
		Boolean set = false;
		HttpURLConnection conn;
		
		
		public void run(){
			Message m = new Message();
			InputStream is;
			StringBuilder resultData = new StringBuilder();
			StringBuffer sb;
			
		}
	}
}
