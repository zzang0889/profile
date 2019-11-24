package com.akka.profiles.main;


import static akka.http.javadsl.server.PathMatchers.longSegment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import javax.naming.Context;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import com.akka.profiles.actor.ReadImageActor;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.marshalling.Marshaller;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.Multipart.FormData;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Directives;
import akka.http.javadsl.server.RequestContext;
import akka.http.javadsl.server.Route;
import akka.http.javadsl.server.directives.RouteAdapter;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import akka.japi.Pair;
import akka.stream.Materializer;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Flow;

public class HttpServerProfile extends AllDirectives {
	private static SqlSessionFactory sqlSessionFactory;
	static ActorSystem system = ActorSystem.create("routes");
	final static Materializer materializer = Materializer.createMaterializer(system);
	
  public static void main(String[] args) throws Exception {
    // boot up server using the route as defined below
    final Http http = Http.get(system);
    
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
  

  private Route createRoute() {
    Unmarshaller.requestToEntity();
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
      })))
      ,
      post(() ->
      path("profile", () -> 
          entity(Unmarshaller.entityToMultipartFormData(), requestBody  -> {
    	  CompletionStage<String> after = importImageProfileFromRequest(requestBody);
    	  return onSuccess(after, done -> complete("order created"));
      })))
    );
  }

  
  protected CompletionStage<String> importImageProfileFromRequest(FormData requestFile) {
		  CompletionStage<HashMap<String, Object>> filesFuture  = requestFile.getParts().mapAsync(1, bodyPart->{
					  if ("image".equals(bodyPart.getName())) {
			          // stream into a file as the chunks of it arrives and return a CompletionStage
			          // file to where it got stored
			          final File file = File.createTempFile("upload", "tmp");
			          return bodyPart.getEntity().getDataBytes()
			            .runWith(FileIO.toPath(file.toPath()), materializer)
			            .thenApply(ignore ->
			              new Pair<String, Object>(bodyPart.getName(), file)
			            );
			        } else {
			          // collect form field values
			          return bodyPart.toStrict(2 * 1000, materializer)
			            .thenApply(strict ->
			              new Pair<String, Object>(bodyPart.getName(),
			                strict.getEntity().getData().utf8String())
			            );
			        }
				}).runFold(new HashMap<String, Object>(), (acc, pair) -> {
			        acc.put(pair.first(), pair.second());
			        return acc;
			      }, materializer);

		return filesFuture.thenApply(files -> {
			try {
				ActorSystem actorSystem = ActorSystem.create("ImageProcess");
				ActorRef ReadImageActor = actorSystem.actorOf(Props.create(ReadImageActor.class), "ReadImageActor");
				ReadImageActor.tell(files, ActorRef.noSender());
			} catch (Exception e) {
				// TODO: handle exception
				throw new RuntimeException("Failed to read the image from bytes.", e);
			}
//			
			return "ok";
		});
	}

}