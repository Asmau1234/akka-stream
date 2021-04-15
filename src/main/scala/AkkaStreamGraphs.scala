import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ClosedShape
import akka.stream.scaladsl.GraphDSL.Implicits.{SourceShapeArrow, port2flow}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, Zip}

object AkkaStreamGraphs {

  implicit val system = ActorSystem() // "thread pool"

  // a source element that "emits" integers
  val source = Source(1 to 1000)
  // a flow that receives integers, transforms them and passes their results further down
  val flow = Flow[Int].map(x => x * 2)
  // a sink that receives integers and prints each to the console: receiver
  val sink = Sink.foreach[Int](println)
  // combine all components together in a static graph
  val graph = source.via(flow).to(sink)

  //source of ints -> 2 independent "hard" computations -> stitch the results in a tuple -> print the tuples to the console

  //step 1 - the frame
  //this is a curryed function with two arguments create and builder
  //builder is a mutable data structure
  val specialGraph = GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>


    //step 2 - create the building blocks
    val input = builder.add(Source(1 to 1000))
    val incrementer = builder.add(Flow[Int].map(_ + 1)) //"hard" computation #1
    val multiplier = builder.add(Flow[Int].map(_ * 10)) //"hard" computation #2
    val output = builder.add(Sink.foreach[(Int, Int)](println))

    //non-standard components
    //broadcast will take a single input and it will duplicate every single element into the outputs
    val broadcast = builder.add(Broadcast[Int](2))
    val zip = builder.add(Zip[Int, Int])

    //step 3 - glue the components together
    //input feeds into broadcast meaning output of source will be fed into input of broadcast
    input ~> broadcast
    broadcast.out(0) ~> incrementer ~> zip.in0
    broadcast.out(1) ~> multiplier ~> zip.in1

    zip.out ~> output

    //step 4 - closing
    //singleton object which is a marker to instantiate graph
    ClosedShape
  }

  def main(args: Array[String]): Unit = {
    // start the graph = "materialize" it
    //graph.run()
    //we are passing an akka stream here
    RunnableGraph.fromGraph(specialGraph).run()
  }

}
