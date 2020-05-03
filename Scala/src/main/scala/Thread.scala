package threadingdemo
import Thread.State

// A thread is a sequence of statements.
class Thread private(
    val code: String,
    val ast: Seq[RavelStatement],
    val assembly: Seq[Instruction],
    val memory: Memory = new Memory(),
    val bindings: Bindings,
    val sp: Int // starting point of stack pointer. Defaults to top of the memory.
) {
    private var _pc: Int = 0
    val registers: RegisterFile = new RegisterFile()
    this.registers("$sp") = sp
    this.registers("$fp") = sp
    
    private var _locksHeld: Set[Int] = Set(); // contains a list of addresses of locks held by this thread.

    // Getters
    def pc = _pc
    def state: State.Value = {
        if (_pc >= assembly.length) State.Complete
        else if (memory.getBlockerFor(this).isDefined) State.Blocked
        else State.Running
    }
    def currentInstr = assembly(_pc) // returns the current instruction.
    def locksHeld = _locksHeld // returns the current instruction.

    /**
      * Returns an iterator of all the current variables and their current values and types in the thread.
      * as (name, value, type) tuples
      */
    def variables: Iterator[(String, Int, RavelType.Value)] = {
        return for ((id, loc, addr, typ) <- bindings.iterator) yield 
            if (loc == VarLoc.Register) {
                (id, this.registers(addr), typ)
            } else {
                (id, this.memory(addr.toInt), typ)
            }
    }

    /**
     * Returns the value for the given variable at the current point in exectution.
     */
    def getVar(id: String): Int = {
        return this.getVarWithType(id)._1
    }

    /**
     * Returns the value and type for the given variable at the current point in exectution.
     */
    def getVarWithType(id: String): (Int, RavelType.Value) = {
        return bindings.findWithType(id) match {
            case (VarLoc.Register, reg, typ) => (registers(reg), typ)
            case (VarLoc.Stack, offset, typ) => (memory(registers("$fp") + offset.toInt), typ)
            case (VarLoc.Heap,    addr, typ) => (memory(addr.toInt), typ)
        }
    }

    /**
     * Takes a VarLock and and an reg|address|offset and returns a (optional name, value, type) tuple.
     * The address may not be bound to a name, in that case name will be None, and type defaults to Int.
     */
    def getVarFromAddr(loc: VarLoc, addr: String): (Option[String], Int, RavelType.Value) = {
        val value = loc match {
            case VarLoc.Register => registers(addr)
            case VarLoc.Stack    => memory(registers("$fp") + addr.toInt)
            case VarLoc.Heap     => memory(addr.toInt)
        }
        val definition = bindings.reverseFind(loc, addr)
        if (definition.isDefined) {
            val (name, typ) = definition.get
            return (Some(name), value, typ)
        } else {
            return (None, value, RavelType.Int)
        }
    }
   
    /**
     * executes one instruction. wakeThread is the thread to wake, if a selection from
     * the user was required. 
     */
    private def execInstr(instr: Instruction, wakeThread: Thread = null): Unit = {
        // Returns the value of the assembly param, either a reg or an immediate value
        def get(param: String) = if (Instruction.isReg(param)) registers(param) else param.toInt

        instr match {
            case Instruction("lw", dest, src, offset) =>
                registers(dest) = memory(registers(src) + offset.toInt)
            case Instruction("sw", src, dest, offset) =>
                memory(registers(dest) + offset.toInt) = registers(src)
            case Instruction("li", dest, num, null) =>
                registers(dest) = num.toInt

            case Instruction("add", dest, a, b) => 
                registers(dest) = get(a) + get(b)
            case Instruction("sub", dest, a, b) =>
                registers(dest) = get(a) - get(b)
            case Instruction("mult", dest, a, b) =>
                registers(dest) = get(a) * get(b)
            case Instruction("div", dest, a, b) =>
                registers(dest) = get(a) / get(b)
            case Instruction("mod", dest, a, b) =>
                registers(dest) = get(a) % get(b)

            case Instruction("move", dest, src, null) =>
                registers(dest) = registers(src)

            case Instruction("seq", dest, a, b) =>
                registers(dest) = if ( get(a) == get(b) ) 1 else 0
            case Instruction("sne", dest, a, b) =>
                registers(dest) = if ( get(a) != get(b) ) 1 else 0
            case Instruction("slt", dest, a, b) =>
                registers(dest) = if ( get(a) < get(b) ) 1 else 0
            case Instruction("sgt", dest, a, b) =>
                registers(dest) = if ( get(a) > get(b) ) 1 else 0
            case Instruction("sle", dest, a, b) =>
                registers(dest) = if ( get(a) <= get(b) ) 1 else 0
            case Instruction("sge", dest, a, b) =>
                registers(dest) = if ( get(a) >= get(b) ) 1 else 0

            case Instruction("beq", a, b, offset) =>
                if ( get(a) == get(b) ) _pc += offset.toInt
            case Instruction("bne", a, b, offset) =>
                if ( get(a) != get(b) ) _pc += offset.toInt
            case Instruction("j", offset, null, null) => // we're using offsets, unlike MIPS which uses absolute addresses
                _pc += offset.toInt

            case Instruction("lock", addr, null, null) => {
                if (memory(addr.toInt) == 0) { // lock open
                    memory(addr.toInt) = 1
                    _locksHeld += addr.toInt
                } else {
                    memory.enqueueThread(addr.toInt, this)
                }
            }
            case Instruction("unlock", addr, null, null) => {
                memory(addr.toInt) = 0; // Release the lock.
                _locksHeld -= addr.toInt
                memory.clearThreadQueue(addr.toInt) // unblock everything for lock
            }

            case Instruction("cond_wait", addr, null, null) => {
                if (memory(addr.toInt) == 0) { // condition variable not set
                    memory.enqueueThread(addr.toInt, this)
                }
            }
            case Instruction("cond_signal", addr, null, null) =>
                memory(addr.toInt) = 1 // set condition variable
                memory.clearThreadQueue(addr.toInt) // unblock everything for cond_var

            case Instruction("sem_wait", addr, null, null) => {
                if ( !memory.getThreadQueue(addr.toInt).contains(this) ) {
                    memory(addr.toInt) -= 1 // don't decrement multiple times.
                    if (memory(addr.toInt) < 0) memory.enqueueThread(addr.toInt, this) // block thread.
                }
            }
            case Instruction("sem_post", addr, null, null) => {
                memory(addr.toInt) += 1

                if (memory(addr.toInt) < 1) { // if we have a waiting thread.
                    // we have to have the user choose a thread to wake. We don't have to do this for locks and cond_vars
                    // because for those we can just use a "spin wait" system where it checks every time we try to step.
                    // but that doesn't work with semaphores.
                    if ( wakeThread != null && this.inputOptions.contains(wakeThread) ) {
                        memory.dequeueThread(addr.toInt, wakeThread)
                        wakeThread._pc += 1; // step the other thread past the sem_wait.
                    } else {
                        throw new Exception("sem_post requires a wakeThread to be specified from the inputOptions set.")
                    }
                }
            }
            case _ => throw new IllegalArgumentException(s"Unknown instruction '${instr}'")
        }
    }

    /**
     * Preforms one step in the execution of this thread, if possible. It returns the new state of the thread. One of
     * Complete, Running, Blocked (the thread is blocked on a lock etc.) wakeThread is the thread selected by the 
     * user to wake. Only use it when inputOptions is non-empty, and choose the thread from inputOptions.
     */
    def step(wakeThread: Thread = null): State.Value = {
        if (this.state == State.Running) {
            execInstr(this.currentInstr, wakeThread)
            if (this.state == State.Running) // if we are still running, move to next instruction
                _pc += 1
        }
    
        return this.state
    }

    // Executes the thread until the end, or the thread is blocked, or user input is required.
    def execute(): Unit = {
        while (this.inputOptions.isEmpty && this.step() == State.Running) {}
    }

    /**
     * Returns the threads that the user needs to choose from before this instruction is executed.
     * Returns empty set if there are none.
     */
    def inputOptions: Set[Thread] = {
        if (this.state == State.Running && currentInstr.instr == "sem_post") {
            return memory.getThreadQueue(currentInstr.a.toInt)
        } else {
            return Set()
        }
    }
}

