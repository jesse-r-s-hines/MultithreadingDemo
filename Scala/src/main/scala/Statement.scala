package threadingdemo

import Parser.Source

// Represents a statement in the Ravel language.
abstract class RavelStatement extends Iterable[RavelStatement] {
    val src: Source;
    // Compiles the RavelStatement. Vars is a map of variable names to their addresses.
    // Returns the compiled instructions and modifies bindings to contain any new variable mappings.
    // If global is set, all variables will be put in globals.
    def compile(bindings: Bindings, global: Boolean): Seq[Instruction];

    // Iterables toString will try to make a comma seperated list and toString each of elements.
    // But since RavelStatement is an iterable of RavelStatements that causes an infinite recursion.
    // So just call the default object toString.
    override def toString(): String = super[Object].toString()
}

// Represents a if statement.
final class RavelIf(
    val cond: RavelExpr,
    val ifCase: Seq[RavelStatement],
    val elseCase: Seq[RavelStatement],
    val src: Source = null,
) extends RavelStatement {

    override def equals(that: Any): Boolean =
        that match { 
            case that: RavelIf =>
                this.cond == that.cond &&
                this.ifCase.sameElements(that.ifCase) && this.elseCase.sameElements(that.elseCase)
            case _ => false
        }

    override def iterator: Iterator[RavelStatement] = Iterator(this) ++ cond.iterator ++
        ifCase.foldLeft(Iterator[RavelStatement]())(_ ++ _) ++ elseCase.foldLeft(Iterator[RavelStatement]())(_ ++ _)

    override def compile(bindings: Bindings, global: Boolean): Seq[Instruction] = {
        // {cond}
        // beq $v0, 0, else:
        //     {ifCase}
        //     j end
        // else:
        //     {elseCase}
        // end:
        val (condComp, condRes) = this.cond.result(bindings, global)
        val ifCaseComp = Parser.compile(this.ifCase, bindings, global)
        val elseCaseComp = Parser.compile(this.elseCase, bindings, global)

        var compiled =
            condComp ++
            Vector(
                new Instruction("beq", condRes, "$zero", s"${ifCaseComp.length + 1}", this.src)
            ) ++
            ifCaseComp

        if (elseCaseComp.length > 0) {
            compiled = compiled ++
                Vector( new Instruction("j", s"${elseCaseComp.length}", src=this.src) ) ++
                elseCaseComp
        }
        return compiled
    }
}

// Represents a while loop.
final class RavelWhile(
    val cond: RavelExpr,
    val body: Seq[RavelStatement],
    val src: Source = null,
) extends RavelStatement {

    override def equals(that: Any): Boolean =
        that match { 
            case that: RavelWhile => this.cond == that.cond && this.body.sameElements(that.body)
            case _ => false
        }

    override def iterator: Iterator[RavelStatement] = Iterator(this) ++ cond.iterator ++
        body.foldLeft(Iterator[RavelStatement]())(_ ++ _)

    override def compile(bindings: Bindings, global: Boolean): Seq[Instruction] = {
        // top:
        // {cond}
        // beq $v0, 0, end:
        //     {body}
        //     j top
        // end:
        val (condComp, condRes) = this.cond.result(bindings, global)
        val bodyComp = Parser.compile(this.body, bindings, global)

        return condComp ++
            Vector( new Instruction("beq", condRes, "$zero", s"${bodyComp.length + 1}", this.src) ) ++
            bodyComp ++
            Vector( new Instruction("j", s"${-bodyComp.length - condComp.length - 2}", src=this.src) )
    }
}

