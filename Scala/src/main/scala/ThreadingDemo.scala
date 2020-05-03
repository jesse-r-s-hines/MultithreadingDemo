package threadingdemo

import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => jsGlobal}
import scala.scalajs.js.Dynamic.literal
import scala.scalajs.js.annotation._
import org.scalajs.dom
import dom.document
import org.scalajs.dom.html
import io.udash.wrappers.jquery._
import js.JSConverters._
import scala.collection.mutable.Map;
import scala.collection.mutable.ArrayBuffer;
import scala.util.matching.Regex;

object ThreadingDemo {
    // db data
    private var originalData: js.Dictionary[js.Any] = null;
    private var demoId: Option[Int] = None;
    private var threadDbIds: Map[Int, Int] = Map(); // maps thread pos to actual db id for saving.

    // GUI state
    private var globalsEditor: js.Dynamic = null; // CodeMirror objects
    private var threadEditors: Map[Int, js.Dynamic] = Map() // Maps pos to CodeMirror objects
    private var assemblyDisplays: Map[Int, js.Dynamic] = Map() // Maps pos to CodeMirror objects
    private var activeMarks = Vector[js.Dynamic](); // marks on the CodeMirrors for the currently running lines.

    // Demo state
    private var demoRunning = false;
    private var awaitingUserSelection = -1; // the pos of the thread that is waiting for user selection before steping. -1 otherwise.
    private var memory: Memory = null // shared by all processes
    private var globals: Thread = null
    private var threads: Map[Int, Thread] = null // Maps pos to threads

    // Playback state
    private var playbackState = Playback.NotPlaying
    private var recordingPlayback: Playback = null; // the playback currently recording.
    private var playingPlayback: Iterator[Step] = null;
    private var playbackLooper: Int = 0; // setInterval handle for the playback.
    private var playbacks: ArrayBuffer[Playback] = ArrayBuffer();


    def main(args: Array[String]): Unit = {
        Tests.test()

        jQ(() => { // On document load.
            loadDemo()
            initGui()
        })
    }
    
    // Loads the demo's data.
    def loadDemo(): Unit = {
        originalData = jsGlobal.originalData.asInstanceOf[js.Dictionary[js.Any]]
        demoId = originalData.get("id").map( _.asInstanceOf[Int] )

        // Load Playbacks
        val playbacksData = originalData("playback").asInstanceOf[js.Array[js.Dictionary[js.Any]]]
        for (playbackData <- playbacksData) {
            val name = playbackData("name").asInstanceOf[String]
            val stepData = playbackData("step").asInstanceOf[js.Array[js.Dictionary[js.Any]]]
            val stepSorted = stepData.sortBy( _("pos").asInstanceOf[Int] )

            val playback = new Playback(name)
            for (step <- stepSorted) {
                val threadPos = step("threadPos").asInstanceOf[Int]
                val wakeThreadPos = step("wakeThreadPos").asInstanceOf[Int] // null casts to 0 which isn't in threads.
                val breakpoint = step("breakpoint").asInstanceOf[Boolean]
                playback.add(threadPos, if (wakeThreadPos == 0) -1 else wakeThreadPos, breakpoint)
            }
            playbacks.addOne(playback)
        }
    }

