package threadingdemo

import scala.scalajs.js // Required to get parsed version of the code from nearely library.
import scala.scalajs.js.Dynamic.global

// Functions for parsing and compiling code.
object Parser {
    type Source = ((Int, Int), (Int, Int)); // Stores the location of the text that a statement compiled from

    // Parses the code and returns a list of statements.
    def parse(code: String): Seq[RavelStatement] = {
        var grammar = global.nearley.Grammar.fromCompiled(global.grammar)
        var parser = js.Dynamic.newInstance( global.nearley.Parser )(grammar);
        parser.feed(code)
        var results = parser.results.asInstanceOf[js.Array[js.Dynamic]]

        if (results.length < 1) {
            throw new Exception(s"Unexpected end of input. Did you miss a ';'?")
        } else if (results.length > 1) {
            println("Code was ambiguous. Got:")
            global.console.log(results) // console.log print allows us to see internals of the objects
            throw new Exception(s"Code is ambiguous.")
        }

        return transJsAll(results(0)) // convert the javascript objects into scala objects.
    }

    // Compiles a list of seq statements.
    def compile(statements: Seq[RavelStatement], bindings: Bindings, globals: Boolean): Seq[Instruction] = {
        var compiled = Vector[Instruction]()
        for (statement <- statements) {
            compiled = compiled ++ statement.compile(bindings, globals)
        }
        return compiled;
    }

    // Compiles a program
    def compile(code: String, bindings: Bindings, globals: Boolean): Seq[Instruction] = {
        return Parser.compile(Parser.parse(code), bindings, globals)
    }

    // casts ast to an array of Scala objects
    private def transJsAll(ast: js.Dynamic) : Seq[RavelStatement] = {
        val castAst = ast.asInstanceOf[js.Array[js.Dynamic]]
        return for (i <- 0 until castAst.length) yield transJs( castAst(i) )
    }

    // translates the json object that the nearley grammar returns into a Scala object.
    private def transJs(ast: js.Dynamic) : RavelStatement = {
        if (ast == null) return null;

        val typeStr = ast.selectDynamic("type").asInstanceOf[String]
        val srcArr = ast.src.asInstanceOf[js.Array[js.Array[Int]]]
        val src = ((srcArr(0)(0), srcArr(0)(1)), (srcArr(1)(0), srcArr(1)(1)))

        return typeStr match {
            case "if" =>
                new RavelIf(transJsExpr(ast.cond), transJsAll(ast.ifCase), transJsAll(ast.elseCase), src)
            case "while" =>
                new RavelWhile(transJsExpr(ast.cond), transJsAll(ast.body), src)
            case "for" =>
                new RavelFor(transJsExpr(ast.init).asInstanceOf[RavelInit], transJsExpr(ast.cond),
                    transJsExpr(ast.inc), transJsAll(ast.body), src)
            case "lock" | "unlock" | "cond_wait" | "cond_signal" | "sem_wait" | "sem_post" =>
                new RavelProcedure(typeStr, transJsExpr(ast.param).asInstanceOf[RavelId], src)
            case "init" =>
                new RavelInit(RavelType.fromString(ast.varType.asInstanceOf[String]),
                    ast.name.asInstanceOf[String], transJsExpr(ast.rhs), src)
            case "=" =>
                new RavelAssign(ast.name.asInstanceOf[String], transJsExpr(ast.rhs), src)
            case "+=" | "-=" | "*=" | "/=" | "%=" => {
                val name = ast.name.asInstanceOf[String]
                new RavelAssign(name,
                    new RavelBinaryOp(typeStr.slice(0, 1), new RavelId(name, src), transJsExpr(ast.rhs), src),
                src)
            }
            case "sem_create" =>
                new RavelFunc(typeStr, transJsExpr(ast.param), src)
            case "||" | "&&" | "==" | "!=" | "<" | ">" | "<=" | ">=" | "+" | "-" | "*" | "/" | "%" =>
                new RavelBinaryOp(typeStr, transJsExpr(ast.a), transJsExpr(ast.b), src)
            case "unary+" =>
                new RavelUnaryOp("+", transJsExpr(ast.a), src)
            case "unary-" =>
                new RavelUnaryOp("-", transJsExpr(ast.a), src)
            case "!" => 
                new RavelUnaryOp(typeStr, transJsExpr(ast.a), src)
            case "++" | "--" => {
                val name = ast.name.asInstanceOf[String]
                new RavelAssign(name,
                    new RavelBinaryOp(typeStr.slice(0, 1), new RavelId(name, src), new RavelInt(1, src), src),
                src)
            }
            case "id" =>
                new RavelId(ast.name.asInstanceOf[String], src)
            case "int" =>
                new RavelInt(ast.value.asInstanceOf[Int], src)
            case "bool" =>
                new RavelBool(ast.value.asInstanceOf[Boolean], src)
        }
    }

    // casts the result of transJs to a RavelExpr
    private def transJsExpr(ast: js.Dynamic) : RavelExpr = {
        return transJs(ast).asInstanceOf[RavelExpr]
    }
}

/**
 * Combines an address and a variable type.
 */
case class AddrType(addr: Int, typ: RavelType.Value)

 // An "enum", but regular Enums cause a "not exhaustive" warning when pattern matching.
sealed trait VarLoc
object VarLoc { // whether a variable resides in heap, stack, or a register.
    sealed trait Local extends VarLoc

    case object Heap extends VarLoc
    case object Stack extends VarLoc.Local
    case object Register extends VarLoc.Local
}

