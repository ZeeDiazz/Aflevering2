import org.antlr.v4.runtime.tree.ParseTreeVisitor;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.CharStreams;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

public class main {
    public static void main(String[] args) throws IOException{

	// we expect exactly one argument: the name of the input file
	if (args.length!=1) {
	    System.err.println("\n");
	    System.err.println("Hardware Simulator\n");
	    System.err.println("==================\n\n");
	    System.err.println("Please give as input argument a filename\n");
	    System.exit(-1);
	}
	String filename=args[0];

	// open the input file
	CharStream input = CharStreams.fromFileName(filename);
	    //new ANTLRFileStream (filename); // depricated
	
	// create a lexer/scanner
	hwLexer lex = new hwLexer(input);
	
	// get the stream of tokens from the scanner
	CommonTokenStream tokens = new CommonTokenStream(lex);
	
	// create a parser
	hwParser parser = new hwParser(tokens);
	
	// and parse anything from the grammar for "start"
	ParseTree parseTree = parser.start();

	// The JaxMaker is a visitor that produces html/jax output as a string
	String result = new JaxMaker().visit(parseTree);
	System.out.println("\n\n\n"+result);

	/* The AstMaker generates the abstract syntax to be used for
	   the second assignment, where for the start symbol of the
	   ANTLR grammar, it generates an object of class Circuit (see
	   AST.java). */
	
	Circuit p = (Circuit) new AstMaker().visit(parseTree);

	/* For the second assignment you need to extend the classes of
	    AST.java with some methods that correspond to running a
	    simulation of the given hardware for given simulation
	    inputs. The method for starting the simulation should be
	    called here for the Circuit p. */
    }
}

// The visitor for producing html/jax -- solution for assignment 1, task 3:

class JaxMaker extends AbstractParseTreeVisitor<String> implements hwVisitor<String> {

    public String visitStart(hwParser.StartContext ctx){
	// 
	String result = "<!DOCTYPE html>\n"+
	    "<html><head><title> "+ctx.name.getText()+ "</title>\n"+
	    "<script src=\"https://polyfill.io/v3/polyfill.min.js?features=es6\"></script>\n"+
	    "<script type=\"text/javascript\" id=\"MathJax-script\" async src=\"https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-chtml.js\">\n"+
	    "</script></head><body>\n";
	result+="<h1>" +ctx.name.getText()+ "</h1>\n"
	    + "<h2> Inputs </h2>\n";
       
	for(Token t:ctx.ins){
	    result += t.getText() + " ";
	}

	result+="\n <h2> Outputs </h2>\n ";
	for(Token t:ctx.outs){
	    result += t.getText()+ " ";
	}

	result+="\n <h2> Latches </h2>\n";

	for(hwParser.LatchdeclContext t:ctx.ls){
	    result +=  visit(t);
	}

	result+="\n <h2> Updates </h2>\n";
	
	for(hwParser.UpdatedeclContext t:ctx.up){
	    result += visit(t);
	}

	result+="\n <h2> Simulation inputs </h2>\n";
	for(hwParser.SimInpContext t:ctx.simin)
	    result+= visit(t);

	result += "\n</body></html>\n";
	return result;
    };

    public String visitSimInp(hwParser.SimInpContext ctx){
	return "<b>"+ctx.in.getText()+"</b>: "+ctx.str.getText()+"<br>\n";
    }
    
    public String visitLatchdecl(hwParser.LatchdeclContext ctx){
	return ctx.in.getText()+"&rarr;"+ctx.out.getText()+"<br>\n";
    }

    public String visitUpdatedecl(hwParser.UpdatedeclContext ctx){
	return ctx.write.getText()+"&larr;\\("+ visit(ctx.e)+"\\)<br>\n";
    }
        
    public String visitSignal(hwParser.SignalContext ctx){
	return "\\mathrm{"+ctx.x.getText()+"}";
    };

    public String visitConjunction(hwParser.ConjunctionContext ctx){
	return "("+visit(ctx.e1)+"\\wedge"+visit(ctx.e2)+")";
    };

    public String visitDisjunction(hwParser.DisjunctionContext ctx){
	return "("+visit(ctx.e1)+"\\vee"+visit(ctx.e2)+")";
    };

    public String visitNegation(hwParser.NegationContext ctx){
	return "\\neg("+visit(ctx.e)+")";
    };

    public String visitParenthesis(hwParser.ParenthesisContext ctx){
	return visit(ctx.e);
    }

}

// The visitor for producing the Abstract Syntax (see AST.java).

class AstMaker extends AbstractParseTreeVisitor<AST> implements hwVisitor<AST> {

    public AST visitStart(hwParser.StartContext ctx){
	List<String> ins=new ArrayList<String>();
	for(Token t:ctx.ins){
	    ins.add(t.getText());
	}
	List<String> outs=new ArrayList<String>();
	for(Token t:ctx.outs){
	    outs.add(t.getText());
	}
	List<Latch> latches=new ArrayList<Latch>();
	for(hwParser.LatchdeclContext t:ctx.ls){
	    latches.add((Latch) visit(t));
	}
	List<Update> updates=new ArrayList<Update>();
	for(hwParser.UpdatedeclContext t:ctx.up){
	    updates.add((Update) visit(t));
	}
	List<Trace> siminp=new ArrayList<Trace>();
	for(hwParser.SimInpContext t:ctx.simin)
	    siminp.add((Trace) visit(t));
	return new Circuit(ctx.name.getText(),ins,outs,latches,updates,siminp);
    };

    public AST visitSimInp(hwParser.SimInpContext ctx){
	String s=ctx.str.getText();
	// s is a string consisting of characters '0' and '1' (not numbers!)
	Boolean[] tr=new Boolean[s.length()];
	// for the simulation it is more convenient to work with
	// Booleans, so converting the string s to an array of
	// Booleans here:	
	for(int i=0; i<s.length();i++)
	    tr[i]=(s.charAt(i)=='1'); 
	return new Trace(ctx.in.getText(),tr);
    }
    
    public AST visitLatchdecl(hwParser.LatchdeclContext ctx){
	return new Latch(ctx.in.getText(),ctx.out.getText());
    }

    public AST visitUpdatedecl(hwParser.UpdatedeclContext ctx){
	return new Update(ctx.write.getText(),
			  (Expr) visit(ctx.e));
    }
    
    
    public AST visitSignal(hwParser.SignalContext ctx){
	return new Signal(ctx.x.getText());
    };

    public AST visitConjunction(hwParser.ConjunctionContext ctx){
	return new Conjunction((Expr) visit(ctx.e1),
			       (Expr)visit(ctx.e2));
    };

    public AST visitDisjunction(hwParser.DisjunctionContext ctx){
	return new Disjunction((Expr) visit(ctx.e1),
			       (Expr)visit(ctx.e2));
    };

    public AST visitNegation(hwParser.NegationContext ctx){
	return new Negation((Expr) visit(ctx.e)); 
    };

    public AST visitParenthesis(hwParser.ParenthesisContext ctx){
	return (Expr) visit(ctx.e);
    }

}