    // Initializes the GUI.
    def initGui(): Unit = {
        // Load Threads
        val threadsData = Map[Int, js.Dictionary[js.Any]]()
        for (thread <- originalData("thread").asInstanceOf[js.Array[js.Dictionary[js.Any]]]) {
            threadsData(thread("pos").asInstanceOf[Int]) = thread
        }

        threadDbIds = threadsData.map( { case (pos, t) => (pos, t("id").asInstanceOf[Int]) } )

        val globalsCode = if (threadsData.get(0).isDefined) {
            threadsData(0)("code").asInstanceOf[String]
        } else {
            "// Define global variables here.\n"
        }
        globalsEditor = createCodeMirror(jQ("#globals .source-code"), globalsCode)
        globalsEditor.setSize(null, "30vh")
        jQ("#globals .runtime-variables").hide()
        
        for ((pos, thread) <- threadsData.toList.sortBy(_._1) if pos != 0)
            addThread( thread("code").asInstanceOf[String] )
        if (this.threadEditors.size == 0) addThread("// Write a thread here.\n") // start with at least one thread.

        updateControls(); // initialize the control panel buttons and dropdowns.

        jQ("#demo-tags").asInstanceOf[js.Dynamic].select2(literal(
            maximumInputLength = 255,
            multiple = true,
            placeholder = "Tags...",
            // tags = true,
        ));

        jQ("#playback-select").asInstanceOf[js.Dynamic].select2(literal(
            placeholder = "Add a playback...",
            // tags = true,
        ));
        

        // Add event handler to show variable values on hover over.
        jQ("#threads").on("mouseenter", ".variable", (target, event) => {
            val threadId = jQ(target).attr("data-thread").get.toInt
            val content = if (jQ(target).attr("data-varname").isDefined) {
                val varname = jQ(target).attr("data-varname").get
                val (value, typ) = threads(threadId).getVarWithType(varname)
                prettifyVar(typ, varname, value)
            } else {
                val loc = jQ(target).attr("data-loc").get match {
                    case "register" => VarLoc.Register  case "stack" => VarLoc.Stack  case "heap" => VarLoc.Heap
                }
                val addr = jQ(target).attr("data-addr").get
                val (varname, value, typ) = threads(threadId).getVarFromAddr(loc, addr)
                if (varname.isDefined) prettifyVar(typ, varname.get, value) else prettifyVar(typ, value=value)
            }
    
            jsGlobal.tippy(target, literal(
                "content" -> content,
                "allowHTML" -> true,
                "delay" -> 400,
                "trigger" -> "manual", // only trigger from methods.
                "popperOptions" -> literal(
                    "modifiers" -> js.Array(literal(
                        "name" -> "offset",
                        "options" -> literal(
                            "offset" -> js.Array(0, 20),
                        ),
                    ),
                ))
            )).show()
        })
        jQ("#threads").on("mouseleave", ".variable", (target, event) => {
            target.asInstanceOf[js.Dynamic]._tippy.hide()
            target.asInstanceOf[js.Dynamic]._tippy.destroy()
        })

        globalsEditor.on("beforeChange", confirmCodeChange(_, _))
    }

    /**
      * Creates a CodeMirror editor in parentElement. Returns the CodeMirror object.
      * Sets the contents of the mirror to value.
      */
    private def createCodeMirror(parentElement: JQuery, value: String = ""): js.Dynamic = {
        val codeMirrorDiv = jQ(s"<div class='border border-primary'></div>")
        codeMirrorDiv.appendTo(parentElement)

        def indentAll(cm: js.Dynamic, indent: Int): Unit = { // indents a block.
            var start = cm.getCursor("from"); var end = cm.getCursor("to")
            for (lnNum <- start.line.asInstanceOf[Int] to end.line.asInstanceOf[Int]) {
                var ln = cm.getLine(lnNum).asInstanceOf[String].replaceAll("\t", " " * indent) // tab with spaces
                ln = if (indent < 0) s"^ {0,${-indent}}".r.replaceFirstIn(ln, "") else " " * indent + ln
                cm.replaceRange(ln, literal(line = lnNum, ch = 0), literal(line = lnNum, ch = null))
            }
            start.ch = start.ch.asInstanceOf[Int] + indent
            end.ch = end.ch.asInstanceOf[Int] + indent;
            cm.setSelection(start, end)
        }

        val codeMirror = jsGlobal.CodeMirror(codeMirrorDiv.get(0).get, literal(
            "mode" -> "ravel",
            "value" -> value,
            "theme" -> "darcula",
            "indentUnit" -> 4,
            "lineNumbers" -> true,
            "matchBrackets" -> true,
            "autoCloseBrackets" -> true,
            "extraKeys" -> literal(
                    // spaces instead of tabs
                "Tab" -> ( (cm: js.Dynamic) => {
                    if (cm.getCursor("from").line != cm.getCursor("to").line) {
                        indentAll(cm, 4)
                    } else {
                        cm.replaceSelection(" " * cm.getOption("indentUnit").asInstanceOf[Int])
                    }
                }),
                "Shift-Tab" -> ( (cm: js.Dynamic) => indentAll(cm, -4) ),
                "Ctrl-/" -> ( (cm: js.Dynamic) => cm.toggleComment() ),
            )
        ));
        // codeMirror.setSize("100%", "100%")
        return codeMirror;
    }

