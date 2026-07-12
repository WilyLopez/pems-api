package com.playzone.pems.shared.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TokenHasherTest {

    @Test
    void testGenerarTokenAleatorioProduceValoresUnicos() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            tokens.add(TokenHasher.generarTokenAleatorio());
        }
        assertEquals(100, tokens.size());
    }

    @Test
    void testGenerarTokenAleatorioNoContieneCaracteresDeRelleno() {
        String token = TokenHasher.generarTokenAleatorio();
        assertFalse(token.contains("="));
    }

    @Test
    void testHashearEsDeterministico() {
        String token = "token-de-prueba";
        assertEquals(TokenHasher.hashear(token), TokenHasher.hashear(token));
    }

    @Test
    void testHashearProduceHexDeSesentaYCuatroCaracteres() {
        String hash = TokenHasher.hashear("cualquier-valor");
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]+"));
    }

    @Test
    void testHashearDistintosTokensProduceHashesDistintos() {
        assertNotEquals(TokenHasher.hashear("a"), TokenHasher.hashear("b"));
    }
}
