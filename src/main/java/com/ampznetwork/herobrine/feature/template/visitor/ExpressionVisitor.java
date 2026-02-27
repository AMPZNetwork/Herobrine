package com.ampznetwork.herobrine.feature.template.visitor;

import com.ampznetwork.herobrine.antlr.DiscordMessageTemplateParser;
import com.ampznetwork.herobrine.antlr.DiscordMessageTemplateParserBaseVisitor;
import com.ampznetwork.herobrine.feature.template.model.expr.CallExpression;
import com.ampznetwork.herobrine.feature.template.model.expr.ConstructorCall;
import com.ampznetwork.herobrine.feature.template.model.expr.Expression;
import com.ampznetwork.herobrine.feature.template.model.expr.ParenthesesExpression;
import com.ampznetwork.herobrine.feature.template.model.expr.TernaryExpression;
import com.ampznetwork.herobrine.feature.template.model.expr.lit.LiteralBoolean;
import com.ampznetwork.herobrine.feature.template.model.expr.lit.LiteralNull;
import com.ampznetwork.herobrine.feature.template.model.expr.lit.LiteralNumber;
import com.ampznetwork.herobrine.feature.template.model.expr.lit.LiteralString;
import com.ampznetwork.herobrine.feature.template.model.expr.op.BinaryOperator;
import com.ampznetwork.herobrine.feature.template.model.expr.op.UnaryOperator;
import com.ampznetwork.herobrine.feature.template.types.Type;
import lombok.Value;

import java.util.stream.Collectors;

@Value
public class ExpressionVisitor extends DiscordMessageTemplateParserBaseVisitor<Expression> {
    public static final ExpressionVisitor INSTANCE = new ExpressionVisitor();

    @Override
    public LiteralNull visitExprNull(DiscordMessageTemplateParser.ExprNullContext ctx) {
        return new LiteralNull();
    }

    @Override
    public LiteralBoolean visitExprBoolean(DiscordMessageTemplateParser.ExprBooleanContext ctx) {
        return new LiteralBoolean(Boolean.parseBoolean(ctx.LITERAL_BOOLEAN().getText()));
    }

    @Override
    public CallExpression visitExprCall(DiscordMessageTemplateParser.ExprCallContext ctx) {
        return new CallExpression(ReferenceVisitor.parse(ctx.reference_chain()));
    }

    @Override
    public UnaryOperator visitExprOperatorUnary(DiscordMessageTemplateParser.ExprOperatorUnaryContext ctx) {
        return new UnaryOperator(OperatorVisitor.INSTANCE.visit(ctx.operator_unary()), visit(ctx.expression()));
    }

    @Override
    public LiteralString visitExprString(DiscordMessageTemplateParser.ExprStringContext ctx) {
        var text = ctx.LITERAL_STRING().getText();
        return new LiteralString(text.substring(1, text.length() - 1));
    }

    @Override
    public ConstructorCall visitExprCallCtor(DiscordMessageTemplateParser.ExprCallCtorContext ctx) {
        var properties = ctx.initializer()
                .property_list()
                .property_initializer()
                .stream()
                .collect(Collectors.toMap(ReferenceVisitor::parse, property -> visit(property.expression())));
        return new ConstructorCall(Type.forName(ctx.WORD().getText()), properties);
    }

    @Override
    public BinaryOperator visitExprOperatorBinary(DiscordMessageTemplateParser.ExprOperatorBinaryContext ctx) {
        return new BinaryOperator(OperatorVisitor.INSTANCE.visit(ctx.operator_binary()),
                visit(ctx.left),
                visit(ctx.right));
    }

    @Override
    public TernaryExpression visitExprTernary(DiscordMessageTemplateParser.ExprTernaryContext ctx) {
        return new TernaryExpression(visit(ctx.condition), visit(ctx.onTrue), visit(ctx.onFalse));
    }

    @Override
    public LiteralNumber visitExprNumber(DiscordMessageTemplateParser.ExprNumberContext ctx) {
        return new LiteralNumber(Double.parseDouble(ctx.LITERAL_NUMBER().getText()));
    }

    @Override
    public ParenthesesExpression visitExprParentheses(DiscordMessageTemplateParser.ExprParenthesesContext ctx) {
        return new ParenthesesExpression(visit(ctx.expression()));
    }
}
