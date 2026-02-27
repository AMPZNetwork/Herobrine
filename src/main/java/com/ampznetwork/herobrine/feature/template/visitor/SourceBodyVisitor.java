package com.ampznetwork.herobrine.feature.template.visitor;

import com.ampznetwork.herobrine.antlr.DiscordMessageTemplateParser;
import com.ampznetwork.herobrine.antlr.DiscordMessageTemplateParserBaseVisitor;
import com.ampznetwork.herobrine.feature.template.context.Reference;
import com.ampznetwork.herobrine.feature.template.model.decl.CodeBlock;
import com.ampznetwork.herobrine.feature.template.model.decl.Declaration;
import com.ampznetwork.herobrine.feature.template.model.decl.func.Function;
import com.ampznetwork.herobrine.feature.template.model.decl.func.Parameter;
import com.ampznetwork.herobrine.feature.template.model.decl.stmt.CallStatement;
import com.ampznetwork.herobrine.feature.template.model.decl.stmt.DoWhileStatement;
import com.ampznetwork.herobrine.feature.template.model.decl.stmt.ForEachStatement;
import com.ampznetwork.herobrine.feature.template.model.decl.stmt.ForVarStatement;
import com.ampznetwork.herobrine.feature.template.model.decl.stmt.IfElseStatement;
import com.ampznetwork.herobrine.feature.template.model.decl.stmt.ModifyStatement;
import com.ampznetwork.herobrine.feature.template.model.decl.stmt.MutateStatement;
import com.ampznetwork.herobrine.feature.template.model.decl.stmt.SetStatement;
import com.ampznetwork.herobrine.feature.template.model.decl.stmt.WhileStatement;
import lombok.Value;
import org.antlr.v4.runtime.tree.ParseTree;

@Value
public class SourceBodyVisitor extends DiscordMessageTemplateParserBaseVisitor<Declaration> {
    public static final SourceBodyVisitor INSTANCE = new SourceBodyVisitor();

    @Override
    public IfElseStatement visitStmtIfElse(DiscordMessageTemplateParser.StmtIfElseContext ctx) {
        return new IfElseStatement(ExpressionVisitor.INSTANCE.visit(ctx.expression()),
                visit(ctx.onTrue),
                ctx.onFalse == null ? null : visit(ctx.onFalse));
    }

    @Override
    public ForEachStatement visitStmtForIn(DiscordMessageTemplateParser.StmtForInContext ctx) {
        return new ForEachStatement(Reference.builder().key(new Reference.Part(ctx.WORD().getText(), false)).build(),
                ExpressionVisitor.INSTANCE.visit(ctx.expression()),
                visit(ctx.code_block()));
    }

    @Override
    public ForVarStatement visitStmtForI(DiscordMessageTemplateParser.StmtForIContext ctx) {
        return new ForVarStatement(visit(ctx.init),
                ExpressionVisitor.INSTANCE.visit(ctx.condition),
                visit(ctx.accumulate),
                visit(ctx.code_block()));
    }

    @Override
    public WhileStatement visitStmtWhile(DiscordMessageTemplateParser.StmtWhileContext ctx) {
        return new WhileStatement(ExpressionVisitor.INSTANCE.visit(ctx.expression()), visit(ctx.code_block()));
    }

    @Override
    public DoWhileStatement visitStmtDoWhile(DiscordMessageTemplateParser.StmtDoWhileContext ctx) {
        return new DoWhileStatement(ExpressionVisitor.INSTANCE.visit(ctx.expression()), visit(ctx.code_block()));
    }

    @Override
    public MutateStatement visitStmtMutate(DiscordMessageTemplateParser.StmtMutateContext ctx) {
        return new MutateStatement(ReferenceVisitor.parse(ctx.reference_chain()),
                OperatorVisitor.INSTANCE.visit(ctx.operator_binary()),
                ExpressionVisitor.INSTANCE.visit(ctx.expression()));
    }

    @Override
    public CallStatement visitStmtCall(DiscordMessageTemplateParser.StmtCallContext ctx) {
        return new CallStatement(ReferenceVisitor.parse(ctx.reference_chain()));
    }

    @Override
    public SetStatement visitStmtSet(DiscordMessageTemplateParser.StmtSetContext ctx) {
        return new SetStatement(ReferenceVisitor.parse(ctx.reference_chain()),
                ExpressionVisitor.INSTANCE.visit(ctx.expression()));
    }

    @Override
    public ModifyStatement visitStmtModify(DiscordMessageTemplateParser.StmtModifyContext ctx) {
        return new ModifyStatement(ReferenceVisitor.parse(ctx.reference_chain()),
                OperatorVisitor.INSTANCE.visit(ctx.operator_binary()),
                ExpressionVisitor.INSTANCE.visit(ctx.expression()));
    }

    @Override
    public Function visitFunction(DiscordMessageTemplateParser.FunctionContext ctx) {
        return new Function(ctx.WORD().getText(),
                ctx.param_list().WORD().stream().map(ParseTree::getText).map(Parameter::new).toArray(Parameter[]::new),
                visit(ctx.code_block()));
    }

    @Override
    public CodeBlock visitBlockLarge(DiscordMessageTemplateParser.BlockLargeContext ctx) {
        return new CodeBlock(ctx.statement().stream().map(this::visit).toArray(Declaration[]::new));
    }

    @Override
    public CodeBlock visitSource_body(DiscordMessageTemplateParser.Source_bodyContext ctx) {
        return new CodeBlock(ctx.declaration().stream().map(this::visit).toArray(Declaration[]::new));
    }
}
