import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.List;
import java.util.ArrayList;

public abstract class AST{
    public void error(String msg){
        System.err.println(msg);
        System.exit(-1);
    }
};

/* Expressions are similar to arithmetic expressions in the impl
   language: the atomic expressions are just Signal (similar to
   variables in expressions) and they can be composed to larger
   expressions with And (Conjunction), Or (Disjunction), and
   Not (Negation) */

abstract class Expr extends AST{

    abstract public Boolean eval(Environment env);
    abstract public void eval(Environment env);
}

class Conjunction extends Expr{
    Expr e1,e2;
    Conjunction(Expr e1,Expr e2){this.e1=e1; this.e2=e2;}

    @Override
    public Boolean eval(Environment env) {
        return null;
    }
}

class Disjunction extends Expr{
    Expr e1,e2;
    Disjunction(Expr e1,Expr e2){this.e1=e1; this.e2=e2;}

    @Override
    public Boolean eval(Environment env){
        return e1.eval(env) || e2.eval(env);
    }

}

class Negation extends Expr{
    Expr e;
    Negation(Expr e){this.e=e;}
}

class Signal extends Expr{
    String varname; // a signal is just identified by a name
    Signal(String varname){this.varname=varname;}
    @Override
    public void eval(Environment env){
        env.getVariable(varname);
    }

}

// Latches have an input and output signal

class Latch extends AST{
    String inputname;
    String outputname;
    Latch(String inputname, String outputname){
        this.inputname=inputname;
        this.outputname=outputname;
    }
}

// An Update is any of the lines " signal = expression "
// in the .update section

class Update extends AST{
    String name;
    Expr e;
    Update(String name, Expr e){this.e=e; this.name=name;}
}

/* A Trace is a signal and an array of Booleans, for instance each
   line of the .simulate section that specifies the traces for the
   input signals of the circuit. It is suggested to use this class
   also for the output signals of the circuit in the second
   assignment.
*/

class Trace extends AST{
    String signal;
    Boolean[] values;
    Trace(String signal, Boolean[] values){
        this.signal=signal;
        this.values=values;
    }
    //print the num and handle
    public String toString(){
        return " Signal: " + this.signal + " Values: " + Arrays.toString(this.values);
    }
}

/* The main data structure of this simulator: the entire circuit with
   its inputs, outputs, latches, and updates. Additionally for each
   input signal, it has a Trace as simulation input.

   There are two variables that are not part of the abstract syntax
   and thus not initialized by the constructor (so far): simoutputs
   and simlength. It is suggested to use them for assignment 2 to
   implement the interpreter:

   1. to have simlength as the length of the traces in siminputs. (The
   simulator should check they have all the same length and stop with
   an error otherwise.) Now simlength is the number of simulation
   cycles the interpreter should run.

   2. to store in simoutputs the value of the output signals in each
   simulation cycle, so they can be displayed at the end. These traces
   should also finally have the length simlength.
*/

class Circuit extends AST{
    String name;
    List<String> inputs;
    List<String> outputs;
    List<Latch>  latches;
    List<Update> updates;
    List<Trace>  siminputs;
    List<Trace>  simoutputs;
    int simlength;
    Circuit(String name,
            List<String> inputs,
            List<String> outputs,
            List<Latch>  latches,
            List<Update> updates,
            List<Trace>  siminputs){
        this.name=name;
        this.inputs=inputs;
        this.outputs=outputs;
        this.latches=latches;
        this.updates=updates;
        this.siminputs=siminputs;
    }
}
