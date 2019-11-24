package com.akka.profiles.actor;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class ReadImageActor extends UntypedAbstractActor {
	LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	private ActorRef ImageStatisticsActor;
	private ActorRef ImportServiceActor;
	private int count = 0;
	
	public ReadImageActor() {
		ImageStatisticsActor = context().actorOf(Props.create(ImageStatisticsActor.class));
		ImportServiceActor = context().actorOf(Props.create(ImportServiceActor.class));
	}
	
	@Override
	public void onReceive(Object file) throws Throwable {
		if(count>=1) {
			ImportServiceActor.tell(file, getSelf());
			context().system().terminate();
		} else {
			count++;
			ImageStatisticsActor.tell(file, getSelf());
		}
		
	}

}
