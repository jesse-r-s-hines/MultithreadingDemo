package threadingdemo

import scala.scalajs.js.JavaScriptException // I need this to make the errors show their source in the original Scala code.

object Tests {
    def test(): Unit = {
        scala.scalajs.js.Dynamic.global.test() // call testParse.js test functions
        testEquals()
        testMutableMap()
        testParse()
        testCompile()
        testBindings()
        testMemory()
        testExecute()
        testSynchronizationPrimitives()
        println("Tests Done.")
    }

    def testEquals(): Unit = {
        assertEqual(new RavelFunc("sem", new RavelInt(2)), new RavelFunc("sem", new RavelInt(2)))
        assertEqual(new RavelLock(), new RavelLock())
        assertEqual(new RavelCondVar(), new RavelCondVar())
        assertEqual(new RavelSem(3), new RavelSem(3))

        assertNotEqual(new RavelId("test"), new RavelId("Test"))
        assertNotEqual(new RavelSem(3), new RavelSem(5))

        assertEqual(new RavelIf(
            new RavelBinaryOp("<", new RavelInt(2), new RavelInt(3)),
            Vector( new RavelProcedure("lock", new RavelId("x")) ),
            Vector( new RavelInit(RavelType.Mutex, "y", new RavelBool(true)) )
        ), new RavelIf(
            new RavelBinaryOp("<", new RavelInt(2), new RavelInt(3)),
            Vector( new RavelProcedure("lock", new RavelId("x")) ),
            Vector( new RavelInit(RavelType.Mutex, "y", new RavelBool(true)) )
        ))

        assertEqual(new RavelWhile(new RavelBool(true),
            Vector( new RavelAssign("x", new RavelInt(3)) ),
        ), new RavelWhile(new RavelBool(true),
            Vector( new RavelAssign("x", new RavelInt(3)) ),
        ))

        assertEqual(new RavelFor(
            new RavelInit(RavelType.Int, "i", new RavelInt(2)),
            new RavelBool(true),
            new RavelAssign("i", new RavelBinaryOp("+",
                new RavelId("i"), new RavelInt(1))),
            Vector( new RavelUnaryOp("-", new RavelInt(3)) ),
        ), new RavelFor(
            new RavelInit(RavelType.Int, "i", new RavelInt(2)),
            new RavelBool(true),
            new RavelAssign("i", new RavelBinaryOp("+",
                new RavelId("i"), new RavelInt(1))),
            Vector( new RavelUnaryOp("-", new RavelInt(3)) ),
        ))

        assertNotEqual(new RavelWhile(new RavelBool(true),
            Vector( new RavelAssign("x", new RavelInt(3)) ),
        ), new RavelWhile(new RavelBool(true),
            Vector( new RavelAssign("x", new RavelInt(4)) ),
        ))

        assertEqual(new RavelFor(null, null, null, Vector()), new RavelFor(null, null, null, Vector()));
        assertEqual(new RavelInt(3, ((1,2), (1,2))), new RavelInt(3, ((1,2), (1,3)))); // src is NOT compared.
    }

    def testMutableMap(): Unit = {
        val map = new MutableMap[Int, Int]()
        map(4) = 1
        assertEqual(map(4), 1)
        map.remove(4)
        assertEqual(map.get(4).isEmpty, true)
    }