// Represents a for loop.
final class RavelFor(
    val varInit: RavelInit,
    val cond: RavelExpr,
    val inc: RavelExpr,
    val body: Seq[RavelStatement],
    val src: Source = null,
) extends RavelStatement {

    override def equals(that: Any): Boolean =
        that match { 
            case that: RavelFor =>
                this.varInit == that.varInit && this.cond == that.cond && this.inc == that.inc &&
                this.body.sameElements(that.body)
            case _ => false
        }

    override def iterator: Iterator[RavelStatement] = Iterator(this) ++ varInit.iterator ++ cond.iterator ++
        body.foldLeft(Iterator[RavelStatement]())(_ ++ _) ++ inc.iterator

    override def compile(bindings: Bindings, global: Boolean): Seq[Instruction] = {
        // {varInit}
        // top:
        // {cond}
        // beq $v0, 0, end:
        //     {body}
        //     {inc}
        //     j top
        // end:
        val initComp = this.varInit.compile(bindings, global)
        val (condComp, condRes) = this.cond.result(bindings, global)
        val incComp  = this.inc.compile(bindings, global)
        val bodyComp = Parser.compile(this.body, bindings, global)

        return initComp ++
            condComp ++
            Vector( new Instruction("beq", condRes, "$zero", s"${bodyComp.length + incComp.length + 1}", this.src) ) ++
            bodyComp ++
            incComp ++
            Vector( new Instruction("j", s"${-incComp.length - bodyComp.length - condComp.length - 2}", src=this.src) )
    }
}

// Represents a builtin procedure.
// all my "procedures" take one reference parameter and you can't define more.
final class RavelProcedure(
    val name: String,
    val param: RavelId,
    val src: Source = null
) extends RavelStatement {
    override def equals(that: Any): Boolean =
        that match { 
            case that: RavelProcedure => this.name == that.name && this.param == that.param
            case _ => false
        }

    override def iterator: Iterator[RavelStatement] = Iterator(this) ++ param.iterator

    override def compile(bindings: Bindings, global: Boolean): Seq[Instruction] = {
        return bindings.find(param.name) match {
            case (VarLoc.Heap, addr) => Vector( new Instruction(this.name, addr, src=this.src, comment=param.name) )
            case _ => throw new Exception(s"Procedure $name must take a global variable as a parameter.")
        }
    }
}



// Expressions

// Represents anything that returns a value. Places the result in the v0 register when compiled.
abstract class RavelExpr extends RavelStatement {

    override def compile(bindings: Bindings, global: Boolean): Seq[Instruction] = {
        return result(bindings, global)._1;
    }

    /**
     * returns the result of compiling the expression, and the register that result is stored in or a immediate value
     * if this is a value. For example, if the expression is an Id in a register, it returns (Vector(), reg_name).
     * If its a +, it would return (compiled, "$v0"). If its a number it would return (Vector(), number)
     */
    def result(bindings: Bindings, global: Boolean): (Seq[Instruction], String);

    /**
     * If res is an immediate value, it will place it in a register and return the code needed to do so plus
     * the evaluation of the code function with the register. If res is already a register, it will just return
     * the result of the code function with res as the parameter and res as the second part of the tuple.
     * It returns (compiled, reg) where reg is where the result was placed, either "$v0" or res.
     */
    def wrapImmed(res: String, code: (String) => Seq[Instruction]): (Seq[Instruction], String) = {
        if (Instruction.isReg(res)) {
            return (code(res), res);
        } else {
            return (Vector( new Instruction("li", "$v0", res, src=this.src) ) ++ code("$v0"), "$v0")
        }
    }
}