    /**
     * Creates readonly CodeMirror for MIPS
     */
    private def createAssemblyCodeMirror(parentElement: JQuery, value: String = ""): js.Dynamic = {
        val codeMirrorDiv = jQ(s"<div class='border border-primary'></div>")
        codeMirrorDiv.appendTo(parentElement)

        val codeMirror = jsGlobal.CodeMirror(codeMirrorDiv.get(0).get, literal(
            "mode" -> "mips",
            "value" -> value,
            "theme" -> "darcula",
            "readOnly" -> false,
        ));
        // codeMirror.setSize("100%", "100%")
        return codeMirror;
    }

    /**
      * Adds a thread text box.
      */
    @JSExportTopLevel("addThread")
    def addThread(content: String): Unit = {
        val id = if (threadEditors.size > 0) threadEditors.keys.max + 1 else 1 // the new start at 1. globals is 0.

        val threadColumn = jQ(s"""
            <div id='thread-$id' class="thread col-md pr-2 pb-1">
                <div class="row no-gutters flex-nowrap">
                    <div class="col-md">
                        <div class="row"><div class="col">
                            <button class="btn btn-sm float-right remove-thread" onclick="removeThread($id)" title="Remove Thread">
                                <i class="far fa-trash-alt"></i>
                            </button>
                        </div></div>
                        <div class="row"><div class="col source-code"></div></div>
                    </div>
                    <div class="col-md assembly">
                        <div class="d-flex flex-row-reverse">
                            <button class="btn btn-sm step-button" onclick="stepThread($id)"></button>
                            <div "flex-grow-1">
                                <span class="badge text-right mr-2 thread-state"></span>
                            </div>
                        </div>
                        <div class="row"><div class="col assembly-disp"></div></div>
                    </div>
                </div>
            </div>""")

        threadColumn.appendTo(jQ("#threads"))

        val srcCode = createCodeMirror(threadColumn.find(".source-code"), content)
        srcCode.setSize(null, "50vh")
        threadEditors(id) = srcCode;

        val assembly = createAssemblyCodeMirror(threadColumn.find(".assembly-disp"))
        assembly.setSize(null, "50vh")
        threadColumn.find(".assembly").hide()
        assemblyDisplays(id) = assembly

        srcCode.on("beforeChange", confirmCodeChange(_, _))
    }

    // default arguments don't work with JSExportTopLevel, but overloading does.
    @JSExportTopLevel("addThread")
    def addThread(): Unit = {
        if (!confirmCodeChange()) return; // we only need to confirm when making a new thread.
        addThread("// Write a thread here.\n") 
    }

    /**
      * Removes a thread.
      */
    @JSExportTopLevel("removeThread")
    def removeThread(id: Int): Unit = {
        if (!confirmCodeChange()) return;

        jQ(s"#thread-$id").remove()
        threadEditors.remove(id)
        assemblyDisplays.remove(id)
    }

    /**
     * Run/Stop the demo.
     */
    @JSExportTopLevel("toggleDemo")
    def toggleDemo(): Unit = {
        if (demoRunning) {
            stopDemo()
        } else {
            runDemo()
        }
    }