    def testParse() : Unit = {
        def codeEqual(code: String, expected: Seq[RavelStatement]) = {
            val parsed = Parser.parse(code)
            assert(parsed.sameElements(expected), s"For '${code}': Expected $expected, but got $parsed")
        }

        codeEqual("2;",   Vector(new RavelInt(2)  ));
        codeEqual("+2;",  Vector(new RavelUnaryOp("+", new RavelInt(2)) ));
        codeEqual("2+3;", Vector(new RavelBinaryOp("+", new RavelInt(2), new RavelInt(3)) ));
        codeEqual("7/3;", Vector(new RavelBinaryOp("/", new RavelInt(7), new RavelInt(3)) ));
        codeEqual("2 + 2 * 4;",
            Vector(new RavelBinaryOp("+", new RavelInt(2), new RavelBinaryOp("*", new RavelInt(2), new RavelInt(4))))
        );

        codeEqual("true;",  Vector(new RavelBool(true)  ));
        codeEqual("false;", Vector(new RavelBool(false) ));
        
        codeEqual("! true;", Vector(new RavelUnaryOp("!", new RavelBool(true)) ));

        codeEqual("hello;", Vector(new RavelId("hello") ));

        codeEqual("int x;", Vector(new RavelInit(RavelType.Int, "x",  null)));
        codeEqual("bool myVar = true;", Vector(new RavelInit(RavelType.Bool, "myVar",  new RavelBool(true))));
        codeEqual("mutex m;", Vector(new RavelInit(RavelType.Mutex, "m", null)));
        codeEqual("cond_var c;", Vector(new RavelInit(RavelType.CondVar, "c", null)));
        codeEqual("sem s = sem_create(3);", Vector(
            new RavelInit(RavelType.Sem, "s",  new RavelFunc("sem_create",  new RavelInt(3)))
        ));

        codeEqual("y = 4;", Vector(new RavelAssign("y",  new RavelInt(4))));
        codeEqual("z += 4;", Vector(new RavelAssign("z", new RavelBinaryOp("+", new RavelId("z"),  new RavelInt(4)))));
        codeEqual("z *= 4;", Vector(new RavelAssign("z", new RavelBinaryOp("*", new RavelId("z"),  new RavelInt(4)))));

        codeEqual("x++;",  Vector(new RavelAssign("x", new RavelBinaryOp("+", new RavelId("x"),  new RavelInt(1)))));
        codeEqual("x --;", Vector(new RavelAssign("x", new RavelBinaryOp("-", new RavelId("x"),  new RavelInt(1)))));



        codeEqual("lock(x);",         Vector(new RavelProcedure("lock",      new RavelId("x"))));
        codeEqual("unlock(y);",       Vector(new RavelProcedure("unlock",    new RavelId("y"))));
        codeEqual("cond_wait(z);",     Vector(new RavelProcedure("cond_wait",  new RavelId("z"))));
        codeEqual("cond_signal(var);", Vector(new RavelProcedure("cond_signal",new RavelId("var"))));
        codeEqual("sem_wait(bar);",    Vector(new RavelProcedure("sem_wait",   new RavelId("bar"))));
        codeEqual("sem_post(foo);",    Vector(new RavelProcedure("sem_post",   new RavelId("foo"))));

        codeEqual("if (1 < 2) {x = 3;}", Vector(
            new RavelIf(
                new RavelBinaryOp("<", new RavelInt(1), new RavelInt(2)),
                Vector(new RavelAssign("x",  new RavelInt(3))),
                Vector())
        ));
        codeEqual(
            """if (true) {
                x = 3;
                y = 3;
            } else {
                x = 2;
            }""", Vector(
            new RavelIf(
                new RavelBool(true) ,
                Vector(new RavelAssign("x",  new RavelInt(3)), new RavelAssign("y",  new RavelInt(3))),
                Vector(new RavelAssign("x",  new RavelInt(2))))
        ));

        codeEqual("while (x <= 9) { x++;;};", Vector(
            new RavelWhile(new RavelBinaryOp("<=", new RavelId("x"), new RavelInt(9)),
                Vector(new RavelAssign("x", new RavelBinaryOp("+", new RavelId("x"),  new RavelInt(1)))))
        ));

        codeEqual("for (int i = 3; i < 100; i++) {2;};", Vector(
            new RavelFor(
                new RavelInit(RavelType.Int, "i",  new RavelInt(3)),
                new RavelBinaryOp("<", new RavelId("i"), new RavelInt(100)),
                new RavelAssign("i", new RavelBinaryOp("+", new RavelId("i"),  new RavelInt(1))),
                Vector(new RavelInt(2)))
        ));

        codeEqual("if (true); else;", Vector( new RavelIf(new RavelBool(true), Vector(), Vector()) ))

        val ast = Parser.parse("""
            mutex m;
            for (int i = 3; i < 100; i = i + 1) {
                while (true) {
                    if (false) lock(m); else unlock(m);
                }
            }
        """) // test iterator.

        assertEqual(ast(1).toVector.map(_.getClass.getSimpleName), Vector(
            "RavelFor", "RavelInit", "RavelInt", "RavelBinaryOp", "RavelId", "RavelInt", "RavelWhile",
            "RavelBool", "RavelIf", "RavelBool", "RavelProcedure", "RavelId", "RavelProcedure", "RavelId",
            "RavelAssign", "RavelBinaryOp", "RavelId", "RavelInt"
        ))

        Parser.parse("int x;").toString(); // just make sure it doesn't throw.
    }

