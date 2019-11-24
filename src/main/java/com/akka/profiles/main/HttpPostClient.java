package com.akka.profiles.main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

public class HttpPostClient {
	public static void main(String[] args) {
//		post("http://localhost:8080/profile");
		postImage("http://localhost:8080/profile");
	}
	
	public static void postImage(String url) {
		HttpParams params = new BasicHttpParams();
		params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
		HttpClient httpClient = new DefaultHttpClient(params);
		HttpPost httpPost = new HttpPost(url);
		try {
			MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
			String organizedfilePath = "C:/Temp/test.jpg"; // 전송할 파일 경로
			Path source = Paths.get(organizedfilePath);
			String mimeType = Files.probeContentType(source);
			
		    System.out.println(mimeType);
			entity.addPart("image", new FileBody(source.toFile(),mimeType));
//			entity.addPart("imageName", new StringBody("image Test4"));
			
			httpPost.setEntity(entity);
			
			HttpResponse response = httpClient.execute(httpPost);
			System.out.println(response.getStatusLine());
			HttpEntity resEntity = response.getEntity();
		    if (resEntity != null) {
		      System.out.println(EntityUtils.toString(resEntity));
		    }
			httpClient.getConnectionManager().shutdown();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