    /**
      * Puts the demo in the running state.
      */
    @JSExportTopLevel("runDemo")
    def runDemo(): Unit = {
        // get and compile all the code.
        if (demoRunning) stopDemo()
        demoRunning = true

        memory = new Memory()
        var sp = Int.MaxValue;

        val globalsCode = globalsEditor.getValue().asInstanceOf[String]
        try {
            globals = Thread.compileGlobalFrom(globalsCode, memory, sp)
        } catch { case e: Exception =>
            showError(s"In globals:\n${e.getMessage()}")
            stopDemo()
            return
        }

        threads = Map();

        for ((id, editor) <- threadEditors) { // create the threads, add to threads map.
            val code = editor.getValue().asInstanceOf[String]
            sp -= 4 * 1024 * 1024 // leave 4 Mebibytes for each stack.
            try {
                threads(id) = Thread.compileFrom(code, memory, globals.bindings.globals, sp)
            } catch { case e: Exception =>
                showError(s"In thread $id:\n${e.getMessage()}")
                stopDemo()
                return
            }
        }

        globals.execute()

        // Set up the GUI
        jQ("#globals .source-code").hide()
        jQ("#globals .runtime-variables").show()
        jQ(".ravel-error").remove() // clear any error messages since we've compiled successfully.

        // Set up globals
        if (globals.variables.isEmpty) {
            jQ("#globals .runtime-variables").append("No global variables to show here!")
        } else {
            for (column <- globals.variables.grouped(4)) {
                val table = jQ("""
                    <div class="px-1">
                    <table class="table table-striped table-bordered table-hover w-auto">
                        <thead>
                        <tr>
                            <th scope="col text-right">Name</th>
                            <th scope="col">Value</th>
                        </tr>
                        </thead>
                        <tbody></tbody>
                    </table>
                    </div>
                """)
                for ((name, value, typ) <- column) {
                    table.find("tbody").append(jQ(
                        s"""<tr>
                            <td class='text-right'>${prettifyVar(typ, name)}</td>
                            <td data-varname="${name}"></td>
                            </tr>
                    """))
                }
                jQ("#globals .runtime-variables").append(table)
            }
        }

        // Set up threads and assembly.
        for ((id, thread) <- threads) {
            val editor = threadEditors(id)
            editor.setOption("readOnly", true)

            // mark text so we can have popups showing current values
            for (statement <- thread.ast; subStatement <- statement) {
                subStatement match {
                    case variable: RavelId => {
                        val ((ln0, ch0),(ln1, ch1)) = subStatement.src
                        editor.markText(literal(line = ln0, ch = ch0), literal(line = ln1, ch = ch1),
                            literal("className" -> "variable", "attributes" -> literal("data-thread" -> id, "data-varname" -> variable.name)))
                    }
                    case _ => {}
                }
            }

            val assembly = assemblyDisplays(id)
            assembly.setValue(thread.assembly.mkString("\n"))
            jQ(s"#thread-$id .assembly").show()

            for ((instr, line) <- thread.assembly.map( _.toString() ).zipWithIndex) {
                val matches = ArrayBuffer[(Regex.Match, String, String)]()
                // match reg not in parentheses.
                for (m <- """(?<!\()\$[a-z0-9]+""".r.findAllMatchIn(instr)) matches.addOne( (m, "register", m.matched) )
                for (m <- """(-?\d+)\((\$[a-z0-9]+)\)""".r.findAllMatchIn(instr)) 
                    matches.addOne( (m, "stack", m.group(1)) )
                for (m <- """0x\d+""".r.findAllMatchIn(instr)) 
                    matches.addOne( (m, "heap", Integer.parseInt(m.matched.drop(2), 16).toString) )

                for ((m, reg, offset) <- matches)
                    assembly.markText(literal(line = line, ch = m.start), literal(line = line, ch = m.end),
                        literal("className" -> "variable",
                            "attributes" -> literal("data-thread" -> id, "data-loc" -> reg, "data-addr" -> offset)))
            }

        }

        updateControls()
        updateDemo()
    }

    /**
      * Stops running the code.
      */
    @JSExportTopLevel("stopDemo")
    def stopDemo(): Unit = {
        demoRunning = false
        if (playbackState == Playback.Playing) {
            cancelPlayback()
        } else if (playbackState == Playback.Recording) {
            cancelRecord()
        }

        // clear state
        memory = null; globals = null; threads = null

        // Reset the GUI
        jQ("#globals .source-code").show()
        jQ("#globals .runtime-variables").hide()
        jQ("#globals .runtime-variables").empty()

        activeMarks = Vector()

        for ((id, editor) <- threadEditors) {
            jQ(s"#thread-$id .assembly").hide()
            assemblyDisplays(id).setValue("")
            editor.setOption("readOnly", false)
            for (mark <- editor.getAllMarks().asInstanceOf[js.Array[js.Dynamic]]) mark.clear()
        }

        updateControls()
    }

