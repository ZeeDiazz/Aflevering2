import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;


public abstract class AST {
    public void error(String msg) {
        System.err.println(msg);
        System.exit(-1);
    }
};

/* Expressions are similar to arithmetic expressions in the impl
   language: the atomic expressions are just Signal (similar to
   variables in expressions) and they can be composed to larger
   expressions with And (Conjunction), Or (Disjunction), and
   Not (Negation) */

abstract class Expr extends AST {

    public abstract Boolean eval(Environment env);

    public abstract List<String> getSignalNames();


}

class Conjunction extends Expr {
    Expr e1, e2;

    Conjunction(Expr e1, Expr e2) {
        this.e1 = e1;
        this.e2 = e2;
    }

    @Override
    public Boolean eval(Environment env) {
        return e1.eval(env) && e2.eval(env);
    }

    @Override
    public List<String> getSignalNames() {

        List<String> list = e1.getSignalNames();
        list.addAll(e2.getSignalNames());


        return list;
    }

}

class Disjunction extends Expr {
    Expr e1, e2;

    Disjunction(Expr e1, Expr e2) {
        this.e1 = e1;
        this.e2 = e2;
    }

    @Override
    public Boolean eval(Environment env) {
        return e1.eval(env) || e2.eval(env);
    }

    @Override
    public List<String> getSignalNames() {

        List<String> list = e1.getSignalNames();
        list.addAll(e2.getSignalNames());


        return list;
    }


}

class Negation extends Expr {
    Expr e;

    Negation(Expr e) {
        this.e = e;
    }

    @Override
    public Boolean eval(Environment env) {
        return !e.eval(env);
    }

    @Override
    public List<String> getSignalNames() {
        return e.getSignalNames();
    }

}

class Signal extends Expr {
    String varname; // a signal is just identified by a name

    Signal(String varname) {
        this.varname = varname;
    }

    @Override
    public Boolean eval(Environment env) {
        return env.getVariable(varname);
    }

    @Override
    public List<String> getSignalNames() {
        List<String> list = new ArrayList<>();
        list.add(varname);
        return list;
    }

}

// Latches have an input and output signal

class Latch extends AST {
    String inputname;
    String outputname;

    Latch(String inputname, String outputname) {
        this.inputname = inputname;
        this.outputname = outputname;
    }

    public void initialize(Environment env) {
        env.setVariable(outputname, false);
    }

    public void nextCycle(Environment env) {
        env.setVariable(outputname, env.getVariable(inputname));
    }
}

// An Update is any of the lines " signal = expression "
// in the .update section

class Update extends AST {
    String name;
    Expr e;

    Update(String name, Expr e) {
        this.e = e;
        this.name = name;
    }

    public void eval(Environment env) {
        env.setVariable(name, e.eval(env));
    }
}

/* A Trace is a signal and an array of Booleans, for instance each
   line of the .simulate section that specifies the traces for the
   input signals of the circuit. It is suggested to use this class
   also for the output signals of the circuit in the second
   assignment.
*/

class Trace extends AST {
    String signal;
    Boolean[] values;

    Trace(String signal, Boolean[] values) {
        this.signal = signal;
        this.values = values;
    }

    //print the num and handle
    public String toString() {

        return Arrays.toString(this.values)
                .replace("false", "0")
                .replace("true", "1")
                .replace("[", "")
                .replace("]", "")
                .replace(", ", "") + " " + this.signal;
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

class Circuit extends AST {
    String name;
    List<String> inputs;
    List<String> outputs;
    List<Latch> latches;
    List<Update> updates;
    List<Trace> siminputs;
    List<Trace> simoutputs;
    int simlength;

    HashSet<String> signalNames = new HashSet<>();
    HashSet<String> duplicates = new HashSet<>();
    List<String> outputNameLatches = new ArrayList<>();
    List<String> nameUpdates = new ArrayList<>();

    Circuit(String name,
            List<String> inputs,
            List<String> outputs,
            List<Latch> latches,
            List<Update> updates,
            List<Trace> siminputs) {
        this.name = name;
        this.inputs = inputs;
        this.outputs = outputs;
        this.latches = latches;
        this.updates = updates;
        this.siminputs = siminputs;
        this.simoutputs = new ArrayList<>();

        for (Latch latch : this.latches) {
            outputNameLatches.add(latch.outputname);
        }
        for (Update update : this.updates) {
            nameUpdates.add(update.name);
        }

        for (String input : this.inputs) {
            if (!signalNames.add(input)) {
                duplicates.add(input);
            }
        }

        for (String outputNameLatch : this.outputNameLatches) {
            if (!signalNames.add(outputNameLatch)) {
                duplicates.add(outputNameLatch);
            }
        }

        for (String nameUpdates : this.nameUpdates) {
            if (!signalNames.add(nameUpdates)) {
                duplicates.add(nameUpdates);
            }
        }
        simlength = siminputs.get(0).values.length;
        for (String out : outputs) {
            simoutputs.add(new Trace(out, new Boolean[simlength]));
        }
    }


    public void initialize(Environment env) {
        for (Trace t : siminputs) {
            if (t.values.length == 0 || t.signal == null) {
                continue;
            }
            env.setVariable(t.signal, t.values[0]);
        }

        for (Latch latch : latches) {
            latch.initialize(env);
        }
        HashSet<String> legalSignals = new HashSet<>();
        legalSignals.addAll(inputs);
        legalSignals.addAll(outputNameLatches);


        for (Update update : updates) {
            if (legalSignals.containsAll(update.e.getSignalNames())) {
                update.eval(env);
                legalSignals.add(update.name);
            } else {
                error("Update signal " + update.name + " hasn't been defined and may be cyclical");
            }
        }

        for (Trace t : simoutputs) {
            t.values[0] = env.getVariable(t.signal);
        }
        System.out.println(env.toString());
    }

    public void nextCycle(Environment env, int i) {

        for (Trace t : siminputs) {
            env.setVariable(t.signal, t.values[i]);
        }

        for (Latch latch : latches) {
            latch.nextCycle(env);
        }

        for (Update update : updates) {
            update.eval(env);
        }

        for (Trace t : simoutputs) {
            t.values[i] = env.getVariable(t.signal);
        }
        System.out.println(env.toString());
    }

    public void runSimulator() {
        Environment environment = new Environment();
        initialize(environment);
        for (int i = 1; i < simlength; i++) {
            nextCycle(environment, i);
        }
        for (Trace t : siminputs) {
            System.out.println(t.toString());
        }

        for (Trace t : simoutputs) {
            System.out.println(t.toString());
        }

        if (signalNames.size() != inputs.size() + outputNameLatches.size() + nameUpdates.size()) {
            error("Error: There is 1 or more duplicate Signals which are:\n" +
                    duplicates);
        }

        for (Trace siminput : siminputs) {
            if (!signalNames.contains(siminput.signal)) {
                error("Error: Signal " + siminput.signal + " is not an input, latch output, or update output.");
                break;
            }
        }

        for (int i = 1; i < simlength; i++) {
            nextCycle(environment, i);
        }
    }
}


