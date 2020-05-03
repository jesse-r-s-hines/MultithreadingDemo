@{%
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
%}

@lexer lexer



### Statements
program -> # A sequence of statements
  statement:* {% ([statements]) => statements.filter(s => s !== null) %} # remove any "empty" statements

codeBlock -> # A sequence of statements. Either 0 or 1 statement, or any number in {}. Evaluates to an array.
    statement       {% ([s]) => s === null ? [] : [s] %}
  | "{" program "}" {% ([ , p, ]) => p %}

statement -> # Any statement in a program.
    (if | while | for)           {% ([[s]]) => s %}
  | (procedure | initialization | expression) ";" {% ([[s], c]) => ({...s, src:src(s, c)}) %}
  | ";"                          {% d => null %} # the empty statement

if ->
    "if" "(" expression ")" codeBlock ("else" codeBlock {% ([ , d]) => d %}):?
    {% ([f, , cond, rp, ifBody, elseBody]) => ({type:"if", cond:cond, ifCase:ifBody, elseCase:elseBody || [], src:src(f, rp)}) %}

while ->
    "while" "(" expression ")" codeBlock
    {% ([w, , cond, rp, body]) => ({type:"while", cond:cond, body:body, src:src(w, rp)}) %}
  
for ->
    "for" "(" initialization ";" expression ";" expression ")" codeBlock
     {% ([f, , init, , cond, , inc, rp, body]) => ({type:"for", init:init, cond:cond, inc:inc, body:body, src:src(f, rp)}) %}

procedure ->
   ("lock" | "unlock" | "cond_wait" | "cond_signal" | "sem_wait" | "sem_post") "(" identifier ")"
   {% ([[proc], , param, rp]) => ({type:proc.text, param: param, src:src(proc, rp)}) %}

initialization -> # Initialize a variable, with an optional value.
    type identifier ("=" expression {% ([ , d]) => d %}):?
    {% ([type, id, rhs]) => ({type:"init", varType:type.name, name:id.name, rhs: rhs, src:src(type, rhs || id)}) %} 

### Expressions

expression -> # Anything that returns a value.
    (assignment | boolSum) {% ([[e]]) => e %}

assignment -> # Assign a value to a variable.
    identifier ("=" | "+=" | "-=" | "*=" | "/=" | "%=") expression
    {% ([id, [op], rhs]) => ({type:op.text, name:id.name, rhs:rhs, src:src(id, rhs)}) %}

function -> # Function that returns a value.
    ("sem_create") "(" expression ")" {% ([[name], , param, rp]) => ({type:name.text, param: param, src:src(name, rp)}) %}

# Operators. Lower precedence operators are defined first, and then chain down.

boolSum -> # sequence of 1 or more &&s, ||ed together.
    boolSum ("||") boolProduct {% binaryOp %}
  | boolProduct {% id %}

boolProduct -> # sequence of 1 or more comparisons, &&ed together. && binds more tightly than ||.
    boolProduct ("&&") comparison {% binaryOp %}
  | comparison {% id %}

comparison -> # comparison of 1 or more sums
    comparison ("==" | "!=" | "<" | ">" | "<=" | ">=") sum {% binaryOp %}
  | sum {% id %}

sum -> # sum of 1 or more products
    sum ("+"|"-") product {% binaryOp %}
  | product {% id %}

product -> # product of 1 or more prefixOps
     product ("*"|"/"|"%") prefixOps {% binaryOp %}
   | prefixOps {% id %}

prefixOps -> # A postfixOps with 0 or more prefix ops.
    ("+"|"-") prefixOps {% ([[op], a]) => ({type:"unary"+op.text, a:a, src:src(op, a)}) %}
  | "!"       prefixOps {% ([op, a]) => ({type:"!", a:a, src:src(op, a)}) %}
  | postfixOps {% id %}

postfixOps -> # A term with 0 or more postfix ops
    identifier ("++" | "--") {% ([id, [op]])=> ({type:op.text, name:id.name, src:src(id, op)}) %}
  | term {% id %}

term -> # a literal, variable, or parentheses.
    "(" expression ")" {% ([lp, e, rp]) => ({...e, src:src(lp, rp)}) %}
  | int        {% id %}
  | bool       {% id %}
  | identifier {% id %}
  | function   {% id %}

identifier -> %identifier {% ([id])  => ({type:"id", name:id.text, src:src(id)}) %}
int  -> %number           {% ([i])   => ({type:"int", value:parseInt(i), src:src(i)}) %}
bool -> ("true"|"false")  {% ([[b]]) => ({type:"bool", value: (b.text == "true"), src:src(b)}) %}
type -> ("int" | "bool" | "mutex" | "cond_var" | "sem") {% ([[t]]) => ({type:"type", name:t.text, src:src(t)}) %}