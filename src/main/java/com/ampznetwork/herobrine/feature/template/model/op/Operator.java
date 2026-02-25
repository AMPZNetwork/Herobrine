package com.ampznetwork.herobrine.feature.template.model.op;

import com.ampznetwork.herobrine.feature.template.TypeUtil;
import com.ampznetwork.herobrine.feature.template.model.CodeComponent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

@Getter
@RequiredArgsConstructor
@SuppressWarnings("SwitchStatementWithTooFewBranches")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public enum Operator implements CodeComponent, BinaryOperator<Object> {
    LogicNot("!") {
        @Override
        public Object apply(Object x, Object $) {
            return !TypeUtil.toBoolean(x);
        }
    }, BinaryNot("~") {
        @Override
        public Object apply(Object x, Object $) {
            return switch (x) {
                case Number n -> ~n.longValue();
                default -> throw invalidArg(x);
            };
        }
    }, Negate("-") {
        @Override
        public Object apply(Object x, Object $) {
            return switch (x) {
                case Double d -> -d;
                case Float f -> -f;
                case Long l -> -l;
                case Integer i -> -i;
                case Short s -> -s;
                case Byte b -> -b;
                default -> throw invalidArg(x);
            };
        }
    }, Plus("+") {
        @Override
        public Object apply(Object x, Object y) {
            if (x instanceof Number nx && y instanceof Number ny) return nx.doubleValue() + ny.doubleValue();
            if (x instanceof String str) return str + y;
            if (y instanceof String str) return x + str;
            throw invalidArg(x, y);
        }
    }, Minus("-") {
        @Override
        public Object apply(Object x, Object y) {
            if (x instanceof Number nx && y instanceof Number ny) return nx.doubleValue() - ny.doubleValue();
            throw invalidArg(x, y);
        }
    }, Multiply("*") {
        @Override
        public Object apply(Object x, Object y) {
            if (x instanceof Number nx && y instanceof Number ny) return nx.doubleValue() * ny.doubleValue();
            if (x instanceof String str && y instanceof Number n)
                return LongStream.range(0, n.longValue()).mapToObj($ -> str).collect(Collectors.joining());
            throw invalidArg(x, y);
        }
    }, Divide("/") {
        @Override
        public Object apply(Object x, Object y) {
            if (x instanceof Number nx && y instanceof Number ny) return nx.doubleValue() / ny.doubleValue();
            throw invalidArg(x, y);
        }
    }, Modulo("%") {
        @Override
        public Object apply(Object x, Object y) {
            if (x instanceof Number nx && y instanceof Number ny) return nx.doubleValue() % ny.doubleValue();
            throw invalidArg(x, y);
        }
    }, LogicOr("||") {
        @Override
        public Object apply(Object x, Object y) {
            if (x instanceof Boolean bx && y instanceof Boolean by) return bx || by;
            throw invalidArg(x, y);
        }
    }, LogicAnd("&&") {
        @Override
        public Object apply(Object x, Object y) {
            if (x instanceof Boolean bx && y instanceof Boolean by) return bx && by;
            throw invalidArg(x, y);
        }
    }, BinaryOr("|") {
        @Override
        public Object apply(Object x, Object y) {
            if (x instanceof Boolean bx && y instanceof Boolean by) return bx | by;
            if (x instanceof Number nx && y instanceof Number ny) return nx.longValue() | ny.longValue();
            throw invalidArg(x, y);
        }
    }, BinaryAnd("&") {
        @Override
        public Object apply(Object x, Object y) {
            if (x instanceof Boolean bx && y instanceof Boolean by) return bx & by;
            if (x instanceof Number nx && y instanceof Number ny) return nx.longValue() & ny.longValue();
            throw invalidArg(x, y);
        }
    }, NonNullElse("??") {
        @Override
        public Object apply(Object x, Object y) {
            return x == null ? y : x;
        }
    };

    String string;

    @Override
    public String toSerializedString() {
        return string;
    }

    protected IllegalArgumentException invalidArg(Object x) {
        return new IllegalArgumentException("Invalid argument '%s'; not compatible with operator %s".formatted(x,
                name()));
    }

    protected IllegalArgumentException invalidArg(Object x, Object y) {
        return new IllegalArgumentException("Invalid arguments '%s' and '%s'; not compatible with operator %s".formatted(
                x,
                y,
                name()));
    }
}
