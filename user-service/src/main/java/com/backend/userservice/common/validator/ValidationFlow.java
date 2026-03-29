package com.backend.userservice.common.validator;

import java.util.function.Function;
import java.util.function.UnaryOperator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidationFlow<T> {

    private T target;
    private Function<T, T> chain;

    private ValidationFlow(T target) {
        this.target = target;
        this.chain = UnaryOperator.identity();
    }

    public static <T> ValidationFlow<T> start(T target) {
        return new ValidationFlow<>(target);
    }

    public ValidationFlow<T> next(UnaryOperator<T> validator) {
        this.chain = this.chain.andThen(validator);
        return this;
    }

    public void end() {
        this.chain.apply(target);
    }
}
