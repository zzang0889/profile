package com.akka.profiles.main;


import static akka.http.javadsl.server.PathMatchers.longSegment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Flow;

public class HttpServerProfile extends AllDirectives {
	private static SqlSessionFactory sqlSessionFactory;
	static ActorSystem system = ActorSystem.create("routes");
	final static Http http = Http.get(system);
    final static ActorMaterializer materializer = ActorMaterializer.create(system);

    public interface IDao {
    	public void insertProfile(Map map);
    	}
  public static void main(String[] args) throws Exception {
    // boot up server using the route as defined below
//    ActorSystem system = ActorSystem.create("routes");
//
//    final Http http = Http.get(system);
//    final ActorMaterializer materializer = ActorMaterializer.create(system);

    //In order to access all directives we need an instance where the routes are define.
	  HttpServerProfile app = new HttpServerProfile();

    final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute().flow(system, materializer);
    final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
      ConnectHttp.toHost("localhost", 8080), materializer);

    System.out.println("Server online at http://localhost:8080/\nPress RETURN to stop...");
    System.in.read(); // let it run until user presses return

    binding
      .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
      .thenAccept(unbound -> system.terminate()); // and shutdown when done
  }

  // (fake) async database query api
  private CompletableFuture<Optional<List<Map<String, Object>>>> fetchProfile() {
	  List<Map<String, Object>> profileList = null;
	  InputStream is = null;
	  SqlSession session = null;
		try {
			is = Resources.getResourceAsStream("config/myBitisConfig.xml");
			sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
			session = sqlSessionFactory.openSession();
			profileList = session.selectList("profile.getProfile");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			session.close();
		}
    return CompletableFuture.completedFuture(Optional.of(profileList));
  }
  
  private CompletableFuture<Optional<List<Map<String, Object>>>> fetchProfileOne(long seq) {
	  List<Map<String, Object>> profile = null;
	  InputStream is = null;
	  SqlSession session = null;
	  try {
		  is = Resources.getResourceAsStream("config/myBitisConfig.xml");
		  sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
		  session = sqlSessionFactory.openSession();
		  profile = session.selectList("profile.getProfileOne",seq);
	  } catch (IOException e) {
		  // TODO Auto-generated catch block
		  e.printStackTrace();
	  } finally {
		  session.close();
	  }
	  return CompletableFuture.completedFuture(Optional.of(profile));
  }

  // (fake) async database query api
  private CompletionStage<Done> saveProfile() {
		  HashMap<String, Object> profile = new HashMap<String, Object>();
		  InputStream is = null;
		  SqlSession session = null;
		  profile.put("imageName", "test");
		  profile.put("imageWidth", 1);
		  profile.put("imageHeight", 2);
		  profile.put("imageSdate", LocalDate.now());
		  profile.put("imageMax", 55);
		  profile.put("imageMin", 22);
		  profile.put("imageAvg", 33);
		  
		  try {
			  is = Resources.getResourceAsStream("config/myBitisConfig.xml");
			  sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
			  session = sqlSessionFactory.openSession();
			  int suss = session.insert("profile.insertProfile", profile);
			  session.commit();
		  } catch (IOException e) {
			  // TODO Auto-generated catch block
			  e.printStackTrace();
		  } finally {
			  session.close();
		  }
		return CompletableFuture.completedFuture(Done.getInstance());
  }

  

  private Route createRoute() {
    return concat(
//    GET /profile 리스트 조회
      get(() ->
          path("profile", () -> {
            final CompletableFuture<Optional<List<Map<String, Object>>>> profileList = fetchProfile();
            return onSuccess(profileList, maybeItem ->
              maybeItem.map(item -> completeOK(item, Jackson.marshaller()))
                .orElseGet(() -> complete(StatusCodes.NOT_FOUND, "Not Found"))
            );
          })),
      get(() ->
      pathPrefix("profile", () ->
      path(longSegment(), (Long id) -> {
    	  final CompletableFuture<Optional<List<Map<String, Object>>>> profileOne = fetchProfileOne(id);
    	  return onSuccess(profileOne, maybeItem ->
    	  maybeItem.map(item -> completeOK(item, Jackson.marshaller()))
    	  .orElseGet(() -> complete(StatusCodes.NOT_FOUND, "Not Found"))
    			  );
      }))),
      post(() ->
      path("profile", () -> 
      entity(Unmarshaller.entityToMultipartFormData(), formData -> {
//        	  System.out.println(formData.getParts());
    	  final CompletionStage<HashMap<String, Object>> allParts; 
    	  allParts = formData.getParts().mapAsync(1, bodypart->{
    		  final File file = File.createTempFile("upload", "tmp");
    		  return bodypart.getEntity().getDataBytes()
    				  .runWith(FileIO.toPath(file.toPath()), materializer)
    				  .thenApply(ignore ->
    				  new Pair<String, Object>(bodypart.getName(), file)
    						  );
    		  
    	  }).runFold(new HashMap<String, Object>(), (acc, pair) -> {
    		  acc.put(pair.first(), pair.second());
    		  return acc;
    	  }, materializer);;
    	  System.out.println(allParts.toString());
    	  saveProfile();
    	  return onSuccess(CompletableFuture.completedFuture(Done.getInstance()), done ->
    	  complete("order created")
    			  );
      })))
      
    );
  }

}