// Represents a variable initialization.
final class RavelInit(
    val varType: RavelType.Value, // Can this be an actual type or something?
    val name: String,
    val rhs: RavelExpr = null,
    val src: Source = null,
) extends RavelExpr {

    override def equals(that: Any): Boolean = that match { 
        case that: RavelInit =>
            this.varType == that.varType && this.name == that.name && this.rhs == that.rhs
        case _ => false
    }

    override def iterator: Iterator[RavelStatement] =
        if (rhs != null) Iterator(this) ++ rhs.iterator else Iterator(this)

    // Compiles and adds a binding in bindings.
    override def result(bindings: Bindings, global: Boolean): (Seq[Instruction], String) = {
        // {rhs or default}
        // sw $v0, name
        var (rhsComp, rhsRes) = if (this.rhs != null) this.rhs.result(bindings, global) else (Vector(), "$zero")

        if (global) {
            val (comp, res) = this.wrapImmed(rhsRes, (reg) =>
                Vector( new Instruction("sw", reg, "$zero", bindings.bindGlobal(name, varType), this.src, comment=name) ))
            return (rhsComp ++ comp, res)
        } else {
            val (comp, res) = (bindings.bindLocal(name, varType) match {
                case (VarLoc.Register, reg) =>
                    if (Instruction.isReg(rhsRes)) {
                        ( Vector( new Instruction("move", reg, rhsRes, src=this.src) ), reg ) 
                    } else {
                        ( Vector( new Instruction("li", reg, rhsRes, src=this.src) ), reg )
                    }
                case (VarLoc.Stack, offset) => this.wrapImmed(rhsRes, (reg) =>
                        Vector( new Instruction("sw", reg, "$fp", offset, this.src, comment=name) ))       
            })
            return (rhsComp ++ comp, res)
        }
    }
}

// Represents a variable assignment.
final class RavelAssign(
    val name: String, // TODO change this to a RavelID?
    val rhs: RavelExpr,
    val src: Source = null
) extends RavelExpr {
    override def equals(that: Any): Boolean = that match { 
        case that: RavelAssign => this.name == that.name && this.rhs == that.rhs
        case _ => false
    }

    override def iterator: Iterator[RavelStatement] = Iterator(this) ++ rhs.iterator

    override def result(bindings: Bindings, global: Boolean): (Seq[Instruction], String) = {
        // {rhs}
        // sw $v0, name
        val (rhsComp, rhsRes) = this.rhs.result(bindings, global)

        val (comp, res) = (bindings.find(name) match {
            case (VarLoc.Heap, addr)    => this.wrapImmed(rhsRes, (reg) =>
                Vector( new Instruction("sw", reg, "$zero", addr, this.src, comment=name) ))
            case (VarLoc.Stack, offset) => this.wrapImmed(rhsRes, (reg) =>
                Vector( new Instruction("sw", reg, "$fp", offset, this.src, comment=name) ))
            case (VarLoc.Register, reg) =>
                if (Instruction.isReg(rhsRes))
                    ( Vector( new Instruction("move", reg, rhsRes, src=this.src) ), reg )
                else
                    ( Vector( new Instruction("li", reg, rhsRes, src=this.src) ), reg )
        })

        return (rhsComp ++ comp, res)
    }
}

// Represents a builtin function.
final class RavelFunc(
    val name: String,
    val param: RavelExpr,
    val src: Source = null
) extends RavelExpr {
    override def equals(that: Any): Boolean = that match { 
        case that: RavelFunc => this.name == that.name && this.param == that.param
        case _ => false
    }

    override def iterator: Iterator[RavelStatement] = Iterator(this) ++ param.iterator

    override def result(bindings: Bindings, global: Boolean): (Seq[Instruction], String) = {
        val (paramComp, paramRes) = this.param.result(bindings, global)
        // the only function we have is sem() which is just a wrapper around ints.
        return (paramComp, paramRes)
    }
}

