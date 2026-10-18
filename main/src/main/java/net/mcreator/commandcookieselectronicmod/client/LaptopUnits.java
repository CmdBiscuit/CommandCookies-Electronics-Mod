package net.mcreator.commandcookieselectronicmod.client;

import java.math.BigDecimal;
import java.math.MathContext;

public final class LaptopUnits {
    public static final String[] CATEGORIES={"Length","Mass","Temperature","Time","Stacks"};
    public static final String[][] UNITS={{"meters","km","cm","feet","inches","miles"},{"kg","grams","pounds","ounces"},{"Celsius","Fahrenheit","Kelvin"},{"seconds","minutes","hours","days","MC ticks"},{"items","stacks (64)","stacks (16)","shulkers (1728)"}};
    private static final double[][] FACTORS={{1,1000,.01,.3048,.0254,1609.344},{1,.001,.45359237,.028349523125},{1,1,1},{1,60,3600,86400,.05},{1,64,16,1728}};
    public static String convert(String text,int category,int from,int to){try{if(text.isBlank())return "Enter a number";double n=Double.parseDouble(text.strip());if(!Double.isFinite(n)||Math.abs(n)>1e15)return "Number is too large";double out;if(category==2){double c=from==0?n:from==1?(n-32)*5/9:n-273.15;if(c<-273.150001)return "Below absolute zero";out=to==0?c:to==1?c*9/5+32:c+273.15;}else out=n*FACTORS[category][from]/FACTORS[category][to];if(!Double.isFinite(out))return "Number is too large";return BigDecimal.valueOf(Math.abs(out)<1e-12?0:out).round(new MathContext(10)).stripTrailingZeros().toPlainString();}catch(NumberFormatException error){return "Invalid number";}}
    private LaptopUnits(){}
}
