// Generated automatically by nearley, version 2.19.2
// http://github.com/Hardmath123/nearley
(function () {
function id(x) { return x[0]; }

const lexer = moo.compile({
    WS:           {match: /[ \t\n]+/, lineBreaks: true, value: x => null},
    comment:      {match: /\/\/.*?$/, value: x => null},
    multiComment: {match: /\/\*[^]*?\*\//, lineBreaks: true, value: x => null},
    identifier:   {match: /[a-zA-Z_][a-zA-Z_0-9]*/, type: moo.keywords({
        keyword:  [
          "if", "else", "while", "for", "int", "bool", "mutex", "cond_var", "sem", "true", "false",
          "lock", "unlock", "cond_wait", "cond_signal", "sem_wait", "sem_post", "sem_create"
    ]})},  
    number: /[0-9]+/,
    symbol: [
        "++", "--", "+", "-", "*", "/", "%", "==", "!=", "<", ">", "<=", ">=", "&&", "||", "!",
        "=", "+=", "-=", "*=", "/=", "%=", "(", ")", "{", "}", ";",
    ],
});

// Modify the next method to remove any token with a null value.
const mooNext = lexer.next;
lexer.next = () => { 
    let tok;
    while ((tok = mooNext.call(lexer)) && tok.value === null) {}
    return tok;
};


// helper functions and post processors.

// Returns the selection that contains all the characters of first token or parsed result through last.
// as a [[start_line, start_char], [end_line, end_char]]. lines and chars start at 0, and the selection
// continues until end_line,end_char (it doesn't include end_line,end_char)
function src(first, last = null) {
    // console.log("first:")
    // console.log(first)
    // console.log("last:")
    // console.log(last)

    last = last || first // If last is null, just use first for both ends.
    // Check if first or last or already ast and return the src directly, else get it from the token.
    var firstPt = (first.src) ? first.src[0] : [first.line - 1, first.col - 1]
    var lastPt  = (last.src)  ? last.src[1]  : [last.line - 1, last.col + last.text.length - 1]  
    return [firstPt, lastPt]
}

const binaryOp = ([a, [op], b]) => ({type:op.text, a:a, b:b, src:src(a, b)}); 
var grammar = {
    Lexer: lexer,
    ParserRules: [
    {"name": "program$ebnf$1", "symbols": []},
    {"name": "program$ebnf$1", "symbols": ["program$ebnf$1", "statement"], "postprocess": function arrpush(d) {return d[0].concat([d[1]]);}},
    {"name": "program", "symbols": ["program$ebnf$1"], "postprocess": ([statements]) => statements.filter(s => s !== null)},
    {"name": "codeBlock", "symbols": ["statement"], "postprocess": ([s]) => s === null ? [] : [s]},
    {"name": "codeBlock", "symbols": [{"literal":"{"}, "program", {"literal":"}"}], "postprocess": ([ , p, ]) => p},
    {"name": "statement$subexpression$1", "symbols": ["if"]},
    {"name": "statement$subexpression$1", "symbols": ["while"]},
    {"name": "statement$subexpression$1", "symbols": ["for"]},
    {"name": "statement", "symbols": ["statement$subexpression$1"], "postprocess": ([[s]]) => s},
    {"name": "statement$subexpression$2", "symbols": ["procedure"]},
    {"name": "statement$subexpression$2", "symbols": ["initialization"]},
    {"name": "statement$subexpression$2", "symbols": ["expression"]},
    {"name": "statement", "symbols": ["statement$subexpression$2", {"literal":";"}], "postprocess": ([[s], c]) => ({...s, src:src(s, c)})},
    {"name": "statement", "symbols": [{"literal":";"}], "postprocess": d => null},
    {"name": "if$ebnf$1$subexpression$1", "symbols": [{"literal":"else"}, "codeBlock"], "postprocess": ([ , d]) => d},
    {"name": "if$ebnf$1", "symbols": ["if$ebnf$1$subexpression$1"], "postprocess": id},
    {"name": "if$ebnf$1", "symbols": [], "postprocess": function(d) {return null;}},
    {"name": "if", "symbols": [{"literal":"if"}, {"literal":"("}, "expression", {"literal":")"}, "codeBlock", "if$ebnf$1"], "postprocess": ([f, , cond, rp, ifBody, elseBody]) => ({type:"if", cond:cond, ifCase:ifBody, elseCase:elseBody || [], src:src(f, rp)})},
    {"name": "while", "symbols": [{"literal":"while"}, {"literal":"("}, "expression", {"literal":")"}, "codeBlock"], "postprocess": ([w, , cond, rp, body]) => ({type:"while", cond:cond, body:body, src:src(w, rp)})},
    {"name": "for", "symbols": [{"literal":"for"}, {"literal":"("}, "initialization", {"literal":";"}, "expression", {"literal":";"}, "expression", {"literal":")"}, "codeBlock"], "postprocess": ([f, , init, , cond, , inc, rp, body]) => ({type:"for", init:init, cond:cond, inc:inc, body:body, src:src(f, rp)})},
    {"name": "procedure$subexpression$1", "symbols": [{"literal":"lock"}]},
    {"name": "procedure$subexpression$1", "symbols": [{"literal":"unlock"}]},
    {"name": "procedure$subexpression$1", "symbols": [{"literal":"cond_wait"}]},
    {"name": "procedure$subexpression$1", "symbols": [{"literal":"cond_signal"}]},
    {"name": "procedure$subexpression$1", "symbols": [{"literal":"sem_wait"}]},
    {"name": "procedure$subexpression$1", "symbols": [{"literal":"sem_post"}]},
    {"name": "procedure", "symbols": ["procedure$subexpression$1", {"literal":"("}, "identifier", {"literal":")"}], "postprocess": ([[proc], , param, rp]) => ({type:proc.text, param: param, src:src(proc, rp)})},
    {"name": "initialization$ebnf$1$subexpression$1", "symbols": [{"literal":"="}, "expression"], "postprocess": ([ , d]) => d},
    {"name": "initialization$ebnf$1", "symbols": ["initialization$ebnf$1$subexpression$1"], "postprocess": id},
    {"name": "initialization$ebnf$1", "symbols": [], "postprocess": function(d) {return null;}},
    {"name": "initialization", "symbols": ["type", "identifier", "initialization$ebnf$1"], "postprocess": ([type, id, rhs]) => ({type:"init", varType:type.name, name:id.name, rhs: rhs, src:src(type, rhs || id)})},
    {"name": "expression$subexpression$1", "symbols": ["assignment"]},
    {"name": "expression$subexpression$1", "symbols": ["boolSum"]},
    {"name": "expression", "symbols": ["expression$subexpression$1"], "postprocess": ([[e]]) => e},
    {"name": "assignment$subexpression$1", "symbols": [{"literal":"="}]},
    {"name": "assignment$subexpression$1", "symbols": [{"literal":"+="}]},
    {"name": "assignment$subexpression$1", "symbols": [{"literal":"-="}]},
    {"name": "assignment$subexpression$1", "symbols": [{"literal":"*="}]},
    {"name": "assignment$subexpression$1", "symbols": [{"literal":"/="}]},
    {"name": "assignment$subexpression$1", "symbols": [{"literal":"%="}]},
    {"name": "assignment", "symbols": ["identifier", "assignment$subexpression$1", "expression"], "postprocess": ([id, [op], rhs]) => ({type:op.text, name:id.name, rhs:rhs, src:src(id, rhs)})},
    {"name": "function$subexpression$1", "symbols": [{"literal":"sem_create"}]},
    {"name": "function", "symbols": ["function$subexpression$1", {"literal":"("}, "expression", {"literal":")"}], "postprocess": ([[name], , param, rp]) => ({type:name.text, param: param, src:src(name, rp)})},
    {"name": "boolSum$subexpression$1", "symbols": [{"literal":"||"}]},
    {"name": "boolSum", "symbols": ["boolSum", "boolSum$subexpression$1", "boolProduct"], "postprocess": binaryOp},
    {"name": "boolSum", "symbols": ["boolProduct"], "postprocess": id},
    {"name": "boolProduct$subexpression$1", "symbols": [{"literal":"&&"}]},
    {"name": "boolProduct", "symbols": ["boolProduct", "boolProduct$subexpression$1", "comparison"], "postprocess": binaryOp},
    {"name": "boolProduct", "symbols": ["comparison"], "postprocess": id},
    {"name": "comparison$subexpression$1", "symbols": [{"literal":"=="}]},
    {"name": "comparison$subexpression$1", "symbols": [{"literal":"!="}]},
    {"name": "comparison$subexpression$1", "symbols": [{"literal":"<"}]},
    {"name": "comparison$subexpression$1", "symbols": [{"literal":">"}]},
    {"name": "comparison$subexpression$1", "symbols": [{"literal":"<="}]},
    {"name": "comparison$subexpression$1", "symbols": [{"literal":">="}]},
    {"name": "comparison", "symbols": ["comparison", "comparison$subexpression$1", "sum"], "postprocess": binaryOp},
    {"name": "comparison", "symbols": ["sum"], "postprocess": id},
    {"name": "sum$subexpression$1", "symbols": [{"literal":"+"}]},
    {"name": "sum$subexpression$1", "symbols": [{"literal":"-"}]},
    {"name": "sum", "symbols": ["sum", "sum$subexpression$1", "product"], "postprocess": binaryOp},
    {"name": "sum", "symbols": ["product"], "postprocess": id},
    {"name": "product$subexpression$1", "symbols": [{"literal":"*"}]},
    {"name": "product$subexpression$1", "symbols": [{"literal":"/"}]},
    {"name": "product$subexpression$1", "symbols": [{"literal":"%"}]},
    {"name": "product", "symbols": ["product", "product$subexpression$1", "prefixOps"], "postprocess": binaryOp},
    {"name": "product", "symbols": ["prefixOps"], "postprocess": id},
    {"name": "prefixOps$subexpression$1", "symbols": [{"literal":"+"}]},
    {"name": "prefixOps$subexpression$1", "symbols": [{"literal":"-"}]},
    {"name": "prefixOps", "symbols": ["prefixOps$subexpression$1", "prefixOps"], "postprocess": ([[op], a]) => ({type:"unary"+op.text, a:a, src:src(op, a)})},
    {"name": "prefixOps", "symbols": [{"literal":"!"}, "prefixOps"], "postprocess": ([op, a]) => ({type:"!", a:a, src:src(op, a)})},
    {"name": "prefixOps", "symbols": ["postfixOps"], "postprocess": id},
    {"name": "postfixOps$subexpression$1", "symbols": [{"literal":"++"}]},
    {"name": "postfixOps$subexpression$1", "symbols": [{"literal":"--"}]},
    {"name": "postfixOps", "symbols": ["identifier", "postfixOps$subexpression$1"], "postprocess": ([id, [op]])=> ({type:op.text, name:id.name, src:src(id, op)})},
    {"name": "postfixOps", "symbols": ["term"], "postprocess": id},
    {"name": "term", "symbols": [{"literal":"("}, "expression", {"literal":")"}], "postprocess": ([lp, e, rp]) => ({...e, src:src(lp, rp)})},
    {"name": "term", "symbols": ["int"], "postprocess": id},
    {"name": "term", "symbols": ["bool"], "postprocess": id},
    {"name": "term", "symbols": ["identifier"], "postprocess": id},
    {"name": "term", "symbols": ["function"], "postprocess": id},
    {"name": "identifier", "symbols": [(lexer.has("identifier") ? {type: "identifier"} : identifier)], "postprocess": ([id])  => ({type:"id", name:id.text, src:src(id)})},
    {"name": "int", "symbols": [(lexer.has("number") ? {type: "number"} : number)], "postprocess": ([i])   => ({type:"int", value:parseInt(i), src:src(i)})},
    {"name": "bool$subexpression$1", "symbols": [{"literal":"true"}]},
    {"name": "bool$subexpression$1", "symbols": [{"literal":"false"}]},
    {"name": "bool", "symbols": ["bool$subexpression$1"], "postprocess": ([[b]]) => ({type:"bool", value: (b.text == "true"), src:src(b)})},
    {"name": "type$subexpression$1", "symbols": [{"literal":"int"}]},
    {"name": "type$subexpression$1", "symbols": [{"literal":"bool"}]},
    {"name": "type$subexpression$1", "symbols": [{"literal":"mutex"}]},
    {"name": "type$subexpression$1", "symbols": [{"literal":"cond_var"}]},
    {"name": "type$subexpression$1", "symbols": [{"literal":"sem"}]},
    {"name": "type", "symbols": ["type$subexpression$1"], "postprocess": ([[t]]) => ({type:"type", name:t.text, src:src(t)})}
]
  , ParserStart: "program"
}
if (typeof module !== 'undefined'&& typeof module.exports !== 'undefined') {
   module.exports = grammar;
} else {
   window.grammar = grammar;
}
})();