// Represents a binary operator
final class RavelBinaryOp(
    val op: String, 
    val a: RavelExpr,
    val b: RavelExpr,
    val src: Source = null,
) extends RavelExpr {
    override def equals(that: Any): Boolean = that match { 
        case that: RavelBinaryOp => this.op == that.op && this.a == that.a && this.b == that.b
        case _ => false
    }

    override def iterator: Iterator[RavelStatement] = Iterator(this) ++ a.iterator ++ b.iterator

    override def result(bindings: Bindings, global: Boolean): (Seq[Instruction], String) = {
        return this.op match {
            case "&&" => (this.compileAnd(bindings, global), "$v0")
            case "||" => (this.compileOr(bindings, global), "$v0")
            case _ => (this.compileArithmeticOp(bindings, global), "$v0")
        }
    }

    // Compiles an binary operator. May need to make a temporary variable.
    private def compileArithmeticOp(bindings: Bindings, global: Boolean): Seq[Instruction] = {
        val instr = this.op match {
            case "+"  => "add" case "-"  => "sub" case "*"  => "mult" case "/"  => "div"
            case "%"  => "mod" case "==" => "seq" case "!=" => "sne"  case "<"  => "slt"
            case ">"  => "sgt" case "<=" => "sle" case ">=" => "sge"
        }

        val (aComp, aRes) = this.a.result(bindings, global)

        if (aRes == "$v0") { // We need to store the result in a temporary variable since $v0 gets overwritten.
            val tempVar = bindings.uniqueName()
            val varLoc = bindings.bindLocal(tempVar, RavelType.Int) // everything is a number internally anyways.
            val (bComp, bRes) = this.b.result(bindings, global)

            val rtrn = aComp ++ (varLoc match {
                case (VarLoc.Register, reg) =>
                    Vector( new Instruction("move", reg, "$v0", src=this.src) ) ++
                    bComp ++
                    Vector( new Instruction(instr, "$v0", reg, bRes, this.src) )
                 case (VarLoc.Stack, offset) =>
                    Vector( new Instruction("sw", "$v0", "$fp", offset, this.src) ) ++
                    bComp ++
                    Vector(
                        new Instruction("lw", "$at", "$fp", offset, this.src),
                        new Instruction(instr, "$v0", "$at", bRes, this.src)
                    )
            })
            bindings.unbindLocal(tempVar)
            return rtrn
        } else {
            val (bComp, bRes) = this.b.result(bindings, global)
            return aComp ++ bComp ++ Vector( new Instruction(instr, "$v0", aRes, bRes, this.src) ) 
        } 
    }

    private def compileAnd(bindings: Bindings, global: Boolean): Seq[Instruction] = {
        // {a}
        // beq $v0, 0, false
        // {b}
        // beq $v0, 0, false
        //     li $v0, 1
        //     j end
        // false:
        //     li $v0, 0
        // end:
        val (aComp, aRes) = this.a.result(bindings, global)
        val (bComp, bRes) = this.b.result(bindings, global)
        return aComp ++
            Vector(
                new Instruction("beq", aRes, "$zero", s"${bComp.length + 3}", this.src)
            ) ++
            bComp ++
            Vector(
                new Instruction("beq", bRes, "$zero", "2", this.src),
                new Instruction("li", "$v0", "1", src=this.src),
                new Instruction("j", "1", src=this.src),
                new Instruction("li", "$v0", "0", src=this.src)
            )
    }


    private def compileOr(bindings: Bindings, global: Boolean): Seq[Instruction] = {
        // {a}
        // bne $v0, 0, true
        // {b}
        // bne $v0, 0, true 
        //     li $v0, 0
        //     j end
        // true:
        //     li $v0, 1 
        // end: 
        
        val (aComp, aRes) = this.a.result(bindings, global)
        val (bComp, bRes) = this.b.result(bindings, global)
        return aComp ++
            Vector(
                new Instruction("bne", aRes, "$zero", s"${bComp.length + 3}", this.src)
            ) ++
            bComp ++
            Vector(
                new Instruction("bne", bRes, "$zero", "2", this.src),
                new Instruction("li", "$v0", "0", src=this.src),
                new Instruction("j", "1", src=this.src),
                new Instruction("li", "$v0", "1", src=this.src)
            )
    }
}

// Represents a unary operator, ie. "-", "+", "!".
final class RavelUnaryOp(
    val op: String,
    val operand: RavelExpr,
    val src: Source = null,
) extends RavelExpr {
    override def equals(that: Any): Boolean = that match { 
        case that: RavelUnaryOp => this.op == that.op && this.operand == that.operand
        case _ => false
    }

    override def iterator: Iterator[RavelStatement] = Iterator(this) ++ operand.iterator

    override def result(bindings: Bindings, global: Boolean): (Seq[Instruction], String) = {
        // {rhs}
        // "+" -- does nothing
        // "-" -- sub $v0, $zero $v0
        // "!" -- seq $v0, $v0, $zero
        val (opComp, opRes) = this.operand.result(bindings, global)
        return this.op match {
            case "+" => (opComp, opRes)
            case "-" => (opComp ++ Vector( new Instruction("sub", "$v0", "$zero", opRes, this.src) ), "$v0")
            case "!" => (opComp ++ Vector( new Instruction("seq", "$v0", opRes, "$zero", this.src) ), "$v0")
        }
    }
}

