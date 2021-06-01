package net.hardnorth.github.merge.model;

import com.auth0.jwt.algorithms.Algorithm;

public class JwtAlgorithm {
    private final Algorithm algo;

    public JwtAlgorithm(final Algorithm algorithm) {
        algo = algorithm;
    }

    public Algorithm get() {
        return algo;
    }
}
