package com.akka.profiles.actor;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

public class Main {
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ActorSystem actorSystem = ActorSystem.create("ImageProcess");
        ActorRef ReadImageActor = actorSystem.actorOf(Props.create(ReadImageActor.class), "ReadImageActor");
        String organizedfilePath = "C:/Temp/test4.jpg"; // 전송할 파일 경로
		Path source = Paths.get(organizedfilePath);
		File file = source.toFile();
		
		ReadImageActor.tell(file, ActorRef.noSender());
	}

}