object Thread {
    /**
     * Factory method, creates a thread. sp is the location of the stack pointer within memory,
     * defaults to the top of the memory.
     */
    def compileFrom(code: String, memory: Memory = new Memory(),
                    globals: MutableMap[String, AddrType] = new MutableMap(), sp: Int = -1): Thread = {
        return create(code, memory, new Bindings(globals), sp, false)
    }

    /**
     * Factory method, creates a "globals" thread (ie all variables will be stored on "heap")
     * globals is the globals portion of the global thread's bindings.
     */
    def compileGlobalFrom(code: String, memory: Memory = new Memory(), sp: Int = -1): Thread = {
        return create(code, memory, new Bindings(), sp, true)
    }

    // Helper creation method.
    private def create(code: String, memory: Memory, bindings: Bindings, sp: Int, global: Boolean): Thread = {
        val ast = Parser.parse(code)
        val assembly = Parser.compile(ast, bindings, global)
        val newSp = if (sp < 0) memory.size - 1 else sp

        return new Thread(code, ast, assembly, memory, bindings, newSp)
    }

    /**
     * State of the thread. Running, Blocked on some synchronization primitive, or Complete if the thread is
     * has reached the end. 
     */
    object State extends Enumeration {
        val Running, Blocked, Complete = Value
    }
}