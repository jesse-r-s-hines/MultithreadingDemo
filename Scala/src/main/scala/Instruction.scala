package threadingdemo

import Parser.Source


/**
 *  paramaters can be a register a raw int
 *  registers start with $, and numbers are translated as numbers, and anything else is an id.
 *
 *  "lw" -- lw <reg> <reg> <int>
 *  "sw" -- sw <reg> <reg> <int>
 *  "li" -- li <reg>, <int>
 *
 *  "add"  -- add  <reg>, <reg>, <reg>
 *  "sub"  -- sub  <reg>, <reg>, <reg>
 *  "mult" -- mult <reg>, <reg>, <reg>
 *  "div"  -- div  <reg>, <reg>, <reg>
 *  "mod"  -- mod  <reg>, <reg>, <reg>
 *
 *  "move" -- move <reg> <reg>
 *
 *  "seq" -- seq <reg>, <reg>, <reg>
 *  "sne" -- sne <reg>, <reg>, <reg>
 *  "slt" -- slt <reg>, <reg>, <reg>
 *  "sgt" -- sgt <reg>, <reg>, <reg>
 *  "sle" -- sle <reg>, <reg>, <reg>
 *  "sge" -- sge <reg>, <reg>, <reg>
 *
 *  "beq" -- beq <reg> <reg> <int>
 *  "bne" -- bne <reg> <reg> <int>
 *  "j"   -- j <int>
 *
 *  "lock"       -- lock       <int/address>
 *  "unlock"     -- unlock     <int/address>
 *  "cond_wait"   -- cond_wait   <int/address>
 *  "cond_signal" -- cond_signal <int/address>
 *  "sem_wait"    -- sem_wait    <int/address>
 *  "sem_post"    -- sem_post    <int/address>
 */ 
final class Instruction(val instr: String,
    val a: String = null, val b: String = null, val c: String = null, val src: Source, val comment: String = "") {
 
    override def equals(that: Any): Boolean =
        that match { 
            case that: Instruction => // src doesn't get compared
                this.instr == that.instr && this.a == that.a && this.b == that.b && this.c == that.c
            case _ => false
        }

    override def toString() : String = {
        var str = instr match {
            case "lw" | "sw" => if (b == "$zero") f"$instr $a, 0x${c.toInt}%x" else  s"$instr $a, $c($b)";
            case "lock" | "unlock" | "cond_wait" | "cond_signal" | "sem_wait" | "sem_post" => f"$instr 0x${a.toInt}%x"
            case _ => instr + " " + Vector(a, b, c).filter(_ != null).mkString(", "); 
        }
        if (comment != "") str = s"$str # $comment"; 
        return str;
    }
}

object Instruction {
    /**
      * Deconstructs an Instruction into its paramaters again for pattern matching.
      */
    def unapply(instr: Instruction): Some[(String, String, String, String)] = {
        Some((instr.instr, instr.a, instr.b, instr.c))
    }

    /**
     * Returns true if the string represents a regester.
     */
    def isReg(str: String): Boolean = {
        return str(0) == '$'
    }
}