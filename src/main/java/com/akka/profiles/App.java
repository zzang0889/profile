package com.akka.profiles;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

/**
 * Hello world!
 *
 */

public class App {

	private static SqlSessionFactory sqlSessionFactory;

	public static void main(String[] args) {
		InputStream is = null;
		try {
			is = Resources.getResourceAsStream("config/myBitisConfig.xml");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);

		System.out.println("Hello World!");
		SqlSession session = sqlSessionFactory.openSession();
		List<Map<String, Object>> test = session.selectList("test.getTest");
		
		System.out.println("Player Name: " + test.toString());
		session.close();
	}
}
