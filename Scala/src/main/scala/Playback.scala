package threadingdemo

import scala.collection.mutable.Buffer;
import scala.collection.mutable.ArrayBuffer;

// Represents a playback
class Playback(
    var name: String = "",
) extends Iterable[Step] {
    private val steps: Buffer[Step] = new ArrayBuffer()
    /**
     * Returns an iterator of Steps
     */
    override def iterator = steps.iterator

    def add(threadPos: Int, wakeThread: Int = -1, breakpoint: Boolean = false): Unit = {
        steps.addOne( Step(threadPos, if (wakeThread < 0) None else Some(wakeThread), breakpoint) )
    }

    /*
     * Adds a breakpoint to the Playback so that execution will pause.
     * Just sets the last step's breakpoint to true.
     */
    def addBreakpoint(): Unit = {
        if (steps.nonEmpty) steps.last.breakpoint = true;
    }
}

/**
 * Stores one step. Thread is the thread to step, wakeThread is the wakeThread parameter, if its needed,
 * breakpoint is whether this step marks a breakpoint. Execution will pause after completing the step marked
 * with a breakpoint.
 */
case class Step(thread: Int, wakeThread: Option[Int], var breakpoint: Boolean);

object Playback extends Enumeration {
    val NotPlaying, Playing, Paused, Recording = Value;
}