// Used to keep track of variables during compilation.
class Bindings(
    val globals: MutableMap[String, AddrType] = new MutableMap()
) extends Iterable[(String, VarLoc, String, RavelType.Value)]{
    val locals: MutableMap[String, AddrType] = new MutableMap()
    val registers: MutableMap[String, AddrType] = new MutableMap() // keeps tracks of registers.
    private var anonymousVarCount = 0;


    /**
     * Finds whether the given variable is in a register, on the stack, or a global.
     * Returns a tuple of the (Register|Stack|Heap, address|offset|regname). Does not
     * return the type information of the variable.
     */ 
    def find(id: String): (VarLoc, String) = {
        val (loc, addr, typ) = this.findWithType(id)
        return (loc, addr)
    }

    /**
     * Finds whether the given variable is in a register, on the stack, or a global.
     * Returns a tuple of the (Register|Stack|Heap, address|offset|regname, RavelType)
     */
    def findWithType(id: String): (VarLoc, String, RavelType.Value) = {
        if (registers.get(id).isDefined) {
            return Bindings.unpack(VarLoc.Register,registers(id))
        } else if (locals.get(id).isDefined) {
            return Bindings.unpack(VarLoc.Stack, locals(id))
        } else if (globals.get(id).isDefined) {
            return Bindings.unpack(VarLoc.Heap, globals(id))
        } else {
            throw new NoSuchElementException(s"'$id' doesn't exist in bindings.")
        }
    }

    /**
     * Takes a VarLoc type and a address|offset|regname string and optionally returns the varname
     * associated with that address and the type
     */
    def reverseFind(loc: VarLoc, addr: String): Option[(String, RavelType.Value)] = {
        val binding = loc match {
            case VarLoc.Register => registers.find( (p) => RegisterFile.regToStr(p._2.addr) == addr )
            case VarLoc.Stack    => locals.find( _._2.addr == addr.toInt )
            case VarLoc.Heap     => globals.find( _._2.addr == addr.toInt )
        }
        return binding.map( (p) => (p._1, p._2.typ) )
    }

    /**
     * Finds a free register, or a free slot in local memory if all the registers are taken, and
     * places the variable there.
     * Returns a tuple of (locType, address)
     */
    def bindLocal(id: String, typ: RavelType.Value): (VarLoc.Local, String) = {
        if (registers.get(id).isDefined || locals.get(id).isDefined) {
            throw new Exception(s"Can't bind local variable $id twice.")
        }

        val reg = this.freeReg()
        if (reg.isDefined) {
            registers(id) = AddrType(reg.get, typ)
            return (VarLoc.Register,  RegisterFile.regToStr(reg.get))
        } else { // place in locals
            locals(id) = AddrType(this.freeStack(), typ)
            return (VarLoc.Stack, s"${locals(id).addr}")
        }
    }

    // Optionally returns the name of a free general purpose register. (a "t" or "s" register.)
    private def freeReg(): Option[Int] = {
        for (reg <- 8 to 25) {
            if (registers.find((pair) => pair._2.addr == reg).isEmpty)
                return Some(reg)
        }
        return None
    }

    // Returns the address of the next free slot of local memory, relative to $sp/$fp, as a string
    private def freeStack(): Int = {
        val sorted = locals.toVector.sortBy((pair) => pair._2.addr)(Ordering.Int.reverse) // stack grows down.
        var prev = 4 // start at 4 so that it will return 0 on empty locals. The stack grows towards negative.
        for ( (id, addrType) <- sorted ) {
            if (addrType.addr < prev - 4) { // a gap in the address space
                return prev - 4
            } else {
                prev = addrType.addr;
            }
        }
        return prev - 4;
    }

    // Places the variable in the next free slot of global memory, and returns
    // the address as a string
    def bindGlobal(id: String, typ: RavelType.Value): String = {
        if (globals.get(id).isDefined) {
            throw new Exception(s"Can't bind global variable $id twice.")
        }

        val addr = if (globals.size == 0) 0 else globals.values.maxBy(_.addr).addr + 4
        globals(id) = AddrType(addr, typ)
        return s"$addr"
    }

    /**
      * Remove the id from local memory or the register file.
      */
    def unbindLocal(id: String): Unit = {
        if (registers.get(id).isDefined) {
            registers.remove(id)
        } else { // must be in locals
            locals.remove(id)
        }
    } 

    /**
      * Returns a unique name for a complier made variable.
      */
    def uniqueName(): String = {
        val name = s"$$temp${anonymousVarCount}"
        anonymousVarCount += 1
        return name;
    }

    /**
     * Iterates through all the bindings returning a (name, VarLoc, addr, RavelType) tuple for each.
     * Iterates through globals then locals then registers.
     */
    override def iterator: Iterator[(String, VarLoc, String, RavelType.Value)] = {
        def conv(loc: VarLoc)(keyVal: (String, AddrType)): (String, VarLoc, String, RavelType.Value)  = {
            val (id, addrType) = keyVal
            val (_, addr, typ) = Bindings.unpack(loc, addrType)
            return (id, loc, addr, typ)
        }

        return globals.iterator.map(conv(VarLoc.Heap)) ++ locals.iterator.map(conv(VarLoc.Stack)) ++
               registers.iterator.map(conv(VarLoc.Register))
    }
}

object Bindings {
    /**
     * Takes a VarLock and AddrType and returns a (VarLock, String, RavelType.Value) tuple
     */
    private def unpack(loc: VarLoc, addrType: AddrType): (VarLoc, String, RavelType.Value) = {
        return loc match {
            case VarLoc.Register => (VarLoc.Register, RegisterFile.regToStr(addrType.addr), addrType.typ)
            case VarLoc.Stack => (VarLoc.Stack, s"${addrType.addr}", addrType.typ)
            case VarLoc.Heap => (VarLoc.Heap, s"${addrType.addr}", addrType.typ)
        }
    }
}