    def testCompile(): Unit = {
        def instr(instr: String, a: String = null, b: String = null, c: String = null) =
            new Instruction(instr, a, b, c, null)

        // Basic test of compile. We will test compile fully when we test that the code behaves as expected in execute.
        val code = Parser.parse("""
            int i = 3;     
            int j = i + 1;
            """);

        assertEqual(Parser.compile(code, new Bindings(), false), Vector(
            instr("li", "$t0", "3"),
            instr("add", "$v0", "$t0", "1"),
            instr("move", "$t1", "$v0")
        ))

        assertEqual(Parser.compile(code, new Bindings(), true), Vector(
            instr("li", "$v0", "3"),
            instr("sw", "$v0", "$zero", "0"),
            instr("lw", "$v0", "$zero", "0"),
            instr("move", "$t0", "$v0"),
            instr("add", "$v0", "$t0", "1"),
            instr("sw", "$v0", "$zero", "4")
        ))

        assertEqual(instr("sub", "$v0", "$zero", "$v0").toString(), "sub $v0, $zero, $v0")
        assertEqual(instr("li", "$v0", "3").toString(), "li $v0, 3")
        assertEqual(instr("lw", "$v0", "$sp", "4").toString(), "lw $v0, 4($sp)")
        assertEqual(instr("sw", "$v0", "$zero", "433").toString(), "sw $v0, 0x1b1")
        assertEqual(instr("lock","16").toString(), "lock 0x10")

        assertEqual(new Instruction("j", "7", src=null, comment="Comment").toString(), "j 7 # Comment")
    }

    def testBindings(): Unit = {
        var bindings = new Bindings()

        for (i <- 0 until 18) { // the first 18 should be put in registers,
            val expected = (VarLoc.Register, RegisterFile.regToStr(i + 8))
            assertEqual(bindings.bindLocal(s"regVar$i", RavelType.Int), expected)
            assertEqual(bindings.find(s"regVar$i"), expected)
        }
        for ( (i, addr) <- (0 until 8).zip(0 to -6*4 by -4) ) { // then it should start using the stack.
            val expected = (VarLoc.Stack, s"$addr")
            assertEqual(bindings.bindLocal(s"stackVar$i", RavelType.Bool), expected)
            assertEqual(bindings.find(s"stackVar$i"), expected)
        }

        assertThrows(bindings.bindLocal("regVar4", RavelType.Int)) // can't rebind.
        assertThrows(bindings.bindLocal("regVar4", RavelType.Bool)) // can't rebind.
        assertThrows(bindings.bindLocal("stackVar3", RavelType.Bool)) // can't rebind.

        assertEqual(bindings.findWithType("regVar0"), (VarLoc.Register, "$t0", RavelType.Int))
        assertEqual(bindings.findWithType("stackVar0"), (VarLoc.Stack, "0", RavelType.Bool))

        bindings.unbindLocal("regVar1") // in a register
        assertThrows(bindings.find("regVar1"))
        assertEqual(bindings.bindLocal(s"newVar0", RavelType.Int), (VarLoc.Register, "$t1")) // uses released register.

        for (id <- Vector("stackVar0", "stackVar1", "stackVar4", "stackVar7")) { // in stack
            bindings.unbindLocal(id)
            assertThrows(bindings.find(id))
        }

        assertEqual(bindings.bindLocal(s"newVar1", RavelType.Int), (VarLoc.Stack, "0")) // uses released stack slot.
        assertEqual(bindings.bindLocal(s"newVar2", RavelType.Int), (VarLoc.Stack, "-4")) // uses released stack slot.
        assertEqual(bindings.bindLocal(s"newVar3", RavelType.Int), (VarLoc.Stack, "-16")) // uses released stack slot.
        assertEqual(bindings.bindLocal(s"newVar4", RavelType.Int), (VarLoc.Stack, "-28")) // uses released stack slot.
        assertEqual(bindings.bindLocal(s"newVar5", RavelType.Int), (VarLoc.Stack, "-32")) // uses next slot.

        
        assertEqual(bindings.bindGlobal("globalVar0", RavelType.Int), "0")
        assertEqual(bindings.bindGlobal("globalVar1", RavelType.Int), "4")
        assertEqual(bindings.find("globalVar0"), (VarLoc.Heap, "0"))

        assertThrows(bindings.bindGlobal("globalVar1", RavelType.Int)) // can't rebind.

        assertEqual(bindings.bindGlobal("regVar0", RavelType.Int), "8")
        assertEqual(bindings.find("regVar0"), (VarLoc.Register, "$t0")) // locals shadow globals.

        assertEqual(bindings.uniqueName(), "$temp0")
        assertEqual(bindings.uniqueName(), "$temp1")

        val bindings2 = new Bindings()
        bindings2.bindGlobal("a", RavelType.Int)
        bindings2.bindLocal("b", RavelType.Bool)
        assertEqual(bindings2.toList, List(("a", VarLoc.Heap, "0", RavelType.Int),
                                          ("b", VarLoc.Register, "$t0", RavelType.Bool)))

    }

