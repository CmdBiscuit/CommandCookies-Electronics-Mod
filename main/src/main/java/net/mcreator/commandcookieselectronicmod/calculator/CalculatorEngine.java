package net.mcreator.commandcookieselectronicmod.calculator;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** A decimal pocket calculator. Chained operations are evaluated left to right. */
public final class CalculatorEngine {
    private static final MathContext PRECISION = new MathContext(16, RoundingMode.HALF_UP);
    private String entry = "0", pending = "", repeat = "", history = "";
    private BigDecimal accumulator = BigDecimal.ZERO, repeatValue = BigDecimal.ZERO, memory = BigDecimal.ZERO;
    private boolean fresh = true, error;

    public String display() { return entry; }
    public String history() { return history; }
    public boolean hasMemory() { return memory.signum() != 0; }
    public boolean isError() { return error; }

    public record Snapshot(String entry, String pending, String repeat, String history,
        String accumulator, String repeatValue, String memory, boolean fresh, boolean error) {}
    public Snapshot snapshot() {
        return new Snapshot(entry, pending, repeat, history, accumulator.toString(), repeatValue.toString(), memory.toString(), fresh, error);
    }
    public void restore(Snapshot state) {
        if (state == null) return;
        try {
            if (state.entry() == null || state.entry().length() > 128 || state.history() == null || state.history().length() > 200
                || !validOperator(state.pending()) || !validOperator(state.repeat())) throw new IllegalArgumentException("Invalid calculator state");
            BigDecimal restoredAccumulator = savedNumber(state.accumulator()), restoredRepeat = savedNumber(state.repeatValue()), restoredMemory = savedNumber(state.memory());
            if (!state.error()) savedNumber(state.entry());
            else if (!state.entry().equals("Error")) throw new IllegalArgumentException("Invalid error state");
            entry = state.entry(); pending = state.pending(); repeat = state.repeat(); history = state.history();
            accumulator = restoredAccumulator; repeatValue = restoredRepeat; memory = restoredMemory;
            fresh = state.fresh(); error = state.error();
        } catch (RuntimeException ex) { clear(); repeatValue = memory = BigDecimal.ZERO; }
    }
    private static boolean validOperator(String value) { return value != null && (value.isEmpty() || value.length() == 1 && "+-*/".contains(value)); }
    private static BigDecimal savedNumber(String value) {
        if (value == null || value.length() > 128) throw new IllegalArgumentException("Invalid saved number");
        BigDecimal number = new BigDecimal(value);
        if (Math.abs((long)number.scale()) > 128 || number.precision() > 32) throw new IllegalArgumentException("Invalid saved number");
        return checked(number);
    }

    public void press(String key) {
        try {
            if (key.equals("C")) { clear(); return; }
            if (key.equals("MC")) { memory = BigDecimal.ZERO; return; }
            if (key.equals("MR") || key.equals("M")) {
                if (error) clear();
                entry = format(memory); fresh = false; repeat = ""; return;
            }
            if (key.length() == 1 && Character.isDigit(key.charAt(0))) { digit(key); return; }
            if (key.equals(".")) {
                if (error) clear();
                if (fresh) { entry = "0"; fresh = false; if (pending.isEmpty()) history = ""; }
                if (!entry.contains(".") && !entry.contains("E")) entry += ".";
                repeat = ""; return;
            }
            if (key.equals("BACK")) {
                if (error) { clear(); return; }
                if (fresh || entry.contains("E")) return;
                entry = entry.length() > 1 ? entry.substring(0, entry.length() - 1) : "0";
                if (entry.equals("-")) entry = "0";
                return;
            }
            if (error) return;
            if (key.equals("SIGN")) {
                if (fresh && !pending.isEmpty()) entry = "0";
                entry = entry.startsWith("-") ? entry.substring(1) : "-" + entry;
                fresh = false; repeat = ""; return;
            }
            if (key.equals("%")) {
                BigDecimal value = number();
                if (pending.equals("+") || pending.equals("-")) value = accumulator.multiply(value, PRECISION);
                entry = format(value.divide(BigDecimal.valueOf(100), PRECISION));
                fresh = false; repeat = ""; return;
            }
            if (key.equals("M+") || key.equals("M-")) {
                memory = checked(key.equals("M+") ? memory.add(number(), PRECISION) : memory.subtract(number(), PRECISION));
                if (pending.isEmpty()) fresh = true; return;
            }
            if (key.equals("=")) { equalsKey(); return; }
            if (key.equals("+") || key.equals("-") || key.equals("*") || key.equals("/")) {
                BigDecimal value = number();
                if (!pending.isEmpty() && !fresh) value = calculate(accumulator, value, pending);
                accumulator = value; entry = format(value); pending = key; repeat = ""; fresh = true;
                history = format(accumulator) + " " + symbol(key);
            }
        } catch (ArithmeticException ex) {
            entry = "Error"; history = ex.getMessage(); error = true; pending = ""; repeat = ""; fresh = true;
        }
    }

    private void digit(String digit) {
        if (error) clear();
        if (fresh) { entry = "0"; fresh = false; if (pending.isEmpty()) history = ""; }
        if (entry.replace("-", "").replace(".", "").length() >= 16) return;
        if (entry.equals("0")) entry = digit;
        else if (entry.equals("-0")) entry = "-" + digit;
        else entry += digit;
        repeat = "";
    }

    private void equalsKey() {
        BigDecimal left = number(), right;
        String operator;
        if (!pending.isEmpty()) {
            left = accumulator; right = fresh ? accumulator : number(); operator = pending;
            repeat = pending; repeatValue = right; pending = "";
        } else if (!repeat.isEmpty()) { right = repeatValue; operator = repeat; }
        else { fresh = true; return; }
        BigDecimal result = calculate(left, right, operator);
        history = format(left) + " " + symbol(operator) + " " + format(right) + " =";
        accumulator = result; entry = format(result); fresh = true;
    }

    private static BigDecimal calculate(BigDecimal left, BigDecimal right, String operator) {
        if (operator.equals("/") && right.signum() == 0) throw new ArithmeticException("Cannot divide by zero");
        return checked(switch (operator) {
            case "+" -> left.add(right, PRECISION);
            case "-" -> left.subtract(right, PRECISION);
            case "*" -> left.multiply(right, PRECISION);
            case "/" -> left.divide(right, PRECISION);
            default -> right;
        });
    }

    private static BigDecimal checked(BigDecimal number) {
        if (number.signum() != 0 && Math.abs(number.precision() - number.scale() - 1) > 99)
            throw new ArithmeticException("Number out of range");
        return number;
    }
    private BigDecimal number() { return new BigDecimal(entry); }
    private static String format(BigDecimal number) {
        number = checked(number).stripTrailingZeros();
        String plain = number.toPlainString();
        return plain.length() <= 20 ? plain : number.toString();
    }
    private static String symbol(String operator) { return operator.equals("*") ? "\u00d7" : operator.equals("/") ? "\u00f7" : operator; }
    private void clear() { entry = "0"; pending = ""; repeat = ""; history = ""; accumulator = BigDecimal.ZERO; fresh = true; error = false; }
}