    /**
      * Updates the demo gui, showing current line etc.
      */
    @JSExportTopLevel("updateDemo")
    def updateDemo(): Unit = {
        for ((name, value, typ) <- globals.variables) { // update the global variables.
            jQ(s"#globals .runtime-variables [data-varname='$name']").html(s"${prettifyVar(typ, value=value)}")
        }

        for (mark <- activeMarks) mark.clear() // clear all of the highlight marks.
        activeMarks = Vector()

        for (id <- threads.keys) { // update all the threads.
            val (code, assembly, thread) = (threadEditors(id), assemblyDisplays(id), threads(id))

            if (thread.state != Thread.State.Complete) { // update highlighted code.
                val ((ln0, ch0),(ln1, ch1)) = thread.currentInstr.src;

                val (assmFrom, assmTo) = ( literal(line = thread.pc, ch = 0), literal(line = thread.pc, ch = null) )
                activeMarks :+= assembly.markText(assmFrom, assmTo, literal("className" -> "active-code"))
                // assembly.scrollIntoView(literal("from" -> assmFrom, "to" -> assmTo), 100)

                val (codeFrom, codeTo) = ( literal(line = ln0, ch = ch0), literal(line = ln1, ch = ch1) )
                activeMarks :+= code.markText(codeFrom, codeTo, literal("className" -> "active-code"))
                // code.scrollIntoView(literal("from" -> codeFrom, "to" -> codeTo), 100)
            }

            jQ(s"#thread-$id .thread-state").text(thread.state match { // update thread state indicator.
                case Thread.State.Running => "Running"  case Thread.State.Complete => "Complete"  case Thread.State.Blocked => "Blocked"
            })

            val (text, className) = thread.state match { // update thread state indicator.
                case Thread.State.Running => ("Running", "badge-success")
                case Thread.State.Complete => ("Complete", "badge-secondary")
                case Thread.State.Blocked => ("Blocked", "badge-danger")
            }
            jQ(s"#thread-$id .thread-state").text(text);
            jQ(s"#thread-$id .thread-state").removeClass("badge-success badge-secondary badge-danger");
            jQ(s"#thread-$id .thread-state").addClass(className);


            val stepIcon = "<i class='fas fa-step-forward'></i>"
            if (awaitingUserSelection >= 0) { // update step buttons
                if ( threads(awaitingUserSelection).inputOptions.contains(thread) ) {
                    jQ(s"#thread-$id .step-button").html("<i class='fas fa-play'></i> Wake")
                } else {
                    jQ(s"#thread-$id .step-button").html(stepIcon).prop("disabled", true)
                }
            } else if (thread.state == Thread.State.Complete) {
                jQ(s"#thread-$id .step-button").html(stepIcon).prop("disabled", true)
            } else {
                jQ(s"#thread-$id .step-button").html(stepIcon).prop("disabled", false)
            }
            if (playbackState == Playback.Playing || playbackState == Playback.Paused)
                jQ(s"#thread-$id .step-button").prop("disabled", true); // disable all step buttons if playing.
        }
    }