    def testMemory(): Unit = {
        val memory = new Memory(1000)
        memory(4) = 14
        memory(100) = 20
        assertEqual(memory(4), 14)
        assertEqual(memory(100), 20)
        assertEqual(memory(123), 0)
        assertThrows(memory(10))
        assertThrows(memory(-1))
        assertThrows(memory(1000))

        val registers = new RegisterFile()
        assertEqual(registers(4), 0)
        registers(2) = 5
        assertEqual(registers(2), 5)
        registers("$v1") = 400
        assertEqual(registers("$v1"), 400)

        assertEqual(memory.toString(), 
            "   address : contents\n" +
            "0x00000004 : 14      \n" +
            "0x00000064 : 20      ")
        assertEqual(registers.toString(), 
            "register ( #) : contents\n" +
            "   $zero ( 0) : 0       \n" + "     $at ( 1) : 0       \n" + "     $v0 ( 2) : 5       \n" +
            "     $v1 ( 3) : 400     \n" + "     $a0 ( 4) : 0       \n" + "     $a1 ( 5) : 0       \n" +
            "     $a2 ( 6) : 0       \n" + "     $a3 ( 7) : 0       \n" + "     $t0 ( 8) : 0       \n" +
            "     $t1 ( 9) : 0       \n" + "     $t2 (10) : 0       \n" + "     $t3 (11) : 0       \n" +
            "     $t4 (12) : 0       \n" + "     $t5 (13) : 0       \n" + "     $t6 (14) : 0       \n" +
            "     $t7 (15) : 0       \n" + "     $s0 (16) : 0       \n" + "     $s1 (17) : 0       \n" +
            "     $s2 (18) : 0       \n" + "     $s3 (19) : 0       \n" + "     $s4 (20) : 0       \n" +
            "     $s5 (21) : 0       \n" + "     $s6 (22) : 0       \n" + "     $s7 (23) : 0       \n" +
            "     $t8 (24) : 0       \n" + "     $t9 (25) : 0       \n" + "     $k0 (26) : 0       \n" +
            "     $k1 (27) : 0       \n" + "     $gp (28) : 0       \n" + "     $sp (29) : 0       \n" +
            "     $fp (30) : 0       \n" + "     $ra (31) : 0       ")

        val thread1 = Thread.compileFrom("int i = 0;", memory)
        val thread2 = Thread.compileFrom("int j = 0;", memory)

        memory.enqueueThread(0, thread1)
        memory.enqueueThread(4, thread2)
        assertEqual(memory.getThreadQueue(0), Set(thread1))
        assertEqual(memory.getBlockerFor(thread2), Some(4))
        memory.dequeueThread(0, thread1)
        assertEqual(memory.getThreadQueue(0), Set[Thread]())
        assertEqual(memory.getThreadQueue(4), Set(thread2))
        memory.clearThreadQueue(4)
        assertEqual(memory.getThreadQueue(4), Set[Thread]())
        assertEqual(memory.getBlockerFor(thread2), None)


        assertEqual(RegisterFile.regFromStr("$zero"), 0)
        assertEqual(RegisterFile.regFromStr("$at"), 1)
        assertEqual(RegisterFile.regFromStr("$v1"), 3)
        assertEqual(RegisterFile.regFromStr("$a2"), 6)
        assertEqual(RegisterFile.regFromStr("$t7"), 15)
        assertEqual(RegisterFile.regFromStr("$t8"), 24)
        assertEqual(RegisterFile.regFromStr("$s0"), 16)
        assertEqual(RegisterFile.regFromStr("$ra"), 31)

        assertEqual(RegisterFile.regToStr(0), "$zero")
        assertEqual(RegisterFile.regToStr(1), "$at")
        assertEqual(RegisterFile.regToStr(3), "$v1")
        assertEqual(RegisterFile.regToStr(6), "$a2")
        assertEqual(RegisterFile.regToStr(15), "$t7")
        assertEqual(RegisterFile.regToStr(24), "$t8")
        assertEqual(RegisterFile.regToStr(16), "$s0")
        assertEqual(RegisterFile.regToStr(31), "$ra")
    }

