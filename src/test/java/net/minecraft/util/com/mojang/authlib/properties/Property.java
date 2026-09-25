package net.minecraft.util.com.mojang.authlib.properties;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Заглушка свойства authlib с обеими сигнатурами конструктора, которые
 * перебирает {@code GameProfileFactory.newAuthlibProperty}.
 *
 * <p>Порядок конструкторов в {@code getConstructors()} не гарантирован, поэтому
 * заглушка сделана состоятельной: каждый конструктор отказывает на нечётных
 * попытках и работает на чётных. За три вызова перебор проходит и падающую
 * сигнатуру, и обе успешные — в любом порядке их выдачи.</p>
 */
public class Property {

    private static final AtomicInteger TWO_ARGUMENT_ATTEMPTS = new AtomicInteger();
    private static final AtomicInteger THREE_ARGUMENT_ATTEMPTS = new AtomicInteger();

    private final String name;
    private final String value;
    private final String signature;

    public Property(String name, String value) {

        if (refuses(TWO_ARGUMENT_ATTEMPTS)) {
            throw new UnsupportedOperationException("stub: the signature refuses the first and third attempts");
        }

        this.name = name;
        this.value = value;
        this.signature = null;

    }

    public Property(String name, String value, String signature) {

        if (refuses(THREE_ARGUMENT_ATTEMPTS)) {
            throw new UnsupportedOperationException("stub: the signature refuses the first and third attempts");
        }

        this.name = name;
        this.value = value;
        this.signature = signature;

    }

    /**
     * Отказ на первой и третьей попытке: три прогона перебора покрывают и обе
     * падающие сигнатуры, и обе успешные, а дальнейшие вызовы всегда успешны.
     */
    private static boolean refuses(AtomicInteger attempts) {
        int attempt = attempts.incrementAndGet();
        return attempt == 1 || attempt == 3;
    }

    /**
     * Сбрасывает счётчики попыток: тест начинает перебор с известного состояния.
     */
    public static void resetStub() {

        TWO_ARGUMENT_ATTEMPTS.set(0);
        THREE_ARGUMENT_ATTEMPTS.set(0);

    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getSignature() {
        return signature;
    }
}
