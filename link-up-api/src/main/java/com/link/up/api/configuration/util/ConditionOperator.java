package com.link.up.api.configuration.util;

/**
 * 条件操作符。
 */
public enum ConditionOperator {

    EQUAL("==", Category.EQUALITY, Arity.BINARY, Source.LITERAL),
    NOT_EQUAL("!=", Category.EQUALITY, Arity.BINARY, Source.LITERAL),

    GREATER_THAN(">", Category.NUMERIC, Arity.BINARY, Source.LITERAL),
    GREATER_OR_EQUAL(">=", Category.NUMERIC, Arity.BINARY, Source.LITERAL),
    LESS_THAN("<", Category.NUMERIC, Arity.BINARY, Source.LITERAL),
    LESS_OR_EQUAL("<=", Category.NUMERIC, Arity.BINARY, Source.LITERAL),

    NOT_BLANK("is not blank", Category.STRING, Arity.UNARY, Source.LITERAL),
    STARTS_WITH("starts with", Category.STRING, Arity.BINARY, Source.LITERAL),
    CONTAINS("contains", Category.STRING, Arity.BINARY, Source.LITERAL),
    MATCHES("matches", Category.STRING, Arity.BINARY, Source.LITERAL),
    UPPER_CASE("is uppercase", Category.STRING, Arity.UNARY, Source.LITERAL),
    LOWER_CASE("is lowercase", Category.STRING, Arity.UNARY, Source.LITERAL),

    NOT_EMPTY("is not empty", Category.COLLECTION, Arity.UNARY, Source.LITERAL),
    COLLECTION_UNIQUE(
            "has unique elements",
            Category.COLLECTION,
            Arity.UNARY,
            Source.LITERAL),

    MAP_NOT_EMPTY("is not empty", Category.MAP, Arity.UNARY, Source.LITERAL),
    MAP_CONTAINS_KEY(
            "contains key",
            Category.MAP,
            Arity.BINARY,
            Source.LITERAL),
    MAP_CONTAINS_KEYS(
            "contains keys",
            Category.MAP,
            Arity.BINARY,
            Source.LITERAL),

    FIELD_LESS_THAN("<", Category.NUMERIC, Arity.BINARY, Source.FIELD),
    FIELD_LESS_OR_EQUAL("<=", Category.NUMERIC, Arity.BINARY, Source.FIELD),
    FIELD_GREATER_THAN(">", Category.NUMERIC, Arity.BINARY, Source.FIELD),
    FIELD_GREATER_OR_EQUAL(">=", Category.NUMERIC, Arity.BINARY, Source.FIELD),

    EXTENSION(
            "extension",
            Category.EXTENSION,
            Arity.EXTENSION,
            Source.EXTENSION);

    private final String symbol;
    private final Category category;
    private final Arity arity;
    private final Source source;

    ConditionOperator(
            String symbol,
            Category category,
            Arity arity,
            Source source) {

        this.symbol = symbol;
        this.category = category;
        this.arity = arity;
        this.source = source;
    }

    public String getSymbol() {
        return symbol;
    }

    public Category getCategory() {
        return category;
    }

    public Arity getArity() {
        return arity;
    }

    public Source getSource() {
        return source;
    }

    public enum Category {
        EQUALITY,
        NUMERIC,
        STRING,
        COLLECTION,
        MAP,
        EXTENSION
    }

    public enum Arity {
        UNARY,
        BINARY,
        EXTENSION
    }

    public enum Source {
        LITERAL,
        FIELD,
        EXTENSION
    }
}