    /**
     * Updates the control buttons.
     */
    @JSExportTopLevel("updateControls")
    def updateControls(): Unit = {
        if (demoRunning) {
            jQ("#toggle-demo").html("<i class='fas fa-stop text-danger'></i>")
            jQ("#toggle-demo").attr("title", "Stop Demo")
        } else {
            jQ("#toggle-demo").html("<i class='fas fa-play text-success'></i>")
            jQ("#toggle-demo").attr("title", "Run Demo")
        }

        // if (playbackState == Playback.Recording) {
        //     jQ("#record-playback").html("<i class='far fa-stop-circle text-danger'></i> Stop Recording")
        //     jQ("#record-playback").attr("title", "Stop Recording")
        // } else {
        //     jQ("#record-playback").html("<i class='fas fa-record-vinyl text-danger'></i> Record Playback")
        //     jQ("#record-playback").attr("title","Record Playback")
        // }

        if (playbackState == Playback.Playing) {
            jQ("#toggle-playback").html("<i class='fas fa-pause text-warning'></i>")
            jQ("#toggle-playback").attr("title", "Pause Playback")
        } else {
            jQ("#toggle-playback").html("<i class='fas fa-play text-success'></i>")
            jQ("#toggle-playback").attr("title", "Continue Playback")
        }

        // update if playbacks have changed.
        val contents = jQ("#playback-select").find("option").get().map( _.textContent )
        if (!contents.sameElements( playbacks.map( _.name ) )) {
            jQ("#playback-select").empty()
            for ((playback, i) <- playbacks.zipWithIndex) { // recreate select.
                jQ("#playback-select").append(s"<option value='${i}'>${playback.name}</option>")
            }
        }


        // Disable buttons
        val recording = playbackState == Playback.Recording
        val playing = playbackState == Playback.Playing
        val paused = playbackState == Playback.Paused
        val normal = playbackState == Playback.NotPlaying

        jQ("#add-thread").prop("disabled", demoRunning)
        jQ("#toggle-demo").prop("disabled", !normal)
        jQ("#play-playback").prop("disabled", !normal || playbacks.isEmpty)
        jQ("#playback-select").prop("disabled", !normal)
        jQ("#remove-playback").prop("disabled", !normal || playbacks.isEmpty)
        jQ("#record-playback").prop("disabled", !normal )
               
        jQ(".remove-thread").prop("disabled", demoRunning)
        if (!(normal || recording)) // only set if disabled, since updateDemo can leave the buttons disabled.
            jQ(".step-button").prop("disabled", true)

        // Show/Hide controlse
        jQ("#playback-controls").toggle( playing || paused )
        jQ("#record-controls").toggle( recording )
    }

    /**
     * steps the thread with the given id.
     */
    @JSExportTopLevel("stepThread")
    def stepThread(id: Int): Unit = {
        val thread = threads(id)

        try {
            if (awaitingUserSelection >= 0) {
                threads(awaitingUserSelection).step(thread) // we can step and tell it to wake the thread we clicked.
                if (playbackState == Playback.Recording) recordingPlayback.add(awaitingUserSelection, id)
                awaitingUserSelection = -1
            } else if (thread.state == Thread.State.Running) {
                if (thread.inputOptions.isEmpty) {
                    thread.step()
                    if (playbackState == Playback.Recording) recordingPlayback.add(id)
                } else if (thread.inputOptions.size == 1) { // only one option, so we don't need to ask the user.
                    val wakeThread = thread.inputOptions.head
                    thread.step(wakeThread)
                    val wakeId = threads.find( (pair) => pair._2 == wakeThread ).get._1;
                    if (playbackState == Playback.Recording) recordingPlayback.add(id, wakeId)
                } else {
                    // we can't actually step until next click, which will choose the thread to wake.
                    awaitingUserSelection = id; 
                }
            }
            updateDemo()
        } catch { case e: Exception =>
            showError(s"Runtime Error:\n${e.getMessage()}")
            stopDemo()
        }
    }

    /**
     * Prints out a dump of memory.
     */
    @JSExportTopLevel("memoryDump")
    def memoryDump(): Unit = {
        println(memory);
    }
    
    /**
     * Displays an error notification.
     */
    def showError(message: String): Unit = {
        jQ(".ravel-error").remove() // remove any previous errors.
        jQ("#threading-demo").prepend(jQ(s"""
            <div class="alert alert-danger ravel-error" role="alert" data-toggle="collapse" href="#collapseExample">
                <button type="button" class="close" data-dismiss="alert">
                    <span aria-hidden="true">&times;</span>
                </button>
                <p id="collapseExample" class="collapse" style="white-space: pre-line">$message</p>
            </div>
        """))
    }