// Represents a variable.
final class RavelId(val name: String, val src: Source = null) extends RavelExpr {
    override def equals(that: Any): Boolean = that match { 
        case that: RavelId => this.name == that.name
        case _ => false
    }

    override def iterator: Iterator[RavelStatement] = Iterator(this)

    override def result(bindings: Bindings, global: Boolean): (Seq[Instruction], String) = {
        // lw $v0, {name} or just the register
        return bindings.find(name) match {
            case (VarLoc.Register, reg) =>  (Vector(), reg)
            case (VarLoc.Stack, offset) =>
                ( Vector( new Instruction("lw", "$v0", "$fp", offset, this.src, comment=s"$name") ), "$v0" )
            case (VarLoc.Heap,    addr) => 
                ( Vector( new Instruction("lw", "$v0", "$zero", addr, this.src, comment=s"$name") ), "$v0" )
        }
    }
}

// Represents a value, either a literal or a return value.
abstract class RavelVal extends RavelExpr {
    override def iterator: Iterator[RavelStatement] = Iterator(this)
}

// Represents an integer.
final class RavelInt(val value: Int, val src: Source = null) extends RavelVal {
    override def equals(that: Any): Boolean = that match { 
        case that: RavelInt => this.value == that.value
        case _ => false
    }
    override def result(bindings: Bindings, global: Boolean): (Seq[Instruction], String) = {
        return (Vector(), s"${value}")
    }
}

// Represents a boolean.
final class RavelBool(val value: Boolean, val src: Source = null) extends RavelVal {
    override def equals(that: Any): Boolean = that match { 
        case that: RavelBool => this.value == that.value
        case _ => false
    }

    override def result(bindings: Bindings, global: Boolean): (Seq[Instruction], String) = {
        return (Vector(), if (value) "1" else "0")
    }
}

// Represents a mutex lock.
final class RavelLock(val src: Source = null) extends RavelVal {
    var held: Boolean = false;

    override def equals(that: Any): Boolean = that match { 
        case that: RavelLock => this.held == that.held
        case _ => false
    }

    override def result(bindings: Bindings, global: Boolean): (Seq[Instruction], String) = {
        return (Vector(), "0"); // TODO
    }
}

// Represents a condition variable.
final class RavelCondVar(val src: Source = null) extends RavelVal {
    var set: Boolean = false;

    override def equals(that: Any): Boolean = that match { 
        case that: RavelCondVar => this.set == that.set
        case _ => false
    }

    override def result(bindings: Bindings, global: Boolean): (Seq[Instruction], String) = {
        return (Vector(), "0"); // TODO
    }
}

// Represents a semaphore.
final class RavelSem(var value: Int, val src: Source = null) extends RavelVal {
    override def equals(that: Any): Boolean = that match { 
        case that: RavelSem => this.value == that.value
        case _ => false
    }

    override def result(bindings: Bindings, global: Boolean): (Seq[Instruction], String) = {
        return (Vector(), "0"); // TODO
    }
}



// A enum for the value types in the language.
object RavelType extends Enumeration { 
    val Int, Bool, Mutex, CondVar, Sem = Value

    def fromString(str: String) : Value = {
        return str match {
            case "int"      => Int
            case "bool"     => Bool
            case "mutex"    => Mutex
            case "cond_var" => CondVar
            case "sem"      => Sem
        }
    }

    def toString(typ: RavelType.Value) : String = {
        return typ match {
            case Int     => "int"
            case Bool    => "bool"
            case Mutex   => "mutex"
            case CondVar => "cond_var"
            case Sem     => "sem"
        }
    }
}
