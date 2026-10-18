package net.mcreator.commandcookieselectronicmod.client;

import java.math.BigDecimal;
import java.math.MathContext;

/** Bounded decimal expression evaluator for the BlockOS terminal. */
final class LaptopExpression {
    private static final MathContext MC=MathContext.DECIMAL128;
    private final String text;private int pos,depth;
    private LaptopExpression(String text){this.text=text.replace(" ","");if(this.text.length()>256)throw new IllegalArgumentException("Expression too long");}
    static String evaluate(String text){try{var p=new LaptopExpression(text);var n=p.expression();if(p.pos!=p.text.length())throw new IllegalArgumentException("Unexpected character");if(n.precision()>100||Math.abs(n.scale())>200)throw new IllegalArgumentException("Result too large");return n.stripTrailingZeros().toPlainString();}catch(ArithmeticException e){throw new IllegalArgumentException("Invalid operation or division by zero");}}
    private boolean take(char c){if(pos<text.length()&&text.charAt(pos)==c){pos++;return true;}return false;}
    private BigDecimal expression(){BigDecimal n=term();for(;;){if(take('+'))n=n.add(term(),MC);else if(take('-'))n=n.subtract(term(),MC);else return n;}}
    private BigDecimal term(){BigDecimal n=unary();for(;;){if(take('*'))n=n.multiply(unary(),MC);else if(take('/'))n=n.divide(unary(),MC);else if(take('%'))n=n.remainder(unary(),MC);else return n;}}
    private BigDecimal unary(){if(++depth>32)throw new IllegalArgumentException("Too many nested terms");try{if(take('+'))return unary();if(take('-'))return unary().negate();return power();}finally{depth--;}}
    private BigDecimal power(){BigDecimal n=atom();if(take('^')){int exponent=unary().intValueExact();if(Math.abs((long)exponent)>100)throw new IllegalArgumentException("Exponent too large");n=exponent<0?BigDecimal.ONE.divide(n.pow(-exponent,MC),MC):n.pow(exponent,MC);}return n;}
    private BigDecimal atom(){
        if(++depth>32)throw new IllegalArgumentException("Too many nested terms");
        try{if(take('(')){BigDecimal n=expression();if(!take(')'))throw new IllegalArgumentException("Missing closing parenthesis");return n;}
            int start=pos;while(pos<text.length()&&(Character.isDigit(text.charAt(pos))||text.charAt(pos)=='.'))pos++;
            if(start==pos)throw new IllegalArgumentException("Expected a number");try{return new BigDecimal(text.substring(start,pos),MC);}catch(NumberFormatException e){throw new IllegalArgumentException("Invalid number");}
        }finally{depth--;}
    }
}
