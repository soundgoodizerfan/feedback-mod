// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

/**
 * The Standard Galactic Alphabet, the script vanilla's enchanting table floats over its book, as the lit pixels of
 * vanilla's own {@code font/ascii_sga.png} (a 5 x 7 cell box per letter; the art is Mojang's, reusable). Each letter is
 * the cells the player must light with the pen: {@link #cells} gives them as (column, row) pairs. Pure data, shared by the
 * screen (which draws the guide) and the server (which grades the pen's trail against it).
 */
public final class SgaGlyphs {

    private SgaGlyphs() {
    }

    public static final int COLUMNS = 5, ROWS = 7;

    /** Seven rows of five cells per letter, {@code #} lit; index 0 is {@code a}. */
    private static final String[][] LETTERS = {
        {"..##.", ".#..#", ".#...", ".#...", ".#...", ".#...", "##..."}, // a
        {"..#..", "..#..", "..#..", "..#..", "...#.", "....#", "#####"}, // b
        {"#....", ".....", "#....", "#....", "##...", ".#...", ".#..."}, // c
        {"#####", ".....", "##...", "..#..", "...##", ".....", "....."}, // d
        {"#...#", "#....", "#....", "#....", "#....", "#....", "#####"}, // e
        {"#####", ".....", "#.#.#", ".....", ".....", ".....", "....."}, // f
        {"..#..", "..#..", "..#..", "###..", "..#..", "..#..", "..#.."}, // g
        {"#####", ".....", "#####", "..#..", "..#..", "..#..", "..#.."}, // h
        {"#....", "#....", "#....", ".....", "#....", "#....", "#...."}, // i
        {"#....", "#....", ".....", "#....", ".....", "#....", "#...."}, // j
        {"..#..", "..#..", "..#..", "#.#.#", "..#..", "..#..", "..#.."}, // k
        {"#....", "#.#..", "#....", "#....", "#....", "#.#..", "#...."}, // l
        {"#...#", "....#", "....#", "....#", "....#", "....#", "#####"}, // m
        {"#..#.", "#..#.", "...#.", "..#..", "..#..", ".#...", "#...."}, // n
        {"####.", "...#.", "...#.", "..#..", "..#..", ".#...", "#...."}, // o
        {"#.#..", "..#..", "#.#..", "#.#..", "#.#..", "#....", "#.#.."}, // p
        {"..#..", ".....", "#####", "....#", "....#", "....#", "#####"}, // q
        {"#..#.", ".....", ".....", ".....", ".....", ".....", "#..#."}, // r
        {"#....", "#....", "#....", "##...", ".#...", ".#...", ".#..."}, // s
        {"#####", "....#", "....#", "....#", "....#", ".....", "....#"}, // t
        {".....", ".....", ".#.#.", ".....", "#####", ".....", "....."}, // u
        {"..#..", "..#..", "..#..", "..#..", "#####", ".....", "#####"}, // v
        {".....", ".....", "..#..", ".....", ".....", "#...#", "....."}, // w
        {"#...#", "...#.", "...#.", "..#..", ".#...", ".#...", "#...."}, // x
        {"#.#..", "#.#..", "#.#..", "#.#..", "#.#..", "#.#..", "#.#.."}, // y
        {"..#..", ".#.#.", "#...#", "#...#", "#...#", "#...#", "#...#"}, // z
    };

    /** The lit cells of {@code letter} (a to z) as {column, row} pairs; empty for any other character. */
    public static int[][] cells(char letter) {
        int i = Character.toLowerCase(letter) - 'a';
        if (i < 0 || i >= LETTERS.length)
            return new int[0][];
        java.util.List<int[]> out = new java.util.ArrayList<>();
        for (int y = 0; y < ROWS; y++)
            for (int x = 0; x < COLUMNS; x++)
                if (LETTERS[i][y].charAt(x) == '#')
                    out.add(new int[] {x, y});
        return out.toArray(new int[0][]);
    }
}
