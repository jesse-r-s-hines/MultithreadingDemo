/**
 * Tests the parser
 */
function test() {
    _testEquals();
    _testArithmetic();
    _testBooleanLogic();
    _testVariables();
    _testStatements();
    _testPrograms();
}

// Tests

function _testArithmetic() {
    assertEqual("2;",     {type:"int", value:2}  );
    assertEqual(" 20 ;",  {type:"int", value:20} );
    assertEqual("+2;",    {type:"unary+", a:{type:"int", value:2}} );
    assertEqual("+ 2;",   {type:"unary+", a:{type:"int", value:2}} );
    assertEqual("-553;",  {type:"unary-", a:{type:"int", value:553}} );
    assertEqual("- 1;",   {type:"unary-", a:{type:"int", value:1}} );
    assertEqual("2+3;",   {type:"+", a:{type:"int", value:2}, b:{type:"int", value:3}} );
    assertEqual("2 + 2;", {type:"+", a:{type:"int", value:2}, b:{type:"int", value:2}} );
    assertEqual("5 - 2;", {type:"-", a:{type:"int", value:5}, b:{type:"int", value:2}} );
    assertEqual("5 -2;",  {type:"-", a:{type:"int", value:5}, b:{type:"int", value:2}} );
    assertEqual("6  *2;", {type:"*", a:{type:"int", value:6}, b:{type:"int", value:2}} );
    assertEqual("7/3;",   {type:"/", a:{type:"int", value:7}, b:{type:"int", value:3}} );
    assertEqual("8 % 2;", {type:"%", a:{type:"int", value:8}, b:{type:"int", value:2}} );
    assertEqual("(1);",   {type:"int", value:1} );
    assertEqual("( 1 );", {type:"int", value:1} );
    assertEqual("2 + 2 * 4;",
        {type:"+", a:{type:"int", value:2}, b:{type:"*", a:{type:"int", value:2}, b:{type:"int", value:4}}}
    );
    assertEqual("2 * 3 + 4;",
        {type:"+",
            a:{type:"*", a:{type:"int", value:2}, b:{type:"int", value:3}},
            b:{type:"int", value:4}}
    );
    assertEqual("1 * 2 + 2 / 3;",
        {type:"+",
            a:{type:"*", a:{type:"int", value:1}, b:{type:"int", value:2}},
            b:{type:"/", a:{type:"int", value:2}, b:{type:"int", value:3}}}
    );
    assertEqual("1 * (2 + 2) / 3;",
        {type:"/",
            a:{type:"*", a:{type:"int", value:1}, b:{type:"+", a:{type:"int", value:2}, b:{type:"int", value:2}}},
            b:{type:"int", value:3}}
    );
    assertEqual("(2 + 2) / 4;",
        {type:"/", a:{type:"+", a:{type:"int", value:2}, b:{type:"int", value:2}}, b:{type:"int", value:4}}
    );
    assertEqual("((2 * 4)) - -4;",
        {type:"-",
            a:{type:"*", a:{type:"int", value:2}, b:{type:"int", value:4}},
            b:{type:"unary-", a:{type:"int", value:4}}}
    );    
    assertEqual("2 + 3 - 1 * 2;",
        {type:"-",
            a:{type:"+", a:{type:"int", value:2}, b:{type:"int", value:3}},
            b:{type:"*", a:{type:"int", value:1}, b:{type:"int", value:2}}}
    );
    assertEqual("2 + +3;",
        {type:"+", a:{type:"int", value:2}, b:{type:"unary+", a:{type:"int", value:3}}}
    );
    assertEqual("4 + + 2;",
        {type:"+", a:{type:"int", value:4}, b:{type:"unary+", a:{type:"int", value:2}}}
    );
    assertEqual("2 + -3;",
        {type:"+", a:{type:"int", value:2}, b:{type:"unary-", a:{type:"int", value:3}}}
    );
    assertEqual("2 + - 3;",
        {type:"+", a:{type:"int", value:2}, b:{type:"unary-", a:{type:"int", value:3}}}
    );
    assertEqual("+ - + 2;",
        {type:"unary+", a:{type:"unary-", a:{type:"unary+", a:{type:"int", value:2}}}}
    );
    assertEqual("2 +- 3;",
        {type:"+", a:{type:"int", value:2}, b:{type:"unary-", a:{type:"int", value:3}}}
    );
    assertEqual("2 -+3;",
        {type:"-", a:{type:"int", value:2}, b:{type:"unary+", a:{type:"int", value:3}}}
    );

    assertFails("2++ 4");
    assertFails("2 -- 4");
    assertFails("()");
}