    def testExecute(): Unit = {
        var thread = Thread.compileFrom(""" // Basic assignment
            int i = 100;
            int j = i + 1;
        """);
        assertEqual(thread.state, Thread.State.Running);
        thread.execute();
        assertVarsEqual(thread, "i" -> 100, "j" -> 101);
        assertEqual(thread.state, Thread.State.Complete);
        assertEqual(thread.getVarFromAddr(VarLoc.Register, "$t0"), (Some("i"), 100, RavelType.Int))


        thread = Thread.compileFrom("""
            bool and0 = true && true;
            bool and1 = 3 && 0;
            bool and2 = false && 1 / 0 == 1;
            bool and3 = 1 == 0 && 1 == 0;

            bool or0 = 0 || 0;
            bool or1 = true || 1/0 == 1;
            bool or2 = false || true;
            bool or3 = 1 == 1 || 2 == 2;

            bool not0 = ! true;
            bool not1 = ! false;
            bool not2 = !false && ! false;
        """);
        thread.execute();
        assertVarsEqual(thread, "and0"  -> 1, "and1"  -> 0, "and2"  -> 0, "and3" -> 0,
                                "or0"   -> 0, "or1"   -> 1, "or2"   -> 1, "or3" -> 1,
                                "not0"  -> 0, "not1"  -> 1, "not2"  -> 1)

        thread = Thread.compileFrom("""
            bool b0 = 1 == 1;
            bool b1 = 1 == 2;
            bool b2 = true != false;
            bool b3 = 3 != 3;
            bool b4 = -1 < 100;
            bool b5 = 2 + 4 < 1 * 2;
            bool b6 = 3 > 1;
            bool b7 = 1 > 2;
            bool b8 = 2 <= 2 && 2 >= 1;
            bool b9 = 2 <= 1;
            bool b10 = 2 >= 3;
        """);
        thread.execute();
        assertVarsEqual(thread, "b0" -> 1, "b1" -> 0, "b2" -> 1, "b3" -> 0, "b4" -> 1, "b5" -> 0,
                                "b6" -> 1, "b7" -> 0, "b8" -> 1, "b9" -> 0, "b10" -> 0)

        thread = Thread.compileFrom(""" 
            int a0 = +2;
            int a1 = -553;
            int a2 = 2 + 3;
            int a3 = 5 - 2;
            int a4 = 6 * 2;
            int a5 = 7 / 3;
            int a6 = 64 % 5;
            int a7 = 1 * 2 + 2 / 3;
            int a8 = ((2 * 4)) - -4;
            int a9 = 2 + 3 - 1 * 2;
        """);
        thread.execute();
        assertVarsEqual(thread, "a0" -> 2, "a1" -> -553, "a2" -> 5, "a3" -> 3, "a4" -> 12,
                                "a5" -> 2, "a6" -> 4, "a7" -> 2, "a8" -> 12, "a9" -> 3)
        assertThrows(thread.getVar("$temp0")) // temp variables should be freed.

        thread = Thread.compileGlobalFrom(""" 
            int sum; // default is 0.

            // Find Sum.
            for (int i = 0; i <= 100; i++) {
                sum += i;
            }
        """);
        thread.execute();
        assertVarsEqual(thread, "sum" -> 5050)
        assertEqual(thread.bindings.find("sum")._1, VarLoc.Heap) // sum is in globals in a global thread.
        assertEqual(thread.getVarFromAddr(VarLoc.Heap, "0"), (Some("sum"), 5050, RavelType.Int))

        thread = Thread.compileFrom("""
            // Fill up all the registers.
            int r0;  int r1;  int r2;  int r3;  int r4;  int r5;  int r6;  int r7;  int r8;  int r9;
            int r10; int r11; int r12; int r13; int r14; int r15; int r16; int r17; int r18; int r19;

            // Now these variables will have to be in stack space. See if it still works.
            int gcd = 0;

            // Euclid's GCD Algorithm.
            int a = 128; int b = 800;
            while (a != b) { // do until the two numbers become equal
                if (a > b) // replace larger number by its difference with the smaller number
                    a = a - b;
                else
                    b = b - a; 
            }

            gcd = a;
        """);
        thread.execute();
        assertVarsEqual(thread, "gcd" -> 32)
        assertEqual(thread.getVarWithType("a"), (32, RavelType.Int))
        assertEqual(thread.getVarFromAddr(VarLoc.Stack, "-8"), (Some("gcd"), 32, RavelType.Int))
        assertEqual(thread.getVarFromAddr(VarLoc.Register, "$v0"), (None, 32, RavelType.Int))

        thread = Thread.compileFrom(""" 
            bool b0 = true && !true || 3 % 2 < 5 && false;
            bool b1 = true && true || true && false;
            int x = 1;
            int y = x = 3;
            int z = (x = 4) + (y = 5);
        """);
        thread.execute();
        assertVarsEqual(thread, "b0" -> 0, "b1" -> 1, "x" -> 4, "y" -> 5, "z" -> 9)

        thread = Thread.compileFrom("""
            bool b;
            int result;

            int x = 30;
            if (x < 10) {
                result = 1;
            } else if (x < 25) {
                result = 2;
            } else if (x < 50) {
                result = 3;
                b = true;
            } else {
                result = 4;
            }
        """);
        thread.execute();
        assertVarsEqual(thread, "b" -> 1, "result" -> 3)

        thread = Thread.compileFrom("""
            int a = 0;
            int b = 0;
            if (1 == 1) {
                a = 1;
                b = 1;
            }
            a = 2;
        """);
        thread.execute();
        assertVarsEqual(thread, "a" -> 2, "b" -> 1)

        thread = Thread.compileFrom("""
            int a = 0;
            int b = 0;
            if (1 == 2) {
                a = 1;
                b = 1;
            }
            a = 2;
        """);
        thread.execute();
        assertVarsEqual(thread, "a" -> 2, "b" -> 0)

        thread = Thread.compileFrom("""
            for (int i = 0; i < 10; i++) {
                int x = 3; // redefining in a loop should still work.
            }
        """);
        thread.execute();
        assertVarsEqual(thread, "i" -> 10, "x" -> 3)

        val memory = new Memory()
        val globalThread = Thread.compileGlobalFrom("int global = 3;", memory);
        globalThread.execute();

        val thread0 = Thread.compileFrom("int local = global;", memory, globalThread.bindings.globals, Int.MaxValue)
        val thread1 = Thread.compileFrom("int local = global + 1;", memory, globalThread.bindings.globals,
                                         Int.MaxValue - 1024 * 1024)
        
        thread0.execute();
        thread1.execute();

        assertVarsEqual(thread0, "local" -> 3)
        assertVarsEqual(thread1, "local" -> 4)


        thread = Thread.compileFrom("""
            int a; int b;
            if (true) a = 3;

            int cond = false;
            if (cond) b = 1; else b = 2;
        """);
        thread.execute();
        assertVarsEqual(thread, "a" -> 3, "b" -> 2)

        thread = Thread.compileFrom("""
            int x; int y;
            int result = 9 - (x=3)*(7 + 4%47) + (9 + 8/(2))*(3*(y=5*100)) - (7<(10000/3)) * -78 +
                         (2*3 == 3*2 && 5*5 == 25) + (-134 || 2 - 2);
        """);
        thread.execute();
        assertVarsEqual(thread, "x" -> 3, "y" -> 500, "result" -> 19556)
    }

