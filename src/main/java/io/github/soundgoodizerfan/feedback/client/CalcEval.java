// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

/**
 * The Calculator's arithmetic: {@code + - * / ^}, brackets, unary minus, decimals. Recursive descent, no allocation beyond the cursor.
 * A malformed expression or a division by zero is {@link Double#NaN}.
 */
final class CalcEval {

    private final String text;
    private int at;

    private CalcEval(String text) {
        this.text = text;
    }

    static double eval(String text) {
        CalcEval parser = new CalcEval(text.replace(" ", ""));
        try {
            double value = parser.sum();
            return parser.at == parser.text.length() ? value : Double.NaN;
        } catch (RuntimeException e) {
            return Double.NaN;
        }
    }

    private boolean eat(char c) {
        if (at < text.length() && text.charAt(at) == c) {
            at++;
            return true;
        }
        return false;
    }

    private double sum() {
        double v = product();
        while (true) {
            if (eat('+'))
                v += product();
            else if (eat('-'))
                v -= product();
            else
                return v;
        }
    }

    private double product() {
        double v = power();
        while (true) {
            if (eat('*'))
                v *= power();
            else if (eat('/'))
                v /= power();
            else
                return v;
        }
    }

    private double power() {
        double base = unary();
        return eat('^') ? Math.pow(base, power()) : base;
    }

    private double unary() {
        if (eat('-'))
            return -unary();
        if (eat('(')) {
            double v = sum();
            if (!eat(')'))
                throw new IllegalStateException();
            return v;
        }
        int start = at;
        while (at < text.length() && (Character.isDigit(text.charAt(at)) || text.charAt(at) == '.'))
            at++;
        if (start == at)
            throw new IllegalStateException();
        return Double.parseDouble(text.substring(start, at));
    }

    /** A figure for the display: whole numbers plain, otherwise up to six decimals, very large or small in exponent form. */
    static String show(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v))
            return "error";
        if (v == Math.rint(v) && Math.abs(v) < 1e12)
            return String.valueOf((long) v);
        if (Math.abs(v) >= 1e9 || Math.abs(v) < 1e-5)
            return String.format("%.4e", v);
        String s = String.format("%.6f", v);
        s = s.indexOf('.') < 0 ? s : s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }
}