    /**
     * Shows a confirm dialog if playbacks are non-empty, and clears the playbacks if confirmed.
     * If given a codeMirror instance and a codemirror change object it will
     * cancel the change in the CodeMirror instance.
     */
    def confirmCodeChange(cm: js.Dynamic = null, changeObj: js.Dynamic = null): Boolean = {
        if (playbacks.nonEmpty) {
            if (dom.window.confirm("Changing the code will invalidate your playbacks! Are you sure?")) {
                playbacks.clear()
                updateControls()
                return true;
            } else {
                if (changeObj != null) changeObj.cancel()
                return false;
            }
        } else {
            return true;
        }
    }

    /**
     * Returns a meaningful version a variable value based on type.
     */
    def getValStr(typ: RavelType.Value, value: Int): String = {
        return typ match {
            case RavelType.Int     => s"$value"
            case RavelType.Bool    => if (value == 0) "false" else "true"
            case RavelType.Mutex   => if (value == 0) "open"  else "held"
            case RavelType.CondVar => if (value == 0) "unset" else "set"
            case RavelType.Sem     => s"$value"
        }
    }


    /**
     * Returns an syntax highlighted version of a variable declaration and/or value.
     * Eg. int i = 0 or bool b = false or false or bool b
     */
    def prettifyVar(typ: RavelType.Value, name: String = null, value: Integer = null): String = {
        // hack to make it use the same styling that CodeMirror uses by stealing the CodeMirror classes..
        var s = ""
        if (name != null) {
            s += s"<span class='cm-type'>${RavelType.toString(typ)}</span> <span class='cm-variable'>${name}</span>"
            if (value != null) s += s" <span class='cm-operator'>=</span>"
        }
        if (value != null) {
            val valClass = if (typ == RavelType.Int) "cm-number" else "cm-atom"
            s += s" <span class='$valClass'>${getValStr(typ, value)}</span>"
        }
        return s"<span class='cm-s-${globalsEditor.getOption("theme")}'>$s</span>";
    }

    /**
     * Saves the demo model.
     */
    @JSExportTopLevel("save")
    def save(): Unit = {
        // Extract the data
        var data = js.Dictionary[js.Any](
            "demo[id]" -> ( if (demoId.isDefined) demoId.get else () ),
            "demo[title]" -> jQ("#demo-title").value().asInstanceOf[String],
            "demo[visibility]" -> ( if (jQ("#demo-visibility").prop("checked").asInstanceOf[Boolean]) 1 else 0 ),
            "demo[description]" -> jQ("#demo-description").value().asInstanceOf[String],
        )

        // Globals is pos 0. We re-index the values so they are sequential.
        val editors = Vector( globalsEditor ) ++ threadEditors.values
        for ((editor, pos) <- editors.zipWithIndex) {
            data(s"demo[thread][$pos][id]") = if (threadDbIds.isDefinedAt(pos)) threadDbIds(pos) else () // unit
            data(s"demo[thread][$pos][pos]") = pos
            data(s"demo[thread][$pos][code]") = editor.getValue()
        }

        for ((playback, pPos) <- playbacks.zipWithIndex) {
            data(s"demo[playback][$pPos][name]") = playback.name
            for ((step, sPos) <- playback.zipWithIndex) {
                data(s"demo[playback][$pPos][step][$sPos][pos]") = sPos
                data(s"demo[playback][$pPos][step][$sPos][threadPos]") = step.thread
                data(s"demo[playback][$pPos][step][$sPos][wakeThreadPos]") = if (step.wakeThread.isDefined)
                                                                                step.wakeThread.get else ()
                data(s"demo[playback][$pPos][step][$sPos][breakpoint]") = step.breakpoint
            }
        }

        val tags = jQ("#demo-tags").asInstanceOf[js.Dynamic].select2("data").asInstanceOf[js.Array[js.Dynamic]]
        for ((tag, i) <- tags.zipWithIndex) {
            data(s"demo[DemoTag][$i][TagName]") = tag.text.asInstanceOf[String];
        }

        // jsGlobal.console.log(data);
        jQ.ajax(JQueryAjaxSettings(
            url = "/Multithreading/Save",
            method = "POST",
            data = data,
            dataType = "json",
            success = (data, _, _) => {
                demoId = Some(data.asInstanceOf[js.Dictionary[js.Any]]("id").asInstanceOf[Int])
                if (dom.window.location.search != s"?id=${demoId.get}") {
                    // dom.window.location.replace(s"demo?id=${demoId.get}")
                    dom.window.history.replaceState("", "", s"demo?id=${demoId.get}")
                }
                println("Saved!")
            },
            error = (jqXHR: JQueryXHR, _: String) => {
                if (jqXHR.asInstanceOf[js.Dynamic].status.asInstanceOf[Int] == 401) {
                    jQ("#login-modal").asInstanceOf[js.Dynamic].modal("show")
                }
            },
        ))
      
    }


