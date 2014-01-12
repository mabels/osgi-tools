/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.nextaudience.db.datasource.base;

public class SplittedKey {

    final String first;
    final String tail;

    public SplittedKey(String first, String tail) {
        this.first = first;
        this.tail = tail;
    }

    public static SplittedKey create(String dotted) {
        int dot = dotted.indexOf('.');
        if (dot < 0) {
            return null;
        }
        return new SplittedKey(dotted.substring(0, dot), dotted.substring(dot+1));
    }
}
