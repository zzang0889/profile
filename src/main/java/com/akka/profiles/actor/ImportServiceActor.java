package com.akka.profiles.actor;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import akka.Done;
import akka.actor.UntypedAbstractActor;

public class ImportServiceActor extends UntypedAbstractActor {
	private static SqlSessionFactory sqlSessionFactory;
	
	@Override
	public void onReceive(Object message) throws Throwable {
		// TODO Auto-generated method stub
		saveProfile((HashMap<String, Object>) message);
	}
	
	  // (fake) async database query api
	  private CompletionStage<Done> saveProfile(HashMap<String, Object> profile) throws IOException {
//			  HashMap<String, Object> profileTest = new HashMap<String, Object>();
			  InputStream is = null;
			  SqlSession session = null;
//			  profile.put("imageName", "test");
//			  profile.put("imageWidth", 1);
//			  profile.put("imageHeight", 2);
//			  profile.put("imageSdate", null);
//			  profile.put("maxR", 55);
//			  profile.put("maxG", 55);
//			  profile.put("maxB", 55);
//			  profile.put("minR", 22);
//			  profile.put("minG", 22);
//			  profile.put("minB", 22);
//			  profile.put("averageR", 22);
//			  profile.put("averageG", 22);
//			  profile.put("averageB", 22);
//			  profile.put("histogramR", 33);
//			  profile.put("histogramG", 33);
//			  profile.put("histogramB", 33);
			  System.out.println(profile);
			  try {
				  is = Resources.getResourceAsStream("config/myBitisConfig.xml");
				  sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
				  session = sqlSessionFactory.openSession();
				  session.insert("profile.insertProfile", profile);
				  session.commit();
			  } catch (IOException e) {
				  // TODO Auto-generated catch block
				  e.printStackTrace();
			  } finally {
				  session.close();
				  is.close();
			  }
			return CompletableFuture.completedFuture(Done.getInstance());
	  }

}