function _testBooleanLogic() {
    assertEqual("true;",  {type:"bool", value:true}  );
    assertEqual("false;", {type:"bool", value:false} );
    
    assertEqual("true && false;",
        {type:"&&", a:{type:"bool", value:true}, b:{type:"bool", value:false}}
    );
    assertEqual("true || false;",
        {type:"||", a:{type:"bool", value:true}, b:{type:"bool", value:false}}
    );
    assertEqual("true == false;",
        {type:"==", a:{type:"bool", value:true}, b:{type:"bool", value:false}}
    );
    assertEqual("12 == 12;",  {type:"==", a:{type:"int", value:12}, b:{type:"int", value:12}} );
    assertEqual("5 != true;", {type:"!=", a:{type:"int", value:5}, b:{type:"bool", value:true}} );
    assertEqual("2 < 5;",     {type:"<", a:{type:"int", value:2}, b:{type:"int", value:5}}  );
    assertEqual("1 > 3;",     {type:">", a:{type:"int", value:1}, b:{type:"int", value:3}}  );
    assertEqual("2 <= 5;",    {type:"<=", a:{type:"int", value:2}, b:{type:"int", value:5}} );
    assertEqual("1 >= 2;",    {type:">=", a:{type:"int", value:1}, b:{type:"int", value:2}} );
    assertEqual("5 != 3;",    {type:"!=", a:{type:"int", value:5}, b:{type:"int", value:3}} );
    assertEqual("! true;",    {type:"!", a:{type:"bool", value:true}} );
    assertEqual("!true == false;",
        {type:"==", a:{type:"!", a:{type:"bool", value:true}}, b:{type:"bool", value: false}}
    );

    assertEqual("5 + 3 == 4 + 4;",
        {type:"==",
            a:{type:"+", a:{type:"int", value:5}, b:{type:"int", value:3}},
            b:{type:"+", a:{type:"int", value:4}, b:{type:"int", value:4}}}
    );
    assertEqual("1 != 4 && 2 > 6;",
        {type:"&&",
            a:{type:"!=", a:{type:"int", value:1}, b:{type:"int", value:4}},
            b:{type:">", a:{type:"int", value:2}, b:{type:"int", value:6}}}
    );
    assertEqual("true && !true || 3 % 2 < 5 && false;",
        {type:"||",
            a:{type:"&&", a:{type:"bool", value:true}, b:{type:"!", a:{type:"bool", value:true}}},
            b:{type:"&&",
                a:{type:"<", a:{type:"%", a:{type:"int", value:3}, b:{type:"int", value:2}}, b:{type:"int", value:5}},
                b:{type:"bool", value:false}}}
    ); 
    assertEqual("true && (true || true) && false;",
        {type:"&&",
            a:{type:"&&",
                a:{type:"bool", value:true},
                b:{type:"||", a:{type:"bool", value:true}, b:{type:"bool", value:true}}},
            b:{type:"bool", value:false}}
    );    
}

