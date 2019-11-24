package com.akka.profiles.actor;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import akka.Done;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class ImportServiceActor extends UntypedAbstractActor {
	private static SqlSessionFactory sqlSessionFactory;
	LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	@Override
	public void onReceive(Object message) throws Throwable {
		saveProfile((HashMap<String, Object>) message);
	}
	
	  private CompletionStage<Done> saveProfile(HashMap<String, Object> profile) throws IOException {
			  InputStream is = null;
			  SqlSession session = null;
			  try {
				  log.info("import {}", profile);
				  is = Resources.getResourceAsStream("config/myBitisConfig.xml");
				  sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
				  session = sqlSessionFactory.openSession();
				  session.insert("profile.insertProfile", profile);
				  session.commit();
			  } catch (IOException e) {
				  e.printStackTrace();
			  } finally {
				  session.close();
				  is.close();
			  }
			return CompletableFuture.completedFuture(Done.getInstance());
	  }

}
