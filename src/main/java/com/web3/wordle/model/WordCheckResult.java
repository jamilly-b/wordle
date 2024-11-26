package com.web3.wordle.model;

public class WordCheckResult {
    private char letter;
    private String status; // "correct", "misplaced", "absent"

    public WordCheckResult(char letter, String status) {
        this.letter = letter;
        this.status = status;
    }

    public char getLetter() {
        return letter;
    }

    public String getStatus() {
        return status;
    }
}