function _testVariables() {
    assertEqual("hello;",     {type:"id", name:"hello"} );
    assertEqual("hello12;",   {type:"id", name:"hello12"} );
    assertEqual("_;",         {type:"id", name:"_"} );
    assertEqual("__he_llo_;", {type:"id", name:"__he_llo_"} );
    assertEqual("whilevar;",  {type:"id", name:"whilevar"} );

    assertFails("0hello");
    assertFails("int");
    assertFails("if");
   
    assertEqual("x &&y;",      {type:"&&", a:{type:"id", name:"x"}, b:{type:"id", name:"y"}} );
    assertEqual("myVar234+5;", {type:"+", a:{type:"id", name:"myVar234"}, b:{type:"int", value:5}} );

    assertEqual("int x;",  {type:"init", varType:"int", name:"x", rhs:null});
    assertEqual("bool x;", {type:"init", varType:"bool", name:"x", rhs:null});
    assertEqual("mutex x;", {type:"init", varType:"mutex", name:"x", rhs:null});
    assertEqual("cond_var x;", {type:"init", varType:"cond_var", name:"x", rhs:null});
    assertEqual("sem x;", {type:"init", varType:"sem", name:"x", rhs:null});

    assertEqual("int myVar = 4;", {type:"init", varType:"int", name:"myVar", rhs:{type:"int", value:4}});
    assertEqual("y = 4 + 4;", {type:"=", name:"y", rhs:{type:"+", a:{type:"int", value:4}, b:{type:"int", value:4}}});
    assertEqual("z += 4;", {type:"+=", name:"z", rhs:{type:"int", value:4}});
    assertEqual("z -= 4;", {type:"-=", name:"z", rhs:{type:"int", value:4}});
    assertEqual("z *= 4;", {type:"*=", name:"z", rhs:{type:"int", value:4}});

    assertEqual("x++;",      {type:"++", name:"x"});
    assertEqual("x --;",     {type:"--", name:"x"});
    assertEqual("z = -x++;", {type:"=", name:"z", rhs: {type:"unary-", a:{type:"++", name:"x"}}});
    assertEqual("x = y = 3;", {type:"=", name:"x", rhs: {type:"=", name:"y", rhs:{type:"int", value:3}}});
    assertEqual("(x = 3) + (y = 2);",
        {type:"+", a:{type:"=", name:"x", rhs:{type:"int", value:3}}, b:{type:"=", name:"y", rhs:{type:"int", value:2}}}
    );
    assertFails("x = 3 + y = 2");

    assertEqual("sem s = sem_create(3);",
        {type:"init", varType:"sem", name:"s", rhs:{type:"sem_create", param:{type:"int", value:3}}}
    );

    assertEqual("sem_create(3) + 3;",
        {type:"+", a:{type:"sem_create", param:{type:"int", value:3}}, b:{type:"int", value:3}}
    );
}

function _testStatements() {
    assertEqual("lock(x);",          {type:"lock", param: {type:"id", name:"x"}});
    assertEqual("unlock(y);",        {type:"unlock", param: {type:"id", name:"y"}});
    assertEqual("cond_wait(z);",     {type:"cond_wait", param: {type:"id", name:"z"}});
    assertEqual("cond_signal(var);", {type:"cond_signal", param: {type:"id", name:"var"}});
    assertEqual("sem_wait(bar);",    {type:"sem_wait", param: {type:"id", name:"bar"}});
    assertEqual("sem_post(foo);",    {type:"sem_post", param: {type:"id", name:"foo"}});

    assertFails("cond_signal");
    assertFails("cond_signal()");
    assertFails("sem_post(x");
    assertFails("sem_post(x + 2)"); // can only pass a variable reference.

    assertEqual("if (true) x = 3;;",
        {type:"if",
            cond:{type:"bool", value:true},
            ifCase: [{type:"=", name:"x", rhs:{type:"int", value:3}}],
            elseCase: []
    });
    assertEqual(`if (1 < 2) {x = 3;}`,
        {type:"if",
            cond:{type:"<", a:{type:"int", value:1}, b:{type:"int", value:2}},
            ifCase: [{type:"=", name:"x", rhs:{type:"int", value:3}}],
            elseCase: []}
    );
    assertEqual(
        `if (true) {
            x = 3;
            y = 3;
        } else
            x = 2;`,
        {type:"if",
            cond:{type:"bool", value:true} ,
            ifCase: [{type:"=", name:"x", rhs:{type:"int", value:3}}, {type:"=", name:"y", rhs:{type:"int", value:3}}],
            elseCase: [{type:"=", name:"x", rhs:{type:"int", value:2}}]}
    );
    assertEqual(
        `if (true) { 1; }
        else if (false) { 2; }
        else { 3; }`,
        {type:"if",
            cond:{type:"bool", value:true} ,
            ifCase: [{type:"int", value:1}],
            elseCase: [{type:"if",
                cond:{type:"bool", value:false},
                ifCase:[{type:"int", value:2}],
                elseCase: [{type:"int", value:3}]}]}
    );

    assertEqual("while (x <= 9) { x++;; };",
        {type:"while", cond:{type:"<=", a:{type:"id", name:"x"}, b:{type:"int", value:9}},
            body:[{type:"++", name:"x"}]}
    );

    assertEqual("for (int i = 3; i < 100; i++) {};",
        {type:"for",
            init:{type:"init", varType:"int", name:"i", rhs:{type:"int", value:3}},
            cond:{type:"<", a:{type:"id", name:"i"}, b:{type:"int", value:100}},
            inc:{type:"++", name:"i"},
            body:[]}
    );
    assertEqual("for (bool t = true; true; false);", // Infinite loop that does nothing
        {type:"for",
            init:{type:"init", varType:"bool", name:"t", rhs:{type:"bool", value:true}},
            cond:{type:"bool", value:true},
            inc:{type:"bool", value:false},
            body:[]}
    );
}