    /**
     * Records a playback.
     */
    @JSExportTopLevel("record")
    def record(): Unit = {
        runDemo()
        playbackState = Playback.Recording;
        recordingPlayback = new Playback()
        updateControls()
    }

    /**
     * stops recording a playback.
     */
    @JSExportTopLevel("stopRecord")
    def stopRecord(): Unit = {
        jQ("#playback-name-modal").asInstanceOf[js.Dynamic].modal("show")
        jQ("#playback-name-modal button").one("click", (target, event) => {
            playbackState = Playback.NotPlaying;

            recordingPlayback.name = jQ("#playback-name").value().asInstanceOf[String]
            playbacks.addOne(recordingPlayback)
            recordingPlayback = null;

            jQ("#playback-name").value(""); // reset the text box for next time.
            updateControls()
        })
    }

    /**
     * Toggle recording the playback.
     */
    @JSExportTopLevel("toggleRecord")
    def toggleRecord(): Unit = {
        if (playbackState == Playback.Recording) {
            stopRecord()
        } else {
            record()
        }
    }

    /**
     * cancels recording a playback.
     */
    @JSExportTopLevel("cancelRecord")
    def cancelRecord(): Unit = {
        playbackState = Playback.NotPlaying;
        recordingPlayback = null;
        updateControls()
    }

    /**
     * Adds a breakpoint to the playback.
     */
    @JSExportTopLevel("addBreakpoint")
    def addBreakpoint(): Unit = {
        recordingPlayback.addBreakpoint();
    }

    /**
     * Removes the selected playback.
     */
    @JSExportTopLevel("removePlayback")
    def removePlayback(): Unit = {
        val index = jQ("#playback-select").value().asInstanceOf[String].toInt
        playbacks.remove(index)
        updateControls()
    }



    /**
     * Plays the selected playback 
     */
    @JSExportTopLevel("playPlayback")
    def playPlayback(): Unit = {
        runDemo()
        val index = jQ("#playback-select").value().asInstanceOf[String].toInt
        playingPlayback = playbacks(index).iterator
        resumePlayback()
    }

    /**
     * Resumes the playback. 
     */
    @JSExportTopLevel("resumePlayback")
    def resumePlayback(): Unit = {
        playbackState = Playback.Playing
        updateControls()
        playbackLooper = dom.window.setInterval( () => {
            if (playingPlayback.hasNext) {
                val Step(threadId, wakeId, breakpoint) = playingPlayback.next()
                threads(threadId).step( if (wakeId.isDefined) threads(wakeId.get) else null )
                updateDemo()
                if (breakpoint) pausePlayback()
            } else {
                cancelPlayback() // finished
            }
        }, 500)
    }

    /**
     * pauses the playing playback
     */
    @JSExportTopLevel("pausePlayback")
    def pausePlayback(): Unit = {
        playbackState = Playback.Paused
        dom.window.clearInterval(playbackLooper)
        playbackLooper = 0;
        updateControls()
    }

    /**
     * toggles pause/play on the current playback
     */
    @JSExportTopLevel("togglePlayback")
    def togglePlayback(): Unit = {
        if (playbackState == Playback.Paused) {
            resumePlayback()
        } else {
            pausePlayback()
        }
    }
  
    /**
     * stops the playing playback
     */
    @JSExportTopLevel("cancelPlayback")
    def cancelPlayback(): Unit = {
        pausePlayback()
        playbackState = Playback.NotPlaying
        playingPlayback = null;
        updateControls()
    }
}

