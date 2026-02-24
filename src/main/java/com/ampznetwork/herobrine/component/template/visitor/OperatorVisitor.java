package com.ampznetwork.herobrine.component.template.visitor;

import com.ampznetwork.herobrine.antlr.DiscordMessageTemplateParser;
import com.ampznetwork.herobrine.antlr.DiscordMessageTemplateParserBaseVisitor;
import com.ampznetwork.herobrine.component.template.model.op.Operator;
import lombok.Value;

@Value
public class OperatorVisitor extends DiscordMessageTemplateParserBaseVisitor<Operator> {
    public static final OperatorVisitor INSTANCE = new OperatorVisitor();

    @Override
    public Operator visitOpLogicNot(DiscordMessageTemplateParser.OpLogicNotContext ctx) {
        return Operator.LogicNot;
    }

    @Override
    public Operator visitOpBinaryNot(DiscordMessageTemplateParser.OpBinaryNotContext ctx) {
        return Operator.BinaryNot;
    }

    @Override
    public Operator visitOpNegate(DiscordMessageTemplateParser.OpNegateContext ctx) {
        return Operator.Negate;
    }

    @Override
    public Operator visitOpPlus(DiscordMessageTemplateParser.OpPlusContext ctx) {
        return Operator.Plus;
    }

    @Override
    public Operator visitOpMinus(DiscordMessageTemplateParser.OpMinusContext ctx) {
        return Operator.Minus;
    }

    @Override
    public Operator visitOpMultiply(DiscordMessageTemplateParser.OpMultiplyContext ctx) {
        return Operator.Multiply;
    }

    @Override
    public Operator visitOpDivide(DiscordMessageTemplateParser.OpDivideContext ctx) {
        return Operator.Divide;
    }

    @Override
    public Operator visitOpModulo(DiscordMessageTemplateParser.OpModuloContext ctx) {
        return Operator.Modulo;
    }

    @Override
    public Operator visitOpLogicOr(DiscordMessageTemplateParser.OpLogicOrContext ctx) {
        return Operator.LogicOr;
    }

    @Override
    public Operator visitOpLogicAnd(DiscordMessageTemplateParser.OpLogicAndContext ctx) {
        return Operator.LogicAnd;
    }

    @Override
    public Operator visitOpBinaryOr(DiscordMessageTemplateParser.OpBinaryOrContext ctx) {
        return Operator.BinaryOr;
    }

    @Override
    public Operator visitOpBinaryAnd(DiscordMessageTemplateParser.OpBinaryAndContext ctx) {
        return Operator.BinaryAnd;
    }

    @Override
    public Operator visitOpNonNullElse(DiscordMessageTemplateParser.OpNonNullElseContext ctx) {
        return Operator.NonNullElse;
    }
}