function _testPrograms() {
    var program = 
`int i = 0;;
while (i < 10 /* a comment */) { 
    i++; // another comment
}
/**/
/*
    for(;;) more comment
*/
int x = i;`;

    assertEqual(program,
        [{type:"init", varType:"int", name:"i",  src:[[0,0],[0,10]],
            rhs:{type:"int", value:0, src:[[0,8],[0,9]]}},
         {type:"while", src:[[1,0],[1,30]],
            cond:{type:"<", src:[[1,7],[1,13]],
                a:{type:"id", name:"i", src:[[1,7],[1,8]]},
                b:{type:"int", value:10, src:[[1,11],[1,13]]}},
            body: [{type:"++", name:"i", src:[[2,4],[2,8]]}]},
         {type:"init", varType:"int", name:"x", src:[[8,0],[8,10]],
            rhs:{type:"id", name:"i", src:[[8,8],[8,9]]}}],
    false); // Don't ignore src.
    // assertFails("my/*interupt*/Id;");
}

function _testEquals() {
    console.assert(recursiveEqual(2, 2));
    console.assert(!recursiveEqual(2, 3));
    console.assert(!recursiveEqual(2, [2]));
    console.assert(!recursiveEqual(2, "2"));
    console.assert(recursiveEqual([1, 2, 3], [1, 2, 3]));
    console.assert(recursiveEqual([], []));
    console.assert(!recursiveEqual([1, 2, 3], [1, 2, 4]));
    console.assert(!recursiveEqual([1, 2, 3], [1, 2]));
    console.assert(recursiveEqual([1, [2, 3]], [1, [2, 3]]));
    console.assert(recursiveEqual({a:1, b:2}, {a:1, b:2}));
    console.assert(!recursiveEqual({a:1, b:2}, {a:1, b:3}));
    console.assert(recursiveEqual({a:{c:1}, b:2}, {a:{c:1}, b:2}));
    console.assert(!recursiveEqual({a:{c:1}, b:2}, {a:{c:2}, b:2}));

}

// Helper functions

// Returns true a and b are equal. If a and b are objects, they are equal if all fields are equal.
// If a and b are arrays, returns true if all elements are equal.
function recursiveEqual(a, b) {
    if (a === b) {
        return true;
    } else if (a instanceof Array && b instanceof Array) {
        if (a.length != b.length) return false;

        for (var i = 0; i < a.length; i++) {
            if (!recursiveEqual(a[i], b[i])) return false;
        }

        return true;
    } else if (a instanceof Object && b instanceof Object) {
        aProps = Object.getOwnPropertyNames(a);
        bProps = Object.getOwnPropertyNames(b);

        if (aProps.length != bProps.length) return false;

        for (let field of aProps) {
            if (!recursiveEqual(a[field], b[field])) return false;
        }

        return true;
    } else {
        return false;
    }
}

// Asserts that the code parses to expected.
// code should parse to an array of objects. If a single object is given, it will assume that the code
// should parse to a 1 line program containing that object.
// If ignoreSrc is set, it will ignore any "src" fields in the generated code.
function assertEqual(code, expected, ignoreSrc = true) {
    var parser = new nearley.Parser(nearley.Grammar.fromCompiled(grammar));
    parser.feed(code);
    var results = parser.results;

    console.assert(results.length == 1,
        `"${code}" is ambiguous or can't be parsed. Got:`, results)

    if (!(expected instanceof Array)) {
        expected = [expected];
    }

    // hack to remove all src fields
    if (ignoreSrc)
        results = results.map((obj) => JSON.parse(JSON.stringify(obj, (k,v) => (k == 'src') ? undefined : v)))

    console.assert(recursiveEqual(results[0], expected),
        `in "${code}". Got:`, results[0], "expected", expected)
}

function assertFails(code) {
    try {
        var parser = new nearley.Parser(nearley.Grammar.fromCompiled(grammar));
        parser.feed(code);
        console.assert(parser.results.length == 0, `"${code}" did not fail. Got:`, results)
    } catch(err) {
        // passed
    }
}