    def testSynchronizationPrimitives():Unit = {
        // Test Locks
        var memory = new Memory()

        var globals = Thread.compileGlobalFrom("""
            int counter = 0;
            mutex m;
        """, memory, Int.MaxValue);
        var threadCode = """
            for (int i = 0; i < 10; i++) {
                lock(m);
                    counter++;
                unlock(m);
            }
        """
        var thread1 = Thread.compileFrom(threadCode, memory, globals.bindings.globals, Int.MaxValue - 1024*1024);
        var thread2 = Thread.compileFrom(threadCode, memory, globals.bindings.globals, Int.MaxValue - 2 * 1024*1024);

        globals.execute();

        while (thread1.locksHeld.isEmpty) {
            thread1.step()
        }
        assertEqual(thread1.state, Thread.State.Running)
        assertEqual(thread1.locksHeld.contains(4), true)
        assertEqual(memory.getThreadQueue(4).isEmpty, true)
        assertVarsEqual(thread1, "m" -> 1)

        thread2.execute() // executes until thread is blocked.
        assertEqual(thread2.state, Thread.State.Blocked)
        assertEqual(thread2.step(), Thread.State.Blocked)
        assertEqual(thread2.locksHeld.isEmpty, true)
        assertEqual(memory.getThreadQueue(4).contains(thread2), true)
        assertVarsEqual(thread2, "m" -> 1)

        thread1.execute() // complete the thread.
        thread2.execute() // complete the thread
        assertEqual(thread2.state, Thread.State.Complete)
        assertEqual(memory.getThreadQueue(4).isEmpty, true)
        assertVarsEqual(thread2, "m" -> 0, "counter" -> 20)

        // Test Condition Variables
        memory = new Memory()
        globals = Thread.compileGlobalFrom("int done = 0; cond_var c;", memory, Int.MaxValue);
        thread1 = Thread.compileFrom("""
            done = 1;
            cond_signal(c);
            for (int i = 0; i < 10; i++);
        """, memory, globals.bindings.globals, Int.MaxValue - 1024*1024);
        thread2 = Thread.compileFrom("""
            cond_wait(c);
            int x = 10; // this won't execute until thread one has set the cond_signal.
        """, memory, globals.bindings.globals, Int.MaxValue - 2 * 1024*1024);

        globals.execute();

        thread2.execute() // executes until thread is blocked.
        assertEqual(thread2.state, Thread.State.Blocked)
        assertEqual(thread2.step(), Thread.State.Blocked)
        assertEqual(memory.getThreadQueue(4).contains(thread2), true)
        assertVarsEqual(thread2, "c" -> 0, "done" -> 0)

        while (thread1.getVar("c") == 0) {
            thread1.step()
        }
        assertEqual(thread1.state, Thread.State.Running)
        assertVarsEqual(thread1, "c" -> 1, "done" -> 1)

        thread2.execute() // complete the thread.
        assertEqual(thread2.state, Thread.State.Complete)
        assertEqual(memory.getThreadQueue(4).isEmpty, true)
        assertVarsEqual(thread2, "c" -> 1, "done" -> 1)

        // Test Semaphores
        memory = new Memory()

        globals = Thread.compileGlobalFrom("""
            int counter = 0;
            sem s = sem_create(1);
        """, memory, Int.MaxValue);
        threadCode = """
            for (int i = 0; i < 10; i++) {
                sem_wait(s);
                    counter++;
                sem_post(s);
            }
        """
        thread1 = Thread.compileFrom(threadCode, memory, globals.bindings.globals, Int.MaxValue - 1 * 1024*1024);
        thread2 = Thread.compileFrom(threadCode, memory, globals.bindings.globals, Int.MaxValue - 2 * 1024*1024);
        var thread3 = Thread.compileFrom(threadCode, memory, globals.bindings.globals, Int.MaxValue - 3 * 1024*1024);

        globals.execute();

        while (globals.getVar("counter") == 0) { // step until inside "lock"
            thread1.step()
        }
        assertEqual(thread1.state, Thread.State.Running)
        assertEqual(memory.getThreadQueue(4).isEmpty, true)
        assertVarsEqual(thread1, "s" -> 0)

        thread2.execute() // executes until thread is blocked, waiting at sem_wait
        thread3.execute() // thread3 as well.
        assertEqual(thread2.state, Thread.State.Blocked)
        assertEqual(thread3.step(), Thread.State.Blocked)
        assertEqual(memory.getThreadQueue(4), Set(thread2, thread3))
        assertVarsEqual(thread2, "s" -> -2)

        thread1.execute() // executes until input is required.
        assertEqual(thread1.inputOptions, Set(thread2, thread3))
        assertEqual(memory.getThreadQueue(4), Set(thread2, thread3))

        thread1.step(thread2) // step over sem_post and wake up thread2
        assertEqual(memory.getThreadQueue(4), Set(thread3))
        assertEqual(thread1.state, Thread.State.Running)
        assertEqual(thread2.state, Thread.State.Running)
        assertEqual(thread3.state, Thread.State.Blocked) // still blocked, only thread3 was woken.
        assertEqual(memory.getThreadQueue(4), Set(thread3))
        assertVarsEqual(thread2, "s" -> -1, "counter" -> 1)

        thread2.execute() // until we hit the sem post for thread2
        assertEqual(thread2.inputOptions, Set(thread3))
        thread2.step(thread3) // step over sem_post and wake up thread3
        assertEqual(thread1.state, Thread.State.Running)
        assertEqual(thread2.state, Thread.State.Running)
        assertEqual(thread3.state, Thread.State.Running)
        assertEqual(memory.getThreadQueue(4).isEmpty, true)
        assertVarsEqual(thread2, "s" -> 0, "counter" -> 2)

        thread3.execute()
        assertEqual(thread3.state, Thread.State.Complete) // no input required since other threads hold the semaphore so thread should complete
        assertVarsEqual(globals, "s" -> 1, "counter" -> 12)
    
        thread2.execute()
        assertEqual(thread2.state, Thread.State.Complete) // no input required since other threads hold the semaphore so thread should complete
        assertVarsEqual(globals, "s" -> 1, "counter" -> 21)

        thread1.execute()
        assertEqual(thread1.state, Thread.State.Complete) // no input required since other threads hold the semaphore so thread should complete
        assertVarsEqual(globals, "s" -> 1, "counter" -> 30)
    }


    /**
     * Tests that a threads variables match the given assertion list. 
     * Give it a vararg list of (varname -> expectedValue) tuples
     */
    def assertVarsEqual(thread: Thread, assertions: (String, Int)*) = {
        for ((id, expected) <- assertions) {
            assert(thread.getVar(id) == expected,
                s"Expected $id to equal $expected but got ${thread.getVar(id)}.\n" +
                s"Code was:\n${thread.code}\nAssembly was:\n${thread.assembly.mkString("\n")}") 
        }
    }

    private def assertEqual(obj: Any, expected: Any) : Unit = {
        // Note, Vector equality works, but Array equality is only identity.
        if (obj != expected)
            throw new JavaScriptException(s"Expected $expected, but got $obj")
    }

    private def assertNotEqual(obj: Any, expected: Any) : Unit = {
        if (obj == expected)
            throw new JavaScriptException(s"Expected to NOT get $expected, but got $obj")
    }

    private def assertThrows(func: => Unit): Unit = { // call-by-name
        try {
            func
            throw new JavaScriptException(s"Expected function to throw an exception.")
        } catch {
            case e:Exception => {}
        }
    }
}