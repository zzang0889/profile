package com.akka.profiles.main;

import java.io.File;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

public class HttpPostClient {
	public static void main(String[] args) {
		post("http://localhost:8080/profile");
	}
	
	public static void post(String url) {
	    HttpClient httpClient = new DefaultHttpClient();
	    HttpContext localContext = new BasicHttpContext();
	    HttpPost httpPost = new HttpPost(url);

	    try {
	        MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
	        String organizedfilePath = "C:/Temp/test.jpg"; // 전송할 파일 경로
	        File file1 = new File(organizedfilePath); // 파일을 생성해줍니다.
			ContentBody cbFile = new FileBody(file1);
	        entity.addPart("imageTest", cbFile);
	        httpPost.setEntity(entity);
	        
	        HttpResponse response = httpClient.execute(httpPost,localContext);
	        System.out.println(response.getStatusLine());
	        
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
}
