package threadingdemo

// Represents the address space of all the threads.
class Memory(
    val size: Int = Int.MaxValue,
    private var memory: MutableMap[Int, Int] = new MutableMap(),
) {
    // maps a synchronization primitive to a queue of blocked/waiting threads.
    private var threadQueues: MutableMap[Int, Set[Thread]] = new MutableMap();

    override def equals(other: Any): Boolean = other match { 
        case other: Memory => this.size == other.size && this.memory == other.memory
        case _ => false
    }

    override def toString(): String = {
        val dump = this.memory.toVector.sortBy( _._1 ) // sort by address
        return f"${"address"}%10s : contents\n" +
            dump.map( {case (addr, value) => f"0x$addr%08X : $value%-8d"} ).mkString("\n")
    }
    
    // The indexing operation. Gets memory at address
    def apply(address: Int): Int = {
        return memory.get(address).getOrElse(0);
    }

    // The indexing set operation. Set the memory at address.
    def update(address: Int, value: Int): Unit = {
        if (address < 0 || address >= size) throw new ArrayIndexOutOfBoundsException()
        memory(address) = value
    }

    // Synchronization primitives data.

    /**
     * Adds a thread to the queue for the given synchronization primitive (by address)
     * Will not place thread multiple times.
     */
    def enqueueThread(synchPrim: Int, thread: Thread): Unit = {
        if (threadQueues.get(synchPrim).isEmpty) threadQueues(synchPrim) = Set()
        threadQueues(synchPrim) += thread
    }

    /**
     * removes a thread to the queue if it exists for the given synchronization primitive (by address)
     */
    def dequeueThread(synchPrim: Int, thread: Thread): Unit = {
        if (threadQueues.get(synchPrim).isDefined) threadQueues(synchPrim) -= thread
    }

    /**
     * Clears the queue for a synchronization primitive.
     */
    def clearThreadQueue(syncPrim: Int): Unit = {
        threadQueues(syncPrim) = Set()
    }

    /**
     * Returns the thread queue for the given synchronization primitive (by address)
     */
    def getThreadQueue(synchPrim: Int): Set[Thread] = {
        return threadQueues.get(synchPrim).getOrElse(Set[Thread]())
    }

    /**
     * Gets the synchronization primitive that the thread is blocked on, or None if there isn't any.
     */ 
    def getBlockerFor(thread: Thread): Option[Int] = {
        for ((synchPrim, threads) <- threadQueues) {
            if (threads.contains(thread)) return Some(synchPrim)
        }
        return None
    }
}

// Contains the register file for a thread.
class RegisterFile() {
    private var registers: Vector[Int] = Vector.fill(32)(0)

    override def equals(other: Any): Boolean = other match { 
        case other: RegisterFile => this.registers == other.registers
        case _ => false
    }

    override def toString(): String = {
        var rtrn = for ((value, i) <- registers.zipWithIndex)
            yield f"${RegisterFile.regToStr(i)}%8s ($i%2d) : ${value}%-8d"
        return "register ( #) : contents\n" + rtrn.mkString("\n");
    }
    

    // The indexing operation. Gets a register value.
    def apply(num: Int): Int = {
        return registers(num);
    }
    
    // The indexing operation. Gets a register value.
    def apply(name: String): Int = {
        return this.apply(RegisterFile.regFromStr(name))
    }

    // The indexing set operation. Set the register.
    def update(num: Int, value: Int): Unit = {
        registers = registers.updated(num, value)
    }

    // The indexing set operation. Set the register.
    def update(name: String, value: Int): Unit = {
        this.update(RegisterFile.regFromStr(name), value)
    }
}

object RegisterFile {
    // Returns the number of a register from its MIPS name, ($v0, $t0, $sp, etc.)
    def regFromStr(name: String): Int = name match {
        case "$zero" => 0  case "$at" => 1
        case "$gp" => 28   case "$sp" => 29
        case "$fp" => 30   case "$ra" => 31
        case _ if (name.length >= 3) => {
            val digit = name.slice(2, 4).toInt
            return name(1) match {
                case 'v' => digit + 2
                case 'a' => digit + 4
                case 't' if digit < 8 => digit + 8
                case 't' if digit >= 8 => digit + 16
                case 's' => digit + 16
                case 'k' => digit + 26
                case _ => throw new IllegalArgumentException(s"Unknown register ${name}.")
            }
        }
        case _ => throw new IllegalArgumentException(s"Unknown register ${name}.")
    }

    // Returns the name of a register from its name.
    def regToStr(num: Int): String = num match {
        case 0  => "$zero"  case 1  => "$at"
        case 28 => "$gp"    case 29 => "$sp"
        case 30 => "$fp"    case 31 => "$ra"
        case _ => {
            if      (num <= 3)  s"$$v${num - 2}"
            else if (num <= 7)  s"$$a${num - 4}"
            else if (num <= 15) s"$$t${num - 8}"
            else if (num <= 23) s"$$s${num - 16}"
            else if (num <= 25) s"$$t${num - 16}"
            else if (num <= 27) s"$$k${num - 26}"
            else throw new IllegalArgumentException(s"Unknown register ${num}.")
        }